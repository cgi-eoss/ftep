 --API keys

 CREATE TABLE ftep_api_keys (
  id      BIGSERIAL PRIMARY KEY,
  owner   BIGINT    NOT NULL REFERENCES ftep_users (uid),
  api_key  CHARACTER VARYING(255)
  );

CREATE UNIQUE INDEX ftep_api_keys_owner_idx
ON ftep_api_keys (owner);
