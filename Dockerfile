FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package dependency:copy-dependencies \
    -DincludeGroupIds=com.microsoft.playwright,com.google.code.gson \
    -DoutputDirectory=target/playwright

FROM eclipse-temurin:21-jre-noble
WORKDIR /app
ENV PLAYWRIGHT_BROWSERS_PATH=/opt/playwright-browsers
ENV PATH="/opt/yt-dlp/bin:${PATH}"
COPY --from=build /build/target/playwright /tmp/playwright
RUN apt-get update \
    && apt-get install -y --no-install-recommends python3 python3-venv ffmpeg ca-certificates \
    && python3 -m venv /opt/yt-dlp \
    && /opt/yt-dlp/bin/pip install --no-cache-dir "yt-dlp[default]" \
    && java -cp '/tmp/playwright/*' com.microsoft.playwright.CLI install --with-deps chromium \
    && rm -rf /tmp/playwright /var/lib/apt/lists/*
COPY --from=build /build/target/si-article-pipeline-1.0-SNAPSHOT.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
