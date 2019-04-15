ALTER TABLE ftep_services
    ADD COLUMN IF NOT EXISTS easy_mode_descriptor TEXT,
    ADD COLUMN IF NOT EXISTS easy_mode_parameter_template TEXT;
