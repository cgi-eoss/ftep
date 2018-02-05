-- Fix column type on ftep_files

ALTER TABLE ftep_files
  ALTER COLUMN type TYPE ftep_files_type USING type :: VARCHAR(255);

-- Create new table for service context files

CREATE TABLE ftep_service_files (
  id         BIGSERIAL PRIMARY KEY,
  service    BIGINT                NOT NULL REFERENCES ftep_services (id),
  filename   CHARACTER VARYING(255),
  executable BOOLEAN DEFAULT FALSE NOT NULL,
  content    TEXT
);
CREATE UNIQUE INDEX ftep_service_files_filename_service_idx
  ON ftep_service_files (filename, service);
CREATE INDEX ftep_service_files_filename_idx
  ON ftep_service_files (filename);
CREATE INDEX ftep_service_files_service_idx
  ON ftep_service_files (service);
