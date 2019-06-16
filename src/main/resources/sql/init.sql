-- Regular postgres table with objects
CREATE TABLE objects
(
  id   SERIAL PRIMARY KEY,
  name TEXT NOT NULL UNIQUE
);

-- Hyper-table with telemetries powered by TimescaleDB
CREATE TABLE telemetries
(
  object_id INTEGER     NOT NULL,
  time      TIMESTAMPTZ NOT NULL,
  latitude  DOUBLE PRECISION,
  longitude DOUBLE PRECISION,
  course    REAL,

  CONSTRAINT telemetries_pkey PRIMARY KEY (object_id, time)
);
SELECT create_hypertable('telemetries', 'time');

-- Telemetry input stream powered by PipelineDB
CREATE FOREIGN TABLE telemetry_stream
(
  object_id INTEGER,
  time TIMESTAMPTZ,
  latitude DOUBLE PRECISION,
  longitude DOUBLE PRECISION,
  course REAL,
  speed REAL
) SERVER pipelinedb;

-- View with some stats for objects powered by PipelineDB
CREATE VIEW object_states WITH (action = materialize) AS
SELECT obj.name AS object_name,
       avg(speed) AS avg_speed,
       percentile_cont(0.95) WITHIN GROUP (ORDER BY speed) AS p95_speed
FROM telemetry_stream AS ts
JOIN objects AS obj ON obj.id = ts.object_id
GROUP BY obj.name;

-- Trigger to store telemetry from PipelineDB's stream to TimescaleDB's hyper-table
CREATE OR REPLACE FUNCTION store_telemetry()
  RETURNS trigger AS
$$
BEGIN
  INSERT INTO telemetries (object_id, time, latitude, longitude, course)
  VALUES (NEW.object_id, NEW.time, NEW.latitude, NEW.longitude, NEW.course);
  RETURN NEW;
END;
$$
  LANGUAGE plpgsql;

-- PipelineDB's transform to store telemetries with speed and actual time only
CREATE VIEW telemetry_transform WITH (action = transform, outputfunc = store_telemetry) AS
SELECT object_id::INTEGER,
       time::TIMESTAMPTZ,
       latitude::DOUBLE PRECISION,
       longitude::DOUBLE PRECISION,
       course::REAL
FROM telemetry_stream
WHERE time > now() - INTERVAL '1 day'
  AND speed > 0;
