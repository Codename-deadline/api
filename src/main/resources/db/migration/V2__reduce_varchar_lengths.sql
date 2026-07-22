ALTER TABLE users
    ALTER COLUMN password TYPE varchar(64),
    ALTER COLUMN language TYPE varchar(3),
    ALTER COLUMN role TYPE varchar(16);

ALTER TABLE organizations
    ALTER COLUMN type TYPE varchar(8);

ALTER TABLE bots
    ALTER COLUMN messenger TYPE varchar(16),
    DROP CONSTRAINT uq_bots_bot_id_messenger,
    ADD CONSTRAINT uq_bots_messenger UNIQUE (messenger);

ALTER TABLE chats
    ALTER COLUMN messenger TYPE varchar(16),
    ALTER COLUMN language TYPE varchar(3);

ALTER TABLE refresh_tokens
    ALTER COLUMN jti TYPE varchar(64);

ALTER TABLE organization_invitations
    ALTER COLUMN status TYPE varchar(8),
    ALTER COLUMN role TYPE varchar(16);

ALTER TABLE user_scopes
    ALTER COLUMN role TYPE varchar(16);

ALTER TABLE user_messenger_accounts
    ALTER COLUMN messenger TYPE varchar(16);

ALTER TABLE deadline_attachments
    ALTER COLUMN object_key TYPE varchar(64);

ALTER TABLE deadline_notifications
    ALTER COLUMN type TYPE varchar(16),
    DROP CONSTRAINT ck_deadline_notifications_type,
    ADD CONSTRAINT ck_deadline_notifications_type CHECK (
        type IN ('FIFTEEN_MINUTES', 'ONE_HOUR', 'ONE_DAY', 'ONE_WEEK', 'ONE_MONTH', 'NO_TIME')
    );

ALTER TABLE notification_outbox
    ALTER COLUMN source TYPE varchar(32),
    ALTER COLUMN messenger TYPE varchar(16),
    ALTER COLUMN topic TYPE varchar(64);
