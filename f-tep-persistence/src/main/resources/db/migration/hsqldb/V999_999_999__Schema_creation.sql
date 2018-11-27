-- F-TEP does not support schema migration when using HSQLDB
-- This 'migration' script is primarily to track the current DB schema for use in tests and test environments

-- Tables & Indexes

CREATE TABLE ftep_users (
  uid          BIGINT IDENTITY PRIMARY KEY,
  mail         CHARACTER VARYING(255),
  name         CHARACTER VARYING(255)                 NOT NULL,
  role         CHARACTER VARYING(255) DEFAULT 'GUEST' NOT NULL CHECK (role IN ('GUEST', 'USER', 'EXPERT_USER', 'CONTENT_AUTHORITY', 'ADMIN')),
  organisation CHARACTER VARYING(255)
);
CREATE UNIQUE INDEX ftep_users_name_idx
  ON ftep_users (name);

CREATE TABLE ftep_wallets (
  id      BIGINT IDENTITY PRIMARY KEY,
  owner   BIGINT        NOT NULL FOREIGN KEY REFERENCES ftep_users (uid),
  balance INT DEFAULT 0 NOT NULL
);
CREATE UNIQUE INDEX ftep_wallets_owner_idx
  ON ftep_wallets (owner);

CREATE TABLE ftep_wallet_transactions (
  id               BIGINT IDENTITY PRIMARY KEY,
  wallet           BIGINT                      NOT NULL FOREIGN KEY REFERENCES ftep_wallets (id),
  balance_change   INT                         NOT NULL,
  transaction_time TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  type             CHARACTER VARYING(255)      NOT NULL CHECK (type IN ('CREDIT', 'JOB', 'DOWNLOAD')),
  associated_id    BIGINT
);
CREATE INDEX ftep_wallet_transactions_wallet_idx
  ON ftep_wallet_transactions (wallet);

CREATE TABLE ftep_services (
  id             BIGINT IDENTITY PRIMARY KEY,
  description    CHARACTER VARYING(255),
  docker_tag     CHARACTER VARYING(255),
  licence        CHARACTER VARYING(255) NOT NULL CHECK (licence IN ('OPEN', 'RESTRICTED')),
  name           CHARACTER VARYING(255) NOT NULL,
  wps_descriptor LONGVARCHAR,
  docker_build_info LONGVARCHAR,
  status         CHARACTER VARYING(255) NOT NULL CHECK (status IN ('IN_DEVELOPMENT', 'AVAILABLE')),
  type           CHARACTER VARYING(255) NOT NULL CHECK (type IN ('PROCESSOR', 'BULK_PROCESSOR', 'APPLICATION', 'PARALLEL_PROCESSOR')),
  owner          BIGINT                 NOT NULL FOREIGN KEY REFERENCES ftep_users (uid)
);
CREATE UNIQUE INDEX ftep_services_name_idx
  ON ftep_services (name);
CREATE INDEX ftep_services_owner_idx
  ON ftep_services (owner);

CREATE TABLE ftep_credentials (
  id               BIGINT IDENTITY PRIMARY KEY,
  certificate_path CHARACTER VARYING(255),
  host             CHARACTER VARYING(255) NOT NULL,
  password         CHARACTER VARYING(255),
  type             CHARACTER VARYING(255) NOT NULL CHECK (type IN ('BASIC', 'X509')),
  username         CHARACTER VARYING(255)
);
CREATE UNIQUE INDEX ftep_credentials_host_idx
  ON ftep_credentials (host);

CREATE TABLE ftep_groups (
  gid         BIGINT IDENTITY PRIMARY KEY,
  description CHARACTER VARYING(255),
  name        CHARACTER VARYING(255) NOT NULL,
  owner       BIGINT                 NOT NULL FOREIGN KEY REFERENCES ftep_users (uid)
);
CREATE INDEX ftep_groups_name_idx
  ON ftep_groups (name);
CREATE INDEX ftep_groups_owner_idx
  ON ftep_groups (owner);
CREATE UNIQUE INDEX ftep_groups_name_owner_idx
  ON ftep_groups (name, owner);

CREATE TABLE ftep_group_member (
  group_id BIGINT FOREIGN KEY REFERENCES ftep_groups (gid),
  user_id  BIGINT FOREIGN KEY REFERENCES ftep_users (uid)
);
CREATE UNIQUE INDEX ftep_group_member_user_group_idx
  ON ftep_group_member (group_id, user_id);

