-- F-TEP does not support schema migration when using HSQLDB
-- This 'migration' script is primarily to track the current DB schema for use in tests and test environments

-- Tables & Indexes

CREATE TABLE ftep_users (
  uid  BIGINT IDENTITY PRIMARY KEY,
  mail CHARACTER VARYING(255),
  name CHARACTER VARYING(255)                 NOT NULL,
  role CHARACTER VARYING(255) DEFAULT 'GUEST' NOT NULL CHECK (role IN
                                                              ('GUEST', 'USER', 'EXPERT_USER', 'CONTENT_AUTHORITY', 'ADMIN'))
);
CREATE UNIQUE INDEX ftep_users_name_idx
  ON ftep_users (name);

CREATE TABLE ftep_services (
  id             BIGINT IDENTITY PRIMARY KEY,
  description    CHARACTER VARYING(255),
  docker_tag     CHARACTER VARYING(255),
  licence        CHARACTER VARYING(255) NOT NULL CHECK (licence IN ('OPEN', 'RESTRICTED')),
  name           CHARACTER VARYING(255) NOT NULL,
  wps_descriptor CLOB,
  status         CHARACTER VARYING(255) NOT NULL CHECK (status IN ('IN_DEVELOPMENT', 'AVAILABLE')),
  type           CHARACTER VARYING(255) NOT NULL CHECK (type IN ('PROCESSOR', 'BULK_PROCESSOR', 'APPLICATION')),
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
  inputs  CLOB,
  owner   BIGINT NOT NULL FOREIGN KEY REFERENCES ftep_users (uid),
  service BIGINT NOT NULL FOREIGN KEY REFERENCES ftep_services (id)
  -- WARNING: No unique index on owner-service-inputs as hsqldb 2.3.4 cannot index CLOB columns
  -- UNIQUE (owner, service, inputs)
);
CREATE INDEX ftep_job_configs_service_idx
  ON ftep_job_configs (service);
CREATE INDEX ftep_job_configs_owner_idx
  ON ftep_job_configs (owner);

CREATE TABLE ftep_jobs (
  id         BIGINT IDENTITY PRIMARY KEY,
  end_time   TIMESTAMP WITHOUT TIME ZONE,
  ext_id     CHARACTER VARYING(255) NOT NULL,
  gui_url    CHARACTER VARYING(255),
  outputs    CLOB,
  stage      CHARACTER VARYING(255),
  start_time TIMESTAMP WITHOUT TIME ZONE,
  status     CHARACTER VARYING(255) NOT NULL CHECK (status IN
                                                    ('CREATED', 'RUNNING', 'COMPLETED', 'ERROR', 'CANCELLED')),
  job_config BIGINT                 NOT NULL FOREIGN KEY REFERENCES ftep_job_configs (id),
  owner      BIGINT                 NOT NULL FOREIGN KEY REFERENCES ftep_users (uid)
);
CREATE UNIQUE INDEX ftep_jobs_ext_id_idx
  ON ftep_jobs (ext_id);
CREATE INDEX ftep_jobs_job_config_idx
  ON ftep_jobs (job_config);
CREATE INDEX ftep_jobs_owner_idx
  ON ftep_jobs (owner);

-- FtepFile and Databasket tables

CREATE TABLE ftep_files (
  id       BIGINT IDENTITY PRIMARY KEY,
  uri      CHARACTER VARYING(255) NOT NULL,
  resto_id BINARY(255)            NOT NULL,
  type     CHARACTER VARYING(255) CHECK (type IN ('REFERENCE_DATA', 'OUTPUT_PRODUCT', 'EXTERNAL_PRODUCT')),
  owner    BIGINT FOREIGN KEY REFERENCES ftep_users (uid),
  filename CHARACTER VARYING(255)
);
CREATE UNIQUE INDEX ftep_files_uri_idx
  ON ftep_files (uri);
CREATE UNIQUE INDEX ftep_files_resto_id_idx
  ON ftep_files (resto_id);
CREATE INDEX ftep_files_owner_idx
  ON ftep_files (owner);

CREATE TABLE ftep_databaskets (
  id    BIGINT IDENTITY PRIMARY KEY,
  name  CHARACTER VARYING(255) NOT NULL,
  owner BIGINT FOREIGN KEY REFERENCES ftep_users (uid)
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