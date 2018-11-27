--Systematic Processing

CREATE TYPE ftep_systematic_processing_status AS ENUM ('ACTIVE', 'BLOCKED' ,'COMPLETED');

CREATE TABLE IF NOT EXISTS ftep_systematic_processings (
  id                BIGSERIAL PRIMARY KEY,
  owner             BIGINT    NOT NULL REFERENCES ftep_users (uid),
  status            ftep_systematic_processing_status,
  parent_job        BIGINT    NOT NULL REFERENCES ftep_jobs (id) ON DELETE CASCADE,
  last_updated      TIMESTAMP WITHOUT TIME ZONE,
  search_parameters TEXT
);

CREATE INDEX ftep_systematic_processing_owner_idx ON ftep_systematic_processings (owner);

ALTER TABLE ftep_job_configs
  ADD COLUMN systematic_parameter CHARACTER VARYING(255);