CREATE TABLE ftep_job_configs (
  id      BIGINT IDENTITY PRIMARY KEY,
  inputs  LONGVARCHAR,
  parent  BIGINT,
  owner   BIGINT NOT NULL FOREIGN KEY REFERENCES ftep_users (uid),
  service BIGINT NOT NULL FOREIGN KEY REFERENCES ftep_services (id),
  systematic_parameter CHARACTER VARYING(255),
  label   CHARACTER VARYING(255),
  UNIQUE (owner, service, inputs, parent, systematic_parameter)
);
CREATE INDEX ftep_job_configs_service_idx
  ON ftep_job_configs (service);
CREATE INDEX ftep_job_configs_owner_idx
  ON ftep_job_configs (owner);
CREATE INDEX ftep_job_configs_label_idx
  ON ftep_job_configs (label);

CREATE TABLE ftep_jobs (
  id         BIGINT IDENTITY PRIMARY KEY,
  end_time   TIMESTAMP WITHOUT TIME ZONE,
  ext_id     CHARACTER VARYING(255) NOT NULL,
  gui_url    CHARACTER VARYING(255),
  gui_endpoint CHARACTER VARYING(255),
  is_parent  BOOLEAN DEFAULT FALSE,
  outputs    LONGVARCHAR,
  stage      CHARACTER VARYING(255),
  start_time TIMESTAMP WITHOUT TIME ZONE,
  status     CHARACTER VARYING(255) NOT NULL CHECK (status IN ('CREATED', 'RUNNING', 'COMPLETED', 'ERROR', 'CANCELLED')),
  job_config BIGINT                 NOT NULL FOREIGN KEY REFERENCES ftep_job_configs (id),
  owner      BIGINT                 NOT NULL FOREIGN KEY REFERENCES ftep_users (uid),
  parent_job_id BIGINT              REFERENCES ftep_jobs (id),
  worker_id  CHARACTER VARYING(255)
);
CREATE UNIQUE INDEX ftep_jobs_ext_id_idx
  ON ftep_jobs (ext_id);
CREATE INDEX ftep_jobs_job_config_idx
  ON ftep_jobs (job_config);
CREATE INDEX ftep_jobs_owner_idx
  ON ftep_jobs (owner);

-- Reference to parent in job config
ALTER TABLE ftep_job_configs ADD FOREIGN KEY (parent) REFERENCES ftep_jobs (id);

-- Data sources

CREATE TABLE ftep_data_sources (
  id     BIGINT IDENTITY PRIMARY KEY,
  name   CHARACTER VARYING(255) NOT NULL,
  owner  BIGINT                 NOT NULL FOREIGN KEY REFERENCES ftep_users (uid),
  policy CHARACTER VARYING(255) DEFAULT 'CACHE' NOT NULL CHECK (policy IN ('CACHE', 'MIRROR', 'REMOTE_ONLY'))
);
CREATE UNIQUE INDEX ftep_data_sources_name_idx
  ON ftep_data_sources (name);
CREATE INDEX ftep_data_sources_owner_idx
  ON ftep_data_sources (owner);

-- FtepFile and Databasket tables

CREATE TABLE ftep_files (
  id         BIGINT IDENTITY PRIMARY KEY,
  uri        CHARACTER VARYING(255) NOT NULL,
  resto_id   BINARY(255)            NOT NULL,
  type       CHARACTER VARYING(255) CHECK (type IN ('REFERENCE_DATA', 'OUTPUT_PRODUCT', 'EXTERNAL_PRODUCT')),
  owner      BIGINT FOREIGN KEY REFERENCES ftep_users (uid),
  filename   CHARACTER VARYING(255),
  filesize   BIGINT,
  datasource BIGINT FOREIGN KEY REFERENCES ftep_data_sources (id)
);
CREATE UNIQUE INDEX ftep_files_uri_idx
  ON ftep_files (uri);
CREATE UNIQUE INDEX ftep_files_resto_id_idx
  ON ftep_files (resto_id);
CREATE INDEX ftep_files_owner_idx
  ON ftep_files (owner);

CREATE TABLE ftep_databaskets (
  id          BIGINT IDENTITY PRIMARY KEY,
  name        CHARACTER VARYING(255) NOT NULL,
  description CHARACTER VARYING(255),
  owner       BIGINT FOREIGN KEY REFERENCES ftep_users (uid)
);
CREATE INDEX ftep_databaskets_name_idx
  ON ftep_databaskets (name);
CREATE INDEX ftep_databaskets_owner_idx
  ON ftep_databaskets (owner);
