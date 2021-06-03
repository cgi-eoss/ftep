CREATE TABLE ftep_subscriptions (
    id                      BIGSERIAL PRIMARY KEY,
    owner                   BIGINT NOT NULL REFERENCES ftep_users (uid),
    package_name            CHARACTER VARYING(255),
    storage_quota           BIGINT,
    processing_quota        BIGINT,
    comment_text            TEXT,
    subscription_start      TIMESTAMP WITHOUT TIME ZONE,
    subscription_end        TIMESTAMP WITHOUT TIME ZONE,
    storage_quota_usage     BIGINT DEFAULT 0,
    processing_quota_usage  BIGINT DEFAULT 0,
    creation_time           TIMESTAMP WITHOUT TIME ZONE,
    creator                 BIGINT REFERENCES ftep_users (uid),
    cancellation_time       TIMESTAMP WITHOUT TIME ZONE,
    canceller               BIGINT REFERENCES ftep_users (uid)
);
CREATE INDEX ftep_subscriptions_owner_idx ON ftep_subscriptions (owner);
CREATE INDEX ftep_subscriptions_creator_idx ON ftep_subscriptions (creator);
CREATE INDEX ftep_subscriptions_canceller_idx ON ftep_subscriptions (canceller);

CREATE TABLE ftep_comments (
    id                      BIGSERIAL PRIMARY KEY,
    owner                   BIGINT NOT NULL REFERENCES ftep_users (uid),
    creation_time           TIMESTAMP WITHOUT TIME ZONE,
    creator                 BIGINT REFERENCES ftep_users (uid),
    comment_text            TEXT
);
CREATE INDEX ftep_comments_owner_idx ON ftep_comments (owner);
CREATE INDEX ftep_comments_creator_idx ON ftep_comments (creator);

-- migrate subscription data from the users table to the subscriptions table
INSERT INTO ftep_subscriptions (owner, subscription_start, subscription_end)
    SELECT uid, subscription_start, subscription_start + interval '30 days'
        FROM ftep_users
        WHERE subscription_start IS NOT NULL;

ALTER TABLE ftep_users
    DROP COLUMN IF EXISTS subscription_start;
