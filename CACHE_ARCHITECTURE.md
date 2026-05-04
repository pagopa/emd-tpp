# Cache Redis distribuita — emd-tpp

## Indice

1. [Contesto e motivazione](#1-contesto-e-motivazione)
2. [Stack tecnologico](#2-stack-tecnologico)
3. [Struttura della cache in Redis](#3-struttura-della-cache-in-redis)
4. [Componenti](#4-componenti)
5. [Flusso di vita della cache](#5-flusso-di-vita-della-cache)
6. [Comportamento con più repliche (multi-pod)](#6-comportamento-con-più-repliche-multi-pod)
7. [Finestra di inconsistenza e fallback su MongoDB](#7-finestra-di-inconsistenza-e-fallback-su-mongodb)
8. [Configurazione](#8-configurazione)
9. [Test](#9-test)
10. [Verifica della logica — analisi dei rischi](#10-verifica-della-logica--analisi-dei-rischi)

---

## 1. Contesto e motivazione

In precedenza la cache dei TPP era basata su **Caffeine** (in-memory): ogni pod JVM manteneva la propria copia locale della mappa `tppId → Tpp`. Con un solo pod questo funzionava; con **più repliche** (scalabilità orizzontale) ogni pod aveva uno stato indipendente:

- una scrittura (es. `updateState`) aggiornava la cache **solo del pod che gestiva la richiesta** → le altre repliche restavano stale;
- un delete (`deleteTpp`) rimuoveva il TPP dalla cache **solo localmente**.

La migrazione a **Redis** centralizzato risolve il problema: tutti i pod condividono la stessa cache.

---

## 2. Stack tecnologico

| Componente | Versione | Ruolo |
|---|---|---|
| `redisson-spring-boot-starter` | `3.43.0` | Client Redis reattivo (stessa versione di emd-citizen) |
| `RMapReactive<String, Tpp>` | Redisson | Redis Hash distribuito — cache dei TPP |
| `RLockReactive` | Redisson | Distributed lock — sincronizzazione tra pod |
| Jackson + `JavaTimeModule` | Spring default | Serializzazione JSON dei `Tpp` (gestisce `LocalDateTime`) |

---

## 3. Struttura della cache in Redis

```
Redis
└── Hash  "emd:tpp:cache"           ← RMapReactive<String, Tpp>
    ├── field "tppId-abc"  → { JSON del Tpp con TokenSection DECIFRATA }
    ├── field "tppId-xyz"  → { JSON del Tpp con TokenSection DECIFRATA }
    └── ...

Lock "emd:tpp:cache-reset-lock"     ← RLockReactive (watchdog)
```

| Chiave Redis | Tipo | Scopo |
|---|---|---|
| `emd:tpp:cache` | Hash | Mappa `tppId → Tpp (JSON)` |
| `emd:tpp:cache-reset-lock` | String (lock) | Lock distribuito per inizializzazione e reset |

> ⚠️ I valori del `TokenSection` (`pathAdditionalProperties`, `bodyAdditionalProperties`) sono **decifrati** in cache.
> MongoDB contiene invece i valori **cifrati** tramite Azure Key Vault.

---

## 4. Componenti

### 4.1 `RedisConfig`

`src/main/java/it/gov/pagopa/common/configuration/RedisConfig.java`

```java
@Bean
public RMapReactive<String, Tpp> tppMapReactive(RedissonReactiveClient redissonReactiveClient) {
    ObjectMapper redisObjectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    return redissonReactiveClient.getMap(TPP_CACHE_MAP_KEY, new JsonJacksonCodec(redisObjectMapper));
}
```

- Crea il bean `RMapReactive<String, Tpp>` che punta alla Redis Hash `"emd:tpp:cache"`.
- `JsonJacksonCodec` con `JavaTimeModule` per serializzare/deserializzare `LocalDateTime` correttamente.
- Singleton iniettato in `TppMapService`.

### 4.2 `TppMapService`

`src/main/java/it/gov/pagopa/tpp/service/TppMapService.java`

#### API pubblica

| Metodo | Firma | Comportamento |
|---|---|---|
| `addToMap` | `Mono<Boolean>` | Decifra il TokenSection via Azure Key Vault → `HSET emd:tpp:cache tppId <json>`. Ritorna `true` su successo, `false` se la decifrazione fallisce. |
| `getFromMap` | `Mono<Tpp>` | `HGET emd:tpp:cache tppId`. `Mono.empty()` se assente. |
| `removeFromMap` | `Mono<Void>` | `HDEL emd:tpp:cache tppId`. |
| `populateMap` *(@PostConstruct)* | `void` (blocking) | Popolamento iniziale all'avvio del pod. |
| `resetCache` *(@Scheduled)* | `void` (blocking) | Reset giornaliero alle 5:00 AM. |

#### Lock distribuito

```java
private Mono<Boolean> acquireLock() {
    return redissonClient.getLock(LOCK_KEY).tryLock(0, -1, TimeUnit.SECONDS);
}
```

| Parametro | Valore | Significato |
|---|---|---|
| `waitTime` | `0` | Non aspetta: se il lock è già preso, ritorna subito `false` (non-blocking). |
| `leaseTime` | `-1` | Attiva il **watchdog** di Redisson: rinnova automaticamente il TTL del lock ogni ~10 s finché il pod è vivo. Il lock non scade mai prematuramente, e viene rilasciato automaticamente alla morte del pod. |

#### Helper interno `buildSnapshotFromDb()`

Sia `populateMap` che `resetCache` usano lo stesso helper:

```java
private Mono<Map<String, Tpp>> buildSnapshotFromDb() {
    return tppRepository.findAll()
            .filter(tpp -> Boolean.TRUE.equals(tpp.getState()))   // null-safe
            .buffer(100)
            .flatMap(batch -> Flux.fromIterable(batch)
                    .flatMap(tpp -> keyDecrypt(...)
                            .doOnSuccess(v -> snapshot.put(tpp.getTppId(), tpp))
                            .onErrorResume(e -> Mono.empty())))   // skip TPP con decrypt fallito
            ...
}
```

- **Filtro null-safe** sullo `state`: evita NPE su `Tpp.state == null`.
- **Errori di decrypt vengono loggati e skippati**, non interrompono il populate/reset.
- Lo snapshot viene poi scritto su Redis con `tppMap.putAll(snapshot)` — operazione **atomica** lato Redis.

### 4.3 `TppServiceImpl` — utilizzo della cache

Pattern **cache-aside** (look-aside) per le letture, **write-through** per le scritture:

```
read:
  getFromMap(tppId)
    → HIT  → usa il dato da Redis
    → MISS → legge da MongoDB → addToMap() → ritorna il dato

write (su MongoDB):
  → tppRepository.save(...)
  → addToMap(savedTpp)        ← propaga subito a Redis (visibile da tutti i pod)
delete:
  → tppRepository.delete(...)
  → removeFromMap(tppId)
```

**Operazioni che aggiornano la cache (write-through):**

| Metodo `TppServiceImpl` | Azione cache |
|---|---|
| `createNewTpp` | `addToMap` |
| `updateTppDetails` | `addToMap` |
| `updateTokenSection` | `addToMap` |
| `updateState` | `addToMap` |
| `updateIsPaymentEnabled` | `addToMap` |
| `insertRecipientIdOnWhitelist` | `addToMap` |
| `removeRecipientIdOnWhitelist` | `addToMap` |
| `updateRecipientIdOnWhitelist` | `addToMap` |
| `deleteTpp` | `removeFromMap` |

**Operazioni che leggono dalla cache (con fallback su MongoDB):**

| Metodo | Logica |
|---|---|
| `filterEnabledList` | Cache → per ID mancanti, query DB + `addToMap` |
| `getTppDetails` | Cache → fallback DB + `addToMap` |
| `getTokenSection` | Cache → fallback DB + `addToMap` |

---

## 5. Flusso di vita della cache

### 5.1 Startup — `populateMap()` (`@PostConstruct`)

```
Pod si avvia
     │
     ▼
acquireLock(waitTime=0, watchdog)
     ├─ false → SKIP (un altro pod sta inizializzando)
     │          il pod è subito Ready, le sue richieste useranno il fallback su MongoDB
     │
     └─ true  → tppMap.isExists()
                 ├─ true  → SKIP (cache già popolata da pod precedente) → releaseLock
                 │
                 └─ false → buildSnapshotFromDb()
                              │
                              ▼
                            tppMap.putAll(snapshot)   ← operazione atomica
                              │
                              ▼
                            releaseLock (in doFinally)
```

**Note:**
- `.block(Duration.ofSeconds(120))`: `@PostConstruct` non è in un context Reactor. Bloccare è necessario e safe.
- Il pod non viene marcato `Ready` da Kubernetes finché `populateMap` non termina.
- Se Pod-A crasha durante `buildSnapshotFromDb`, lo snapshot non viene mai scritto su Redis: `tppMap` resta vuota e Pod-B (o un nuovo Pod-A) ripete il populate da zero (no cache parziale persistente).

### 5.2 Reset giornaliero — `resetCache()` (`@Scheduled`)

Eseguito ogni giorno alle **05:00** (`cron = "0 0 5 * * ?"`).

```
@Scheduled(cron = "0 0 5 * * ?")
resetCache()
     │
     ▼
acquireLock
     ├─ false → SKIP
     │
     └─ true  → buildSnapshotFromDb()
                  │  (legge tutti i TPP attivi da MongoDB e li decifra in memoria)
                  ▼
                tppMap.delete()        [⚠ finestra di inconsistenza: mappa vuota]
                  │
                  ▼
                tppMap.putAll(snapshot)   [Redis Hash ripopolato]
                  │
                  ▼
                releaseLock (in doFinally)
```

Costruire lo snapshot **prima** di `delete()` minimizza la finestra di mappa vuota: dura solo il tempo di `delete()` + `putAll()`, indipendentemente dal tempo di `findAll()` su MongoDB e dei decrypt Azure Key Vault.

### 5.3 Operazioni puntuali

Ogni scrittura su MongoDB è seguita da un aggiornamento immediato della cache:

```java
return tppRepository.save(tpp)
    .flatMap(savedTpp -> tppMapService.addToMap(savedTpp).thenReturn(savedTpp))
    .map(mapperToDTO::map);
```

`addToMap()` chiama `tokenSectionCryptService.keyDecrypt()` per decifrare il TokenSection prima di scrivere in Redis (la cache contiene sempre token decifrati).

---

## 6. Comportamento con più repliche (multi-pod)

### 6.1 Avvio contemporaneo di N pod

```
t=0   Pod-A, Pod-B, Pod-C avviano simultaneamente
t=1   Pod-A: tryLock → true → buildSnapshotFromDb()
      Pod-B: tryLock → false → SKIP (subito Ready)
      Pod-C: tryLock → false → SKIP (subito Ready)
t=2   Pod-A: putAll(snapshot) → releaseLock
t=3   Tutti i pod servono richieste:
      - Pod-A da cache piena
      - Pod-B/C: cache miss iniziali → fallback DB + addToMap()
```

> Pod-B e Pod-C **non aspettano** Pod-A (pattern `waitTime=0`, by design). Funzionalmente corretto perché il fallback su MongoDB compensa nel transitorio. Se servisse comportamento bloccante, basta passare `waitTime > 0`.

### 6.2 Reset giornaliero in presenza di N pod

```
t=05:00:00  Tutti i pod scattano @Scheduled simultaneamente
            Pod-A: acquista il lock (watchdog attivo)
            Pod-B: tryLock → false → skip + log
            Pod-C: tryLock → false → skip + log

t=05:00:00 → t=05:00:XX  Pod-A esegue buildSnapshotFromDb + delete + putAll
            - Watchdog Redisson rinnova il lock ogni ~10 s
            - Nessun reset concorrente possibile

t=05:00:XX  Pod-A → releaseLock
            Tutti i pod leggono dalla cache aggiornata
```

### 6.3 Rolling update Kubernetes

```
t=0   Pod-A (vecchia versione) Running, cache già popolata in Redis
t=1   Pod-B (nuova versione) avvia → populateMap()
      → tryLock → true → isExists() → true (cache già piena) → SKIP
      → Pod-B Ready in pochi secondi
t=2   K8s drena Pod-A
```

Il watchdog è fondamentale per la resilienza: se un pod **muore** mentre detiene il lock, Redisson rilascia automaticamente il lock al rilevamento della disconnessione (no deadlock).

---

## 7. Finestra di inconsistenza e fallback su MongoDB

Durante `performReset()`, tra `tppMap.delete()` e `tppMap.putAll(snapshot)`, la map Redis è **vuota**. Le richieste in questo intervallo:

1. `getFromMap(tppId)` → `Mono.empty()`
2. `TppServiceImpl` rileva la cache-miss → interroga MongoDB direttamente
3. Il risultato è corretto (MongoDB è source of truth)
4. Il TPP viene re-inserito in cache via `addToMap()` (cache-aside warm-up)

La finestra dura tipicamente qualche decina di ms. È accettabile e funzionalmente trasparente.

---

## 8. Configurazione

### `application.yml`

```yaml
spring:
  data:
    redis:
      database: ${REDIS_DATABASE:1}
      host: ${REDIS_CONNECTION_HOSTNAME:localhost}
      port: ${REDIS_PORT:6380}
      ssl:
        enabled: ${REDIS_SSL_ENABLED:false}
      password: ${REDIS_CONNECTION_PASSWORD:}
```

### Variabili d'ambiente

| Variabile | Default | Descrizione |
|---|---|---|
| `REDIS_CONNECTION_HOSTNAME` | `localhost` | Host Redis |
| `REDIS_PORT` | `6380` | Porta Redis (Azure Cache for Redis usa 6380 con SSL) |
| `REDIS_DATABASE` | `1` | Database Redis (separazione per ambiente) |
| `REDIS_SSL_ENABLED` | `false` | Abilitare SSL in produzione |
| `REDIS_CONNECTION_PASSWORD` | *(vuoto)* | Password Redis |

### Watchdog Redisson

Default: TTL del lock = 30 secondi, rinnovato ogni 10 secondi finché il client è vivo. Non richiede configurazione esplicita.

---

## 9. Test

### Test unitari — `TppMapServiceTest`

`src/test/java/it/gov/pagopa/tpp/service/TppMapServiceTest.java`

- `RLockReactive`, `RMapReactive`, `RedissonReactiveClient`, `TppRepository`, `TokenSectionCryptService` mockati.
- Testa `addToMap`, `getFromMap`, `removeFromMap`, `resetCache` in isolamento.

### Test di integrazione — `TppMapServiceIT`

`src/test/java/it/gov/pagopa/tpp/integration/TppMapServiceIT.java`

- `@SpringBootTest` con context completo.
- **Testcontainers**: Redis reale (`redis:8.2.2-alpine`) + MongoDB (`mongo:8.0.15-noble`).
- `@MockBean TokenSectionCryptService` (evita chiamate Azure Key Vault in CI).
- Scenari testati:
  1. `testTppMapServiceBeanLoaded` — wiring Spring corretto
  2. `testAddAndGetFromMap` — round-trip Redis effettivo
  3. `testRemoveFromMap` — cancellazione effettiva da Redis
  4. `testResetCachePopulatesOnlyActiveTpps` — solo TPP `state=true` finiscono in cache
  5. `testResetCacheClearsStaleEntries` — entry obsolete eliminate

I test IT sono inclusi in `mvn clean package` (richiedono Docker disponibile).

---

## 10. Verifica della logica — analisi dei rischi

### ✅ Risolti / non più applicabili

| Rischio | Soluzione |
|---|---|
| Multi-pod con Caffeine: cache divergente per pod | Migrazione a Redis (cache condivisa) |
| Lock scaduto durante reset su dataset grandi | `leaseTime=-1` → watchdog Redisson mantiene il lock vivo finché il pod vive |
| Doppio reset concorrente su N pod | Lock distribuito: solo un pod per volta esegue `performReset()` |
| **NPE su `Tpp.state == null`** in populate/reset | Filtro difensivo `Boolean.TRUE.equals(tpp.getState())` |
| **Cache parziale persistente dopo crash a metà populate** | `doPopulate()` ora costruisce snapshot in memoria e usa `putAll()` atomico (come `performReset`); se il pod crasha durante la build, Redis resta vuoto e il pod successivo ripete da zero |
| **Inconsistenza tra populate (HSET singoli) e reset (`putAll`)** | Helper unico `buildSnapshotFromDb()` + `putAll()` per entrambi |
| **`putAll(emptyMap)` edge case** quando MongoDB è vuoto | Check esplicito `snapshot.isEmpty()` → skip della scrittura |

### ⚠️ Limiti accettati (documentati)

| Limite | Impatto | Mitigazione |
|---|---|---|
| Pod-B/C non aspettano Pod-A allo startup | Cache miss nel transitorio → fallback MongoDB | Funzionalmente corretto, latenza extra trascurabile |
| Finestra di inconsistenza durante reset (`delete` → `putAll`) | Letture cache vuote per ~decine di ms | Fallback automatico su MongoDB |
| **Race tra `addToMap` di Pod-B e `performReset` di Pod-A**: l'update di Pod-B durante la build dello snapshot di Pod-A può essere sovrascritto in cache da `putAll(snapshot)` | L'update non è perso (è in MongoDB), ma sparisce dalla cache | Recovery via cache-miss successivo. Finestra solo alle 5:00 AM, traffico basso |
| **Filtro `state=true` solo in populate/reset, non in `addToMap`** | Le scritture cachano TPP anche `state=false` (es. whitelist update); il reset notturno li rimuove | Fallback DB; scelta architetturale (la cache è per i TPP "attivi") |
| **`keyDecrypt` muta `TokenSection` in-place** | Dopo `addToMap(savedTpp)`, `savedTpp` contiene il token section decifrato | Comportamento intenzionale: `TppServiceImpl` poi mappa `savedTpp → DTO` con dati decifrati per l'API |
| TokenSection decifrata salvata in Redis | Se Redis è compromesso, dati sensibili esposti | Azure Cache for Redis con SSL + password + ACL su database dedicato |
| N chiamate Azure Key Vault all'avvio (una `getKey` per TPP) | Latenza startup proporzionale al numero di TPP | Solo il primo pod paga il costo; gli altri saltano grazie a `isExists()` |
| `releaseLock()` è fire-and-forget (`.subscribe()`) | `block()` può ritornare prima che il lock sia effettivamente rilasciato | Watchdog Redisson rilascia comunque il lock alla morte del pod; `RLock` è reentrant per lo stesso client |

