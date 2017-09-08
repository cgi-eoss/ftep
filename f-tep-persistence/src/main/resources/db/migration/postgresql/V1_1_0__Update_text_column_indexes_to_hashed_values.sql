-- Remove old index/constraint, and create new index&constraint with md5(inputs) to avoid large btree row values
ALTER TABLE ONLY ftep_job_configs
  DROP CONSTRAINT IF EXISTS ftep_job_configs_owner_service_inputs_key;
DROP INDEX IF EXISTS ftep_job_configs_owner_service_inputs_idx;

-- No constraint is possible due to md5() expression, the index must suffice to enforce uniqueness
CREATE UNIQUE INDEX ftep_job_configs_owner_service_inputs_idx
  ON ftep_job_configs USING BTREE (owner, service, md5(inputs));
REINDEX TABLE ftep_job_configs;
