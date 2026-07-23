ALTER TABLE chats ADD COLUMN time_zone VARCHAR(64);

UPDATE chats SET time_zone = 'Etc/UTC';

ALTER TABLE chats ALTER COLUMN time_zone SET NOT NULL;

UPDATE notification_outbox
SET payload = jsonb_set(payload, '{timeZone}', '"Etc/UTC"')
WHERE source = 'DDL_EXPIRATION'
    AND NOT payload ? 'timeZone';