CREATE UNIQUE INDEX ftep_databaskets_name_owner_idx
  ON ftep_databaskets (name, owner);

CREATE TABLE ftep_databasket_files (
  databasket_id BIGINT FOREIGN KEY REFERENCES ftep_databaskets (id),
  file_id       BIGINT FOREIGN KEY REFERENCES ftep_files (id)
);
CREATE UNIQUE INDEX ftep_databasket_files_basket_file_idx
  ON ftep_databasket_files (databasket_id, file_id);

CREATE TABLE ftep_projects (
  id          BIGINT IDENTITY PRIMARY KEY,
  name        CHARACTER VARYING(255) NOT NULL,
  description CHARACTER VARYING(255),
  owner       BIGINT FOREIGN KEY REFERENCES ftep_users (uid)
);
CREATE INDEX ftep_projects_name_idx
  ON ftep_projects (name);
CREATE INDEX ftep_projects_owner_idx
  ON ftep_projects (owner);
CREATE UNIQUE INDEX ftep_projects_name_owner_idx
  ON ftep_projects (name, owner);

CREATE TABLE ftep_project_databaskets (
  project_id    BIGINT FOREIGN KEY REFERENCES ftep_projects (id),
  databasket_id BIGINT FOREIGN KEY REFERENCES ftep_databaskets (id)
);
CREATE UNIQUE INDEX ftep_project_databaskets_ids_idx
  ON ftep_project_databaskets (project_id, databasket_id);

CREATE TABLE ftep_project_services (
  project_id BIGINT FOREIGN KEY REFERENCES ftep_projects (id),
  service_id BIGINT FOREIGN KEY REFERENCES ftep_services (id)
);
CREATE UNIQUE INDEX ftep_project_services_ids_idx
  ON ftep_project_services (project_id, service_id);

CREATE TABLE ftep_project_job_configs (
  project_id    BIGINT FOREIGN KEY REFERENCES ftep_projects (id),
  job_config_id BIGINT FOREIGN KEY REFERENCES ftep_job_configs (id)
);
CREATE UNIQUE INDEX ftep_project_job_configs_ids_idx
  ON ftep_project_job_configs (project_id, job_config_id);

-- FtepServiceContextFile table

CREATE TABLE ftep_service_files (
  id         BIGINT IDENTITY PRIMARY KEY,
  service    BIGINT                NOT NULL FOREIGN KEY REFERENCES ftep_services (id),
  filename   CHARACTER VARYING(255),
  executable BOOLEAN DEFAULT FALSE NOT NULL,
  content    LONGVARCHAR
);
CREATE UNIQUE INDEX ftep_service_files_filename_service_idx
  ON ftep_service_files (filename, service);
CREATE INDEX ftep_service_files_filename_idx
  ON ftep_service_files (filename);
CREATE INDEX ftep_service_files_service_idx
  ON ftep_service_files (service);

-- Cost expressions

CREATE TABLE ftep_costing_expressions (
  id                        BIGINT IDENTITY PRIMARY KEY,
  type                      CHARACTER VARYING(255) NOT NULL CHECK (type IN ('SERVICE', 'DOWNLOAD')),
  associated_id             BIGINT                 NOT NULL,
  cost_expression           CHARACTER VARYING(255) NOT NULL,
  estimated_cost_expression CHARACTER VARYING(255)
);
CREATE UNIQUE INDEX ftep_costing_expressions_type_associated_id_idx
  ON ftep_costing_expressions (type, associated_id);

-- Worker expressions

CREATE TABLE ftep_worker_locator_expressions (
  id         BIGINT IDENTITY PRIMARY KEY,
  service    BIGINT                 NOT NULL FOREIGN KEY REFERENCES ftep_services (id),
  expression CHARACTER VARYING(255) NOT NULL
);
CREATE UNIQUE INDEX ftep_worker_locator_expressions_service_idx
  ON ftep_worker_locator_expressions (service);

-- Publishing requests

CREATE TABLE ftep_publishing_requests (
  id            BIGINT IDENTITY PRIMARY KEY,
  owner         BIGINT                 NOT NULL FOREIGN KEY REFERENCES ftep_users (uid),
  request_time  TIMESTAMP WITHOUT TIME ZONE,
  updated_time  TIMESTAMP WITHOUT TIME ZONE,
  status        CHARACTER VARYING(255) NOT NULL CHECK (status IN
                                                       ('REQUESTED', 'GRANTED', 'NEEDS_INFO', 'REJECTED')),
  type          CHARACTER VARYING(255) NOT NULL CHECK (type IN
                                                       ('DATABASKET', 'DATASOURCE', 'FILE', 'SERVICE', 'GROUP', 'JOB_CONFIG', 'PROJECT')),
  associated_id BIGINT                 NOT NULL
);
CREATE INDEX ftep_publishing_requests_owner_idx
  ON ftep_publishing_requests (owner);
