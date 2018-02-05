ALTER TABLE ftep_job_configs
  ADD COLUMN label CHARACTER VARYING(255);
CREATE INDEX ftep_job_configs_label_idx
  ON ftep_job_configs (label);

