#
# Build
#
FROM maven:3.9.6-amazoncorretto-21-al2023@sha256:8d653ed25358201bdb352ce0d24e4be2f1e34ddf372d3381d22876f9c483cfa1 AS buildtime

WORKDIR /build

COPY pom.xml .
COPY src ./src

# Gestione delle credenziali GitHub per Maven
RUN --mount=type=secret,id=gh_token,uid=1001 \
    --mount=type=secret,id=gh_user,uid=1001 \
    export GH_TOKEN=$(cat /run/secrets/gh_token) && \
    export GH_USER=$(cat /run/secrets/gh_user) && \
    echo "<settings><servers><server><id>github</id><username>${GH_USER}</username><password>${GH_TOKEN}</password></server></servers></settings>" > settings.xml && \
    mvn clean package -DskipTests -s settings.xml


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
