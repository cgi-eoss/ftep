-- Update FtepFiles table with new filesize column

ALTER TABLE ftep_files
  ADD COLUMN filesize BIGINT;

-- Data sources

CREATE TABLE ftep_data_sources (
  id    BIGSERIAL PRIMARY KEY,
  name  CHARACTER VARYING(255) NOT NULL,
  owner BIGINT                 NOT NULL REFERENCES ftep_users (uid)
);
CREATE UNIQUE INDEX ftep_data_sources_name_idx
  ON ftep_data_sources (name);
CREATE INDEX ftep_data_sources_owner_idx
  ON ftep_data_sources (owner);

-- Cost expressions

CREATE TYPE FTEP_COSTING_EXPRESSIONS_TYPE AS ENUM ('SERVICE', 'DOWNLOAD');

CREATE OR REPLACE FUNCTION ftep_costing_expressions_type_cast(VARCHAR)
  RETURNS FTEP_COSTING_EXPRESSIONS_TYPE AS $$ SELECT
                                                ('' || $1) :: FTEP_COSTING_EXPRESSIONS_TYPE $$ LANGUAGE SQL IMMUTABLE;
CREATE CAST ( VARCHAR AS FTEP_COSTING_EXPRESSIONS_TYPE )
WITH FUNCTION ftep_costing_expressions_type_cast(VARCHAR) AS IMPLICIT;


CREATE TABLE ftep_costing_expressions (
  id                        BIGSERIAL PRIMARY KEY,
  type                      FTEP_COSTING_EXPRESSIONS_TYPE NOT NULL,
  associated_id             BIGINT                        NOT NULL,
  cost_expression           CHARACTER VARYING(255)        NOT NULL,
  estimated_cost_expression CHARACTER VARYING(255)
);
CREATE UNIQUE INDEX ftep_costing_expressions_type_associated_id_idx
  ON ftep_costing_expressions (type, associated_id);
