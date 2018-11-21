--Service Docker Build Info
ALTER TABLE ftep_services
  ADD COLUMN IF NOT EXISTS docker_build_info TEXT;

--Add worker id to job
ALTER TABLE ftep_jobs
  ADD COLUMN IF NOT EXISTS worker_id     CHARACTER VARYING(255);
-- Insert the relation
ALTER TABLE ftep_jobs
  ADD COLUMN IF NOT EXISTS parent_job_id BIGINT REFERENCES ftep_jobs (id);
--Add isParent boolean to job
ALTER TABLE ftep_jobs
  ADD COLUMN IF NOT EXISTS is_parent     BOOLEAN DEFAULT FALSE;
--Collections Publication Request
ALTER TABLE ftep_jobs
  ADD COLUMN IF NOT EXISTS gui_endpoint  CHARACTER VARYING(255);

--Reference to parent in job config
ALTER TABLE ftep_job_configs
  ADD COLUMN IF NOT EXISTS parent BIGINT REFERENCES ftep_jobs(id);
