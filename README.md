# 📈 HighLoad++ Siberia 2019 📉

## Demo Project

This is the demo for my presentation on [HighLoad++ Siberia 2019](https://www.highload.ru/siberia/2019) in Novosibirsk
about [TimescaleDB](https://github.com/timescale/timescaledb) and [PipelineDB](https://github.com/pipelinedb/pipelinedb) extensions for PostgreSQL.

Here is the announcement of my speech: 
[https://www.highload.ru/siberia/2019/abstracts/5208](https://www.highload.ru/siberia/2019/abstracts/5208).

> TODO Add links to slides!

### Content

> TODO What's about?

### Getting Started

#### Running on localhost

Required `PostgreSQL` running on port `5432` with database schema. 
It can be done with Docker container:

```bash
docker run \
    --name postgres \
    -e POSTGRES_DB="postgres" \
    -e POSTGRES_USER="postgres" \
    -e POSTGRES_PASSWORD="postgres" \
    -v postgres_data:/var/lib/postgresql/data \
    -v ${PWD}/src/main/resources/sql/init.sql:/docker-entrypoint-initdb.d/init.sql \
    -p 5432:5432 \
    -d binakot/postgresql-timescaledb-pipelinedb
```

Then just build and run the app with your favourite way (e.g. `java -jar` or IDE).

#### Running with Docker (the easiest start)

To package the application into jar file:

```bash
./gradlew shadowJar
```

To build the application docker image:

```bash
docker-compose build
```

To run in Docker with PostgreSQL database:

```bash
docker-compose up -d
```

To stop the application and PostgreSQL database:

```bash
docker-compose down --volumes
```
