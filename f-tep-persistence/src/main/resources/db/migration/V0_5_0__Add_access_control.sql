-- Create default application roles
CREATE TYPE ftep_roles AS ENUM ('GUEST', 'USER', 'EXPERT_USER', 'CONTENT_AUTHORITY', 'ADMIN');

ALTER TABLE ftep_users
  ADD COLUMN
  role ftep_roles NOT NULL DEFAULT 'GUEST';

-- Set all id fields to Long (BIGINT/BIGSERIAL)

ALTER TABLE ftep_users
  ALTER COLUMN uid TYPE BIGINT;
ALTER TABLE ftep_services
  ALTER COLUMN id TYPE BIGINT,
  ALTER COLUMN owner TYPE BIGINT;
ALTER TABLE ftep_credentials
  ALTER COLUMN id TYPE BIGINT;
ALTER TABLE ftep_groups
  ALTER COLUMN gid TYPE BIGINT,
  ALTER COLUMN owner TYPE BIGINT;
ALTER TABLE ftep_group_member
  ALTER COLUMN group_id TYPE BIGINT,
  ALTER COLUMN user_id TYPE BIGINT;
ALTER TABLE ftep_job_configs
  ALTER COLUMN id TYPE BIGINT,
  ALTER COLUMN owner TYPE BIGINT,
  ALTER COLUMN service TYPE BIGINT;
ALTER TABLE ftep_jobs
  ALTER COLUMN id TYPE BIGINT,
  ALTER COLUMN owner TYPE BIGINT,
  ALTER COLUMN job_config TYPE BIGINT;

-- ACL schema from spring-security-acl

CREATE TABLE acl_sid (
  id        BIGSERIAL    NOT NULL PRIMARY KEY,
  principal BOOLEAN      NOT NULL,
  sid       VARCHAR(100) NOT NULL,
  UNIQUE (sid, principal)
);

CREATE TABLE acl_class (
  id    BIGSERIAL    NOT NULL PRIMARY KEY,
  class VARCHAR(100) NOT NULL,
  UNIQUE (class)
);

CREATE TABLE acl_object_identity (
  id                 BIGSERIAL PRIMARY KEY,
  object_id_class    BIGINT  NOT NULL REFERENCES acl_class (id),
  object_id_identity BIGINT  NOT NULL,
  parent_object      BIGINT REFERENCES acl_object_identity (id),
  owner_sid          BIGINT REFERENCES acl_sid (id),
  entries_inheriting BOOLEAN NOT NULL,
  UNIQUE (object_id_class, object_id_identity)
);

CREATE TABLE acl_entry (
  id                  BIGSERIAL PRIMARY KEY,
  acl_object_identity BIGINT  NOT NULL REFERENCES acl_object_identity (id),
  ace_order           INT     NOT NULL,
  sid                 BIGINT  NOT NULL REFERENCES acl_sid (id),
  mask                INTEGER NOT NULL,
  granting            BOOLEAN NOT NULL,
  audit_success       BOOLEAN NOT NULL,
  audit_failure       BOOLEAN NOT NULL,
  UNIQUE (acl_object_identity, ace_order)
);
