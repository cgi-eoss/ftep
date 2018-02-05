ALTER TABLE ftep_files
  ADD COLUMN datasource BIGINT REFERENCES ftep_data_sources (id);
