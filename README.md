# emd-tpp
Service that manages the registration and data recovery operations of TPPs.

The microservice is responsible for:
- managing the registration or modification of third-party data
- changing the activation status of the courtesy message service by the third party
- retrieving TTP data
- updating data used for token generation

## API Documentation

API specification: [openapi.tpp.yml](https://github.com/pagopa/cstar-infrastructure/blob/main/src/domains/mil-app-poc/api/emd_tpp/openapi.tpp.yml)

### Batch lookup anagrafica TPP

`POST /emd/tpp/batch` consente di arricchire una pagina di payment attempt con una sola chiamata HTTP.
Richiede l'header `X-API-Version: 1` e un body con `ids` contenente da 1 a 100 TPP ID non nulli né vuoti.
Gli ID duplicati sono elaborati una sola volta, mantenendo la prima occorrenza. La risposta mantiene
l'ordine degli ID richiesti: `results` contiene esclusivamente `tppId`, `businessName` ed `entityId`,
mentre `missingTppIds` elenca gli ID non trovati. Una lista vuota, ID blank, oltre 100 ID, header
assente o versione sconosciuta producono `400`; gli errori infrastrutturali seguono lo schema d'errore
del servizio e non producono risposte parziali. La rotta deve mantenere le policy di autorizzazione
già applicate alle API TPP, senza introdurre accessi più permissivi.


## Components

---

#### [TppService](src/main/java/it/gov/pagopa/tpp/service/TppServiceImpl.java)

Main class that handles CRUD operations with TPP entities.

---

#### [TppRepository](src/main/java/it/gov/pagopa/tpp/repository/TppRepository.java)

Repository used for operations performed on the database

Collection used: 'tpp'
