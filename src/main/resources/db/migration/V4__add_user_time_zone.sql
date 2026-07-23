ALTER TABLE users ADD COLUMN time_zone VARCHAR(64);

UPDATE users SET time_zone = 'Etc/UTC';

ALTER TABLE users ALTER COLUMN time_zone SET NOT NULL;
