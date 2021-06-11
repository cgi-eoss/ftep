ALTER TABLE ftep_subscriptions
    ALTER COLUMN processing_quota          TYPE DOUBLE PRECISION,
    ALTER COLUMN processing_quota_usage    TYPE DOUBLE PRECISION;