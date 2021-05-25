CREATE TYPE ftep_services_type_new AS ENUM ('PROCESSOR', 'APPLICATION');

UPDATE ftep_services SET type = 'PROCESSOR' WHERE type = 'BULK_PROCESSOR';

ALTER TABLE ftep_services
    ALTER COLUMN type TYPE ftep_services_type_new
        USING (type::text::ftep_services_type_new);

DROP CAST IF EXISTS (VARCHAR AS ftep_services_type);

DROP FUNCTION IF EXISTS ftep_services_type_cast(VARCHAR);

DROP TYPE ftep_services_type;

ALTER TYPE ftep_services_type_new RENAME TO ftep_services_type;

CREATE FUNCTION ftep_services_type_cast(VARCHAR)
    RETURNS ftep_services_type AS $$ SELECT ('' || $1) :: ftep_services_type $$ LANGUAGE SQL IMMUTABLE;

CREATE CAST (VARCHAR AS ftep_services_type)
    WITH FUNCTION ftep_services_type_cast(VARCHAR) AS IMPLICIT;