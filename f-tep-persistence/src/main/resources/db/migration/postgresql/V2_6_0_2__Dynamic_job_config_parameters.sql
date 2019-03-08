ALTER TABLE ftep_job_configs
  ADD COLUMN IF NOT EXISTS parallel_parameters CHARACTER VARYING(255) NOT NULL DEFAULT '',
  ADD COLUMN IF NOT EXISTS search_parameters CHARACTER VARYING(255) NOT NULL DEFAULT '';

DROP INDEX IF EXISTS ftep_job_configs_owner_service_inputs_idx;

CREATE UNIQUE INDEX ftep_job_configs_unique_idx ON ftep_job_configs
  (owner, service, inputs, parent, systematic_parameter, parallel_parameters, search_parameters);

-- Remove PARALLEL_PROCESSOR enum value

CREATE TYPE ftep_services_type_new AS ENUM ('PROCESSOR', 'BULK_PROCESSOR', 'APPLICATION');

UPDATE ftep_services s SET type = 'PROCESSOR' WHERE s.type = 'PARALLEL_PROCESSOR';

ALTER TABLE ftep_services
  ALTER COLUMN type TYPE ftep_services_type_new
    USING (type::text::ftep_services_type_new);

DROP CAST IF EXISTS ( VARCHAR AS ftep_services_type );
DROP FUNCTION IF EXISTS ftep_services_type_cast(VARCHAR);

DROP TYPE ftep_services_type;

ALTER TYPE ftep_services_type_new RENAME TO ftep_services_type;

CREATE FUNCTION ftep_services_type_cast(VARCHAR)
  RETURNS ftep_services_type AS $$ SELECT ('' || $1) :: ftep_services_type $$ LANGUAGE SQL IMMUTABLE;
CREATE CAST ( VARCHAR AS ftep_services_type )
  WITH FUNCTION ftep_services_type_cast(VARCHAR) AS IMPLICIT;
