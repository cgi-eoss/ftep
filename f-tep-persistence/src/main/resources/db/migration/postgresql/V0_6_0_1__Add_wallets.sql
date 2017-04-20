CREATE TABLE ftep_wallets (
  id      BIGSERIAL PRIMARY KEY,
  owner   BIGINT        NOT NULL REFERENCES ftep_users (uid),
  balance INT DEFAULT 0 NOT NULL
);
CREATE UNIQUE INDEX ftep_wallets_owner_idx
  ON ftep_wallets (owner);
