FROM maven:3.9-eclipse-temurin-17@sha256:1a352420f7aba21f5ad08df31bab55f74c013fb491f1ae8ab1dd7ff9ed698584 AS build
WORKDIR /app
COPY pom.xml ./
RUN mvn -q -DskipTests dependency:go-offline
COPY src/main ./src/main
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre-jammy@sha256:c35794d1348777e6bc310b9b7f420098ef8f37e988199549b545fded79a895a2 AS runtime
WORKDIR /app
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl fontconfig fonts-dejavu-core \
 && rm -rf /var/lib/apt/lists/* \
 && groupadd --gid 10001 playneml \
 && useradd --uid 10001 --gid playneml --no-create-home playneml \
 && mkdir -p /app/var/player-photos /app/logs \
 && chown -R playneml:playneml /app
COPY --from=build --chown=playneml:playneml /app/target/sports-platform-0.0.1-SNAPSHOT.jar app.jar
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -Djava.awt.headless=true"
USER 10001:10001
EXPOSE 8001
HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=12 \
 CMD curl --fail --silent http://127.0.0.1:8001/api/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
