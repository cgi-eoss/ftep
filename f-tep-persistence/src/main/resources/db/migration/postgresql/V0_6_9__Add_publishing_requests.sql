-- Publishing requests

CREATE TYPE FTEP_PUBLISHING_REQUESTS_STATUS AS ENUM ('REQUESTED', 'GRANTED', 'NEEDS_INFO', 'REJECTED');

CREATE OR REPLACE FUNCTION ftep_publishing_requests_status_cast(VARCHAR)
  RETURNS FTEP_PUBLISHING_REQUESTS_STATUS AS $$ SELECT ('' ||
                                                        $1) :: FTEP_PUBLISHING_REQUESTS_STATUS $$ LANGUAGE SQL IMMUTABLE;
CREATE CAST ( VARCHAR AS FTEP_PUBLISHING_REQUESTS_STATUS )
WITH FUNCTION ftep_publishing_requests_status_cast(VARCHAR) AS IMPLICIT;


CREATE TYPE FTEP_PUBLISHING_REQUESTS_OBJECT_TYPE AS ENUM ('DATABASKET', 'DATASOURCE', 'FILE', 'SERVICE', 'GROUP', 'JOB_CONFIG', 'PROJECT');

CREATE OR REPLACE FUNCTION ftep_publishing_requests_OBJECT_TYPE_cast(VARCHAR)
  RETURNS FTEP_PUBLISHING_REQUESTS_OBJECT_TYPE AS $$ SELECT ('' ||
                                                             $1) :: FTEP_PUBLISHING_REQUESTS_OBJECT_TYPE $$ LANGUAGE SQL IMMUTABLE;
CREATE CAST ( VARCHAR AS FTEP_PUBLISHING_REQUESTS_OBJECT_TYPE )
WITH FUNCTION ftep_publishing_requests_OBJECT_TYPE_cast(VARCHAR) AS IMPLICIT;


CREATE TABLE ftep_publishing_requests (
  id            BIGSERIAL PRIMARY KEY,
  owner         BIGINT                               NOT NULL REFERENCES ftep_users (uid),
  request_time  TIMESTAMP WITHOUT TIME ZONE,
  updated_time  TIMESTAMP WITHOUT TIME ZONE,
  status        FTEP_PUBLISHING_REQUESTS_STATUS      NOT NULL,
  type          FTEP_PUBLISHING_REQUESTS_OBJECT_TYPE NOT NULL,
  associated_id BIGINT                               NOT NULL
);
CREATE INDEX ftep_publishing_requests_owner_idx
  ON ftep_publishing_requests (owner);
CREATE UNIQUE INDEX ftep_publishing_requests_owner_object_idx
  ON ftep_publishing_requests (owner, type, associated_id);
