CREATE TABLE ftep_worker_locator_expressions (
  id         BIGSERIAL PRIMARY KEY,
  service    BIGINT                 NOT NULL REFERENCES ftep_services (id),
  expression CHARACTER VARYING(255) NOT NULL
);
CREATE UNIQUE INDEX ftep_worker_locator_expressions_service_idx
  ON ftep_worker_locator_expressions (service);
