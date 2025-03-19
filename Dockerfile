#
# Build
#
FROM maven:3.9.6-amazoncorretto-21-al2023@sha256:8d653ed25358201bdb352ce0d24e4be2f1e34ddf372d3381d22876f9c483cfa1 AS buildtime

WORKDIR /build

COPY pom.xml .
COPY src ./src

# Monta il secret di GitHub Token
RUN --mount=type=secret,id=github_token,uid=1001 \
    SECRET_PATH="/run/secrets/github_token"; \
    echo "📂 Contenuto della directory /run/secrets/:";
    ls -lah /run/secrets/ || echo "⚠️ La directory /run/secrets/ non esiste!";
    \
    if [ -f "$SECRET_PATH" ]; then \
        echo "✅ Il secret github_token è stato montato correttamente!"; \
        echo "📜 Contenuto del secret github_token:"; \
        cat "$SECRET_PATH"; \
    else \
        echo "❌ Il secret github_token NON è stato trovato in /run/secrets/!"; \
        echo "⚠️ Questo significa che il secret non è stato montato o è stato passato vuoto!"; \
    fi

# Esegue la build Maven utilizzando settings.xml
RUN mvn --global-settings settings.xml clean package -DskipTests && rm settings.xml

#
# Docker RUNTIME
#
FROM amazoncorretto:21-alpine3.20@sha256:d35e44aa90121164411d3f647e116d6c4c42461ba67dabc7c5ca6e460d380c12 AS runtime

RUN apk add --no-cache shadow vim curl net-tools bind-tools netcat-openbsd wget

RUN useradd --uid 10000 runner

WORKDIR /app

COPY --from=buildtime /build/target/*.jar /app/app.jar
# The agent is enabled at runtime via JAVA_TOOL_OPTIONS.
ADD https://github.com/microsoft/ApplicationInsights-Java/releases/download/3.6.1/applicationinsights-agent-3.6.1.jar /app/applicationinsights-agent.jar

RUN chown -R runner:runner /app

USER 10000

ENTRYPOINT ["java","-jar","/app/app.jar"]
