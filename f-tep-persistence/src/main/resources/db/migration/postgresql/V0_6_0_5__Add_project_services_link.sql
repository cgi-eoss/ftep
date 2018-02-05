CREATE TABLE ftep_project_services (
  project_id BIGINT REFERENCES ftep_projects (id),
  service_id BIGINT REFERENCES ftep_services (id)
);
CREATE UNIQUE INDEX ftep_project_services_ids_idx
  ON ftep_project_services (project_id, service_id);
