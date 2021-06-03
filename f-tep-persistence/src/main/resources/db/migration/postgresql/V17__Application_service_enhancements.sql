ALTER TABLE ftep_services
    ADD COLUMN IF NOT EXISTS
        strip_proxy_path BOOLEAN DEFAULT true;
