-- JobConfig-input file relationships

CREATE TABLE ftep_job_config_input_files (
  job_config_id BIGINT NOT NULL REFERENCES ftep_job_configs (id),
  file_id       BIGINT NOT NULL REFERENCES ftep_files (id)
);
CREATE UNIQUE INDEX ftep_job_config_input_files_job_config_file_idx
  ON ftep_job_config_input_files (job_config_id, file_id);
