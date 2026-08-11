# News Sitemap Scraper

A Spring Boot command-line application that collects article links from XML sitemaps, parses article pages in parallel, stores normalized data in PostgreSQL, downloads referenced images, and exports the result as JSON.

The repository is intentionally source-neutral. Site-specific sitemap URLs, date ranges, and selectors should be configured or adapted for a public source you are authorized to access. Respect the source website's terms, robots policy, copyright, and rate limits.

## Technology

- Java 21 and Spring Boot
- PostgreSQL and Spring Data JPA
- Jsoup for HTML/XML parsing
- MapStruct and Jackson
- Maven and Docker Compose

## Configuration

Shared settings live in `application.properties`. Local database connectivity is defined by the `dev` profile, while Docker connectivity is defined by the `prod` profile.

Create local environment settings from the safe template:

```shell
cp docker/.env.example docker/.env
```

Replace the placeholder database credentials in `docker/.env`. This file is ignored by Git. Collector stage switches, concurrency, limits, and output directories can also be adjusted there.

Before collecting data, add an authorized source configuration in `CollectRunner.configureTeams()`. The checked-in version deliberately contains no source or client configuration.

## Run with Docker

```shell
docker compose --env-file docker/.env -f docker/docker-compose.yml up --build
```

PostgreSQL is published on port `5440` by default. Inside the Compose network, the application uses the `postgres` service on its internal port `5432`. Exported files are written under `results/`.

## Run locally

Start PostgreSQL first:

```shell
docker compose --env-file docker/.env -f docker/docker-compose.yml up -d postgres
set -a
source docker/.env
set +a
mvn spring-boot:run
```

The `dev` profile is the default and connects to `localhost:${POSTGRES_PORT}`.

## Test

```shell
mvn test
```

## Data model

The initial PostgreSQL schema is stored in `src/main/resources/createTable.sql` and is applied automatically when the Docker volume is created for the first time.
