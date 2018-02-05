-- V0.4.0: Initial schema definition

-- Enumerations

CREATE TYPE ftep_credentials_type AS ENUM ('BASIC', 'X509');
CREATE TYPE ftep_jobs_status AS ENUM ('CREATED', 'RUNNING', 'COMPLETED', 'ERROR', 'CANCELLED');
CREATE TYPE ftep_services_licence AS ENUM ('OPEN', 'RESTRICTED');
CREATE TYPE ftep_services_status AS ENUM ('IN_DEVELOPMENT', 'AVAILABLE');
CREATE TYPE ftep_services_type AS ENUM ('PROCESSOR', 'BULK_PROCESSOR', 'APPLICATION');

-- Tables & Indexes

CREATE TABLE ftep_users (
  uid  SERIAL PRIMARY KEY,
  mail CHARACTER VARYING(255),
  name CHARACTER VARYING(255) NOT NULL
);
CREATE UNIQUE INDEX ftep_users_name_idx
  ON ftep_users (name);

CREATE TABLE ftep_services (
  id             SERIAL PRIMARY KEY,
  description    CHARACTER VARYING(255),
  docker_tag     CHARACTER VARYING(255),
  licence        ftep_services_licence  NOT NULL,
  name           CHARACTER VARYING(255) NOT NULL,
  wps_descriptor TEXT,
  status         ftep_services_status   NOT NULL,
  type           ftep_services_type     NOT NULL,
  owner          INTEGER                NOT NULL REFERENCES ftep_users (uid)
);
CREATE UNIQUE INDEX ftep_services_name_idx
  ON ftep_services (name);
CREATE INDEX ftep_services_owner_idx
  ON ftep_services (owner);

CREATE TABLE ftep_credentials (
  id               SERIAL PRIMARY KEY,
  certificate_path CHARACTER VARYING(255),
  host             CHARACTER VARYING(255) NOT NULL,
  password         CHARACTER VARYING(255),
  type             ftep_credentials_type,
  username         CHARACTER VARYING(255)
);
CREATE UNIQUE INDEX ftep_credentials_host_idx
  ON ftep_credentials (host);

CREATE TABLE ftep_groups (
  gid         SERIAL PRIMARY KEY,
  description CHARACTER VARYING(255),
  name        CHARACTER VARYING(255) NOT NULL,
  owner       INTEGER                NOT NULL REFERENCES ftep_users (uid)
);
CREATE UNIQUE INDEX ftep_groups_name_idx
  ON ftep_groups (name);
CREATE INDEX ftep_groups_owner_idx
  ON ftep_groups (owner);

CREATE TABLE ftep_group_member (
  group_id INTEGER REFERENCES ftep_groups (gid),
  user_id  INTEGER REFERENCES ftep_users (uid)
);

CREATE TABLE ftep_job_configs (
  id      SERIAL PRIMARY KEY,
  inputs  TEXT,
  owner   INTEGER NOT NULL REFERENCES ftep_users (uid),
  service INTEGER NOT NULL REFERENCES ftep_services (id),
  UNIQUE (owner, service, inputs)
);
CREATE INDEX ftep_job_configs_service_idx
  ON ftep_job_configs (service);
CREATE INDEX ftep_job_configs_owner_idx
  ON ftep_job_configs (owner);

CREATE TABLE ftep_jobs (
  id         SERIAL PRIMARY KEY,
  end_time   TIMESTAMP WITHOUT TIME ZONE,
  ext_id     CHARACTER VARYING(255) NOT NULL,
  gui_url    CHARACTER VARYING(255),
  outputs    TEXT,
  stage      CHARACTER VARYING(255),
  start_time TIMESTAMP WITHOUT TIME ZONE,
  status     ftep_jobs_status,
  job_config INTEGER                NOT NULL REFERENCES ftep_job_configs (id),
  owner      INTEGER                NOT NULL REFERENCES ftep_users (uid)
);
CREATE UNIQUE INDEX ftep_jobs_ext_id_idx
  ON ftep_jobs (ext_id);
CREATE INDEX ftep_jobs_job_config_idx
  ON ftep_jobs (job_config);
CREATE INDEX ftep_jobs_owner_idx
  ON ftep_jobs (owner);

-- Initial data

-- Fallback internal user
INSERT INTO ftep_users (name, mail) VALUES ('ftep', 'forestry-tep@esa.int');
