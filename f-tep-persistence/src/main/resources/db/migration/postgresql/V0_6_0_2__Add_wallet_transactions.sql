CREATE TYPE FTEP_WALLET_TRANSACTIONS_TYPE AS ENUM ('CREDIT', 'JOB', 'DOWNLOAD');

CREATE OR REPLACE FUNCTION ftep_wallet_transactions_type_cast(VARCHAR)
  RETURNS FTEP_WALLET_TRANSACTIONS_TYPE AS $$ SELECT
                                                ('' || $1) :: FTEP_WALLET_TRANSACTIONS_TYPE $$ LANGUAGE SQL IMMUTABLE;
CREATE CAST ( VARCHAR AS FTEP_WALLET_TRANSACTIONS_TYPE )
WITH FUNCTION ftep_wallet_transactions_type_cast(VARCHAR) AS IMPLICIT;

CREATE TABLE ftep_wallet_transactions (
  id               BIGSERIAL PRIMARY KEY,
  wallet           BIGINT                        NOT NULL REFERENCES ftep_wallets (id),
  balance_change   INT                           NOT NULL,
  transaction_time TIMESTAMP WITHOUT TIME ZONE   NOT NULL,
  type             FTEP_WALLET_TRANSACTIONS_TYPE NOT NULL,
  associated_id    BIGINT
);
CREATE INDEX ftep_wallet_transactions_wallet_idx
  ON ftep_wallet_transactions (wallet);
