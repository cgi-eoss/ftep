-- Job-output file relationships

CREATE TABLE ftep_job_output_files (
  job_id  BIGINT NOT NULL REFERENCES ftep_jobs (id),
  file_id BIGINT NOT NULL REFERENCES ftep_files (id)
);
CREATE UNIQUE INDEX ftep_job_output_files_job_file_idx
  ON ftep_job_output_files (job_id, file_id);
