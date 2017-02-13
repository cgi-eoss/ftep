-- Create default application roles
CREATE TYPE ftep_roles AS ENUM ('GUEST', 'USER', 'EXPERT_USER', 'CONTENT_AUTHORITY', 'ADMIN');

ALTER TABLE ftep_users
  ADD COLUMN
  role ftep_roles NOT NULL DEFAULT 'GUEST';