CREATE UNIQUE INDEX ftep_publishing_requests_owner_object_idx
  ON ftep_publishing_requests (owner, type, associated_id);

-- JobConfig-input file relationships

CREATE TABLE ftep_job_config_input_files (
  job_config_id BIGINT NOT NULL REFERENCES ftep_job_configs (id),
  file_id       BIGINT NOT NULL REFERENCES ftep_files (id)
);
CREATE UNIQUE INDEX ftep_job_config_input_files_job_config_file_idx
  ON ftep_job_config_input_files (job_config_id, file_id);

-- Job-output file relationships

CREATE TABLE ftep_job_output_files (
  job_id  BIGINT NOT NULL FOREIGN KEY REFERENCES ftep_jobs (id),
  file_id BIGINT NOT NULL FOREIGN KEY REFERENCES ftep_files (id)
);
CREATE UNIQUE INDEX ftep_job_output_files_job_file_idx
  ON ftep_job_output_files (job_id, file_id);

-- ACL schema from spring-security-acl

CREATE TABLE acl_sid (
  id        BIGINT GENERATED BY DEFAULT AS IDENTITY (
  START WITH 100 )                  NOT NULL PRIMARY KEY,
  principal BOOLEAN                 NOT NULL,
  sid       VARCHAR_IGNORECASE(100) NOT NULL,
  UNIQUE (sid, principal)
);

CREATE TABLE acl_class (
  id    BIGINT GENERATED BY DEFAULT AS IDENTITY (
  START WITH 100 )              NOT NULL PRIMARY KEY,
  class VARCHAR_IGNORECASE(100) NOT NULL,
  UNIQUE (class)
);

CREATE TABLE acl_object_identity (
  id                 BIGINT GENERATED BY DEFAULT AS IDENTITY (
  START WITH 100 )           NOT NULL PRIMARY KEY,
  object_id_class    BIGINT  NOT NULL FOREIGN KEY REFERENCES acl_class (id),
  object_id_identity BIGINT  NOT NULL,
  parent_object      BIGINT FOREIGN KEY REFERENCES acl_object_identity (id),
  owner_sid          BIGINT FOREIGN KEY REFERENCES acl_sid (id),
  entries_inheriting BOOLEAN NOT NULL,
  UNIQUE (object_id_class, object_id_identity)
);

CREATE TABLE acl_entry (
  id                  BIGINT GENERATED BY DEFAULT AS IDENTITY (
  START WITH 100 )            NOT NULL PRIMARY KEY,
  acl_object_identity BIGINT  NOT NULL FOREIGN KEY REFERENCES acl_object_identity (id),
  ace_order           INT     NOT NULL,
  sid                 BIGINT  NOT NULL FOREIGN KEY REFERENCES acl_sid (id),
  mask                INTEGER NOT NULL,
  granting            BOOLEAN NOT NULL,
  audit_success       BOOLEAN NOT NULL,
  audit_failure       BOOLEAN NOT NULL,
  UNIQUE (acl_object_identity, ace_order)
);

-- Initial data

-- Fallback internal user
INSERT INTO ftep_users (name, mail) VALUES ('ftep', 'forestry-tep@esa.int');

-- Default project
INSERT INTO ftep_projects (name, owner) VALUES
  ('Default Project', (SELECT uid FROM ftep_users WHERE name = 'ftep'));

-- F-TEP datasource
INSERT INTO ftep_data_sources (name, owner) VALUES
  ('F-TEP', (SELECT uid FROM ftep_users WHERE name = 'ftep'));

-- Systematic Processing
CREATE TABLE ftep_systematic_processings (
  id      BIGINT GENERATED BY DEFAULT AS IDENTITY ( START WITH 100 ) PRIMARY KEY,
  owner   BIGINT    NOT NULL REFERENCES ftep_users (uid),
  status  CHARACTER VARYING(255) NOT NULL CHECK (status IN ('ACTIVE', 'BLOCKED', 'COMPLETED')),
  parent_job BIGINT NOT NULL REFERENCES ftep_jobs (id),
  last_updated TIMESTAMP WITHOUT TIME ZONE,
  search_parameters LONGVARCHAR
);
