CREATE SEQUENCE user_sequence START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE token_sequence START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE org_sequence START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE org_inv_sequence START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE thread_sequence START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE deadline_sequence START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE ddl_attach_sequence START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE notification_sequence START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE bots_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE chats_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE user_messenger_accounts_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE users (
    id bigint NOT NULL,
    username varchar(32) NOT NULL,
    joined_at timestamp with time zone NOT NULL,
    full_name varchar(128) NOT NULL,
    password varchar(255),
    language varchar(255) NOT NULL,
    last_password_change timestamp with time zone NOT NULL,
    role varchar(255) NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT ck_users_username CHECK (
        char_length(username) BETWEEN 3 AND 32 AND btrim(username) <> ''
    ),
    CONSTRAINT ck_users_full_name CHECK (
        char_length(full_name) BETWEEN 2 AND 128 AND btrim(full_name) <> ''
    ),
    CONSTRAINT ck_users_language CHECK (language IN ('RU', 'EN')),
    CONSTRAINT ck_users_role CHECK (role IN ('USER', 'ADMIN'))
);

CREATE TABLE organizations (
    id bigint NOT NULL,
    title varchar(128) NOT NULL,
    description varchar(4096),
    type varchar(255) NOT NULL,
    created_at timestamp with time zone NOT NULL,
    CONSTRAINT pk_organizations PRIMARY KEY (id),
    CONSTRAINT ck_organizations_title CHECK (
        char_length(title) BETWEEN 2 AND 128 AND btrim(title) <> ''
    ),
    CONSTRAINT ck_organizations_type CHECK (type IN ('PUBLIC', 'PRIVATE', 'PERSONAL'))
);

CREATE TABLE bots (
    id bigint NOT NULL,
    messenger varchar(255) NOT NULL,
    bot_id bigint NOT NULL,
    username varchar(64) NOT NULL,
    CONSTRAINT pk_bots PRIMARY KEY (id),
    CONSTRAINT uq_bots_bot_id_messenger UNIQUE (bot_id, messenger),
    CONSTRAINT ck_bots_messenger CHECK (messenger IN ('TELEGRAM'))
);

CREATE TABLE threads (
    id bigint NOT NULL,
    title varchar(128) NOT NULL,
    description varchar(4096),
    organization_id bigint NOT NULL,
    created_at timestamp with time zone NOT NULL,
    CONSTRAINT pk_threads PRIMARY KEY (id),
    CONSTRAINT fk_threads_organization FOREIGN KEY (organization_id)
        REFERENCES organizations (id) ON DELETE CASCADE,
    CONSTRAINT ck_threads_title CHECK (
        char_length(title) BETWEEN 2 AND 128 AND btrim(title) <> ''
    )
);

CREATE TABLE deadlines (
    id bigint NOT NULL,
    thread_id bigint NOT NULL,
    title varchar(128) NOT NULL,
    description varchar(4096),
    created_at timestamp with time zone NOT NULL,
    due timestamp with time zone NOT NULL,
    is_completed boolean NOT NULL,
    CONSTRAINT pk_deadlines PRIMARY KEY (id),
    CONSTRAINT fk_deadlines_thread FOREIGN KEY (thread_id)
        REFERENCES threads (id) ON DELETE CASCADE,
    CONSTRAINT ck_deadlines_title CHECK (
        char_length(title) BETWEEN 2 AND 128 AND btrim(title) <> ''
    )
);

CREATE TABLE chats (
    id bigint NOT NULL,
    messenger_chat_id bigint NOT NULL,
    messenger varchar(255) NOT NULL,
    title varchar(256) NOT NULL,
    bot_id bigint NOT NULL,
    language varchar(255) NOT NULL,
    registered_at timestamp with time zone NOT NULL,
    CONSTRAINT pk_chats PRIMARY KEY (id),
    CONSTRAINT uq_chats_messenger_chat_id_messenger UNIQUE (messenger_chat_id, messenger),
    CONSTRAINT fk_chats_bot FOREIGN KEY (bot_id)
        REFERENCES bots (id) ON DELETE CASCADE,
    CONSTRAINT ck_chats_messenger CHECK (messenger IN ('TELEGRAM')),
    CONSTRAINT ck_chats_language CHECK (language IN ('RU', 'EN'))
);

CREATE TABLE refresh_tokens (
    id bigint NOT NULL,
    jti varchar(255) NOT NULL,
    expiry timestamp with time zone NOT NULL,
    revoked boolean NOT NULL,
    user_id bigint NOT NULL,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT uq_refresh_tokens_jti UNIQUE (jti),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE organization_invitations (
    id bigint NOT NULL,
    invited_by_user_id bigint NOT NULL,
    invited_user_id bigint NOT NULL,
    organization_id bigint NOT NULL,
    status varchar(255) NOT NULL,
    role varchar(255) NOT NULL,
    created_at timestamp with time zone NOT NULL,
    answered_at timestamp with time zone,
    CONSTRAINT pk_organization_invitations PRIMARY KEY (id),
    CONSTRAINT ck_organization_invitations_distinct_users CHECK (invited_by_user_id <> invited_user_id),
    CONSTRAINT fk_organization_invitations_invited_by FOREIGN KEY (invited_by_user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_organization_invitations_invited_user FOREIGN KEY (invited_user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_organization_invitations_organization FOREIGN KEY (organization_id)
        REFERENCES organizations (id) ON DELETE CASCADE,
    CONSTRAINT ck_organization_invitations_status CHECK (
        status IN ('ACCEPTED', 'DECLINED', 'PENDING', 'REVOKED')
    ),
    CONSTRAINT ck_organization_invitations_role CHECK (role IN ('ORG_MEMBER', 'ORG_ADMIN'))
);

CREATE TABLE user_scopes (
    user_id bigint NOT NULL,
    scope_type varchar(3) NOT NULL,
    scope_id bigint NOT NULL,
    role varchar(255) NOT NULL,
    assigned_at timestamp with time zone NOT NULL,
    CONSTRAINT pk_user_scopes PRIMARY KEY (user_id, scope_type, scope_id),
    CONSTRAINT fk_user_scopes_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_user_scopes_scope_type CHECK (
        scope_type IN ('ORG', 'THR', 'DDL')
    ),
    CONSTRAINT ck_user_scopes_role_compatibility CHECK (
        (scope_type = 'ORG' AND role IN ('ORG_MEMBER', 'ORG_ADMIN', 'ORG_OWNER'))
        OR (scope_type = 'THR' AND role IN ('THR_ASSIGNEE', 'THR_ADMIN', 'THR_OWNER'))
        OR (scope_type = 'DDL' AND role = 'DDL_ASSIGNEE')
    )
);

CREATE TABLE user_messenger_accounts (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    account_id bigint NOT NULL,
    messenger varchar(255) NOT NULL,
    CONSTRAINT pk_user_messenger_accounts PRIMARY KEY (id),
    CONSTRAINT uq_user_messenger_accounts_account_messenger UNIQUE (account_id, messenger),
    CONSTRAINT fk_user_messenger_accounts_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_user_messenger_accounts_messenger CHECK (messenger IN ('TELEGRAM'))
);

CREATE TABLE chat_subscriptions (
    chat_id bigint NOT NULL,
    scope_type varchar(3) NOT NULL,
    scope_id bigint NOT NULL,
    subscribed_at timestamp with time zone NOT NULL,
    CONSTRAINT pk_chat_subscriptions PRIMARY KEY (chat_id, scope_type, scope_id),
    CONSTRAINT fk_chat_subscriptions_chat FOREIGN KEY (chat_id)
        REFERENCES chats (id) ON DELETE CASCADE,
    CONSTRAINT ck_chat_subscriptions_scope_type CHECK (
        scope_type IN ('ORG', 'THR', 'DDL')
    )
);

CREATE TABLE deadline_attachments (
    id bigint NOT NULL,
    object_key varchar(255) NOT NULL,
    filename varchar(64) NOT NULL,
    mime_type varchar(255) NOT NULL,
    size_bytes bigint NOT NULL,
    user_id bigint,
    deadline_id bigint NOT NULL,
    uploaded_at timestamp with time zone NOT NULL,
    CONSTRAINT pk_deadline_attachments PRIMARY KEY (id),
    CONSTRAINT uq_deadline_attachments_object_key UNIQUE (object_key),
    CONSTRAINT fk_deadline_attachments_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_deadline_attachments_deadline FOREIGN KEY (deadline_id)
        REFERENCES deadlines (id) ON DELETE CASCADE,
    CONSTRAINT ck_deadline_attachments_filename CHECK (
        char_length(filename) BETWEEN 1 AND 64 AND btrim(filename) <> ''
    ),
    CONSTRAINT ck_deadline_attachments_size CHECK (size_bytes >= 0)
);

CREATE TABLE deadline_notifications (
    id bigint NOT NULL,
    deadline_id bigint NOT NULL,
    send_at timestamp with time zone NOT NULL,
    type varchar(255) NOT NULL,
    status varchar(1) NOT NULL,
    CONSTRAINT pk_deadline_notifications PRIMARY KEY (id),
    CONSTRAINT fk_deadline_notifications_deadline FOREIGN KEY (deadline_id)
        REFERENCES deadlines (id) ON DELETE CASCADE,
    CONSTRAINT ck_deadline_notifications_type CHECK (
        type IN ('FIFTEEN_MINUTES', 'ONE_HOUR', 'ONE_DAY', 'ONE_WEEK', 'ONE_MONTH')
    ),
    CONSTRAINT ck_deadline_notifications_status CHECK (status IN ('P', 'I', 'S', 'F'))
);

CREATE TABLE notification_outbox (
    id bigint GENERATED BY DEFAULT AS IDENTITY,
    notification_id bigint NOT NULL,
    source varchar(255) NOT NULL,
    messenger varchar(255) NOT NULL,
    priority integer NOT NULL,
    topic varchar(255) NOT NULL,
    payload jsonb NOT NULL,
    available_at timestamp with time zone NOT NULL,
    status varchar(1) NOT NULL,
    retries integer NOT NULL,
    CONSTRAINT pk_notification_outbox PRIMARY KEY (id),
    CONSTRAINT fk_notification_outbox_notification FOREIGN KEY (notification_id)
        REFERENCES deadline_notifications (id) ON DELETE CASCADE,
    CONSTRAINT ck_notification_outbox_source CHECK (source IN ('DDL_EXPIRATION')),
    CONSTRAINT ck_notification_outbox_messenger CHECK (messenger IN ('TELEGRAM')),
    CONSTRAINT ck_notification_outbox_status CHECK (status IN ('P', 'I', 'S', 'F')),
    CONSTRAINT ck_notification_outbox_retries CHECK (retries >= 0)
);

-- Enforces case-insensitive usernames and serves exact login plus lower(username) prefix hints.
CREATE UNIQUE INDEX uq_users_username_lower
    ON users (lower(username) text_pattern_ops);

-- Serves active-session lookup and revocation for one user without scanning expired/revoked tokens.
CREATE INDEX ix_refresh_tokens_valid_user
    ON refresh_tokens (user_id, expiry)
    WHERE revoked = false;

-- Serializes concurrent invitation creation so a user has at most one pending invite per organization.
CREATE UNIQUE INDEX uq_organization_invitations_pending
    ON organization_invitations (invited_user_id, organization_id)
    WHERE status = 'PENDING';
-- Serves the recipient's pending-invitation page and pending count.
CREATE INDEX ix_organization_invitations_pending_recipient
    ON organization_invitations (invited_user_id, created_at DESC, id DESC)
    WHERE status = 'PENDING';
-- Serves the sender's pending-invitation page.
CREATE INDEX ix_organization_invitations_pending_sender
    ON organization_invitations (invited_by_user_id, created_at DESC, id DESC)
    WHERE status = 'PENDING';
-- Locates invitation rows for the organization FK's ON DELETE CASCADE action.
CREATE INDEX ix_organization_invitations_organization
    ON organization_invitations (organization_id);

-- Enforces at most one organization owner during concurrent ownership transfers.
CREATE UNIQUE INDEX uq_user_scopes_organization_owner
    ON user_scopes (scope_id)
    WHERE scope_type = 'ORG' AND role = 'ORG_OWNER';
-- Serves scope-first member/assignee lists and counts; the PK serves user-first permission hydration.
CREATE INDEX ix_user_scopes_scope
    ON user_scopes (scope_type, scope_id, user_id)
    INCLUDE (role, assigned_at);

-- Serves organization thread pages, thread-ID hydration, statistics, and organization deletion.
CREATE INDEX ix_threads_organization
    ON threads (organization_id, id);
-- Serves thread deadline pages/IDs and total/completed deadline statistics.
CREATE INDEX ix_deadlines_thread_completion
    ON deadlines (thread_id, is_completed)
    INCLUDE (id);

-- Serves profile account listing, per-messenger limits, locking, unlinking, and user deletion.
CREATE INDEX ix_user_messenger_accounts_user_messenger
    ON user_messenger_accounts (user_id, messenger, id);

-- Serves deadline/thread/organization subscription fan-out while publishing notifications.
CREATE INDEX ix_chat_subscriptions_scope
    ON chat_subscriptions (scope_type, scope_id, chat_id);

-- Serves attachment count/list operations for a deadline in newest-first order.
CREATE INDEX ix_deadline_attachments_deadline_uploaded
    ON deadline_attachments (deadline_id, uploaded_at DESC, id DESC);
-- Locates uploaded attachments for the user FK's ON DELETE SET NULL action.
CREATE INDEX ix_deadline_attachments_user
    ON deadline_attachments (user_id);

-- Serves pending-notification reconciliation and locking when a deadline due date changes.
CREATE INDEX ix_deadline_notifications_deadline_status
    ON deadline_notifications (deadline_id, status);
-- Serves ordered SKIP LOCKED claims of due pending deadline notifications.
CREATE INDEX ix_deadline_notifications_due
    ON deadline_notifications (send_at, id)
    WHERE status = 'P';
-- Prevents concurrent planners from creating duplicate pending reminder types for a deadline.
CREATE UNIQUE INDEX uq_deadline_notifications_pending_type
    ON deadline_notifications (deadline_id, type)
    WHERE status = 'P';

-- Serves priority-ordered claims of available pending or abandoned in-progress outbox work.
CREATE INDEX ix_notification_outbox_claim
    ON notification_outbox (priority DESC, retries, available_at, id)
    WHERE status IN ('P', 'I');
-- Serves notification finalization checks for unfinished or failed outbox rows.
CREATE INDEX ix_notification_outbox_notification_status
    ON notification_outbox (notification_id, status);

CREATE FUNCTION validate_polymorphic_scope_target()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    CASE NEW.scope_type
        WHEN 'ORG' THEN
            PERFORM 1 FROM organizations WHERE id = NEW.scope_id FOR KEY SHARE;
        WHEN 'THR' THEN
            PERFORM 1 FROM threads WHERE id = NEW.scope_id FOR KEY SHARE;
        WHEN 'DDL' THEN
            PERFORM 1 FROM deadlines WHERE id = NEW.scope_id FOR KEY SHARE;
        ELSE
            RAISE EXCEPTION USING
                ERRCODE = '23514',
                MESSAGE = format('invalid scope type %L', NEW.scope_type);
    END CASE;

    IF NOT FOUND THEN
        RAISE EXCEPTION USING
            ERRCODE = '23503',
            MESSAGE = format(
                'insert or update on table %I violates polymorphic scope foreign key: %s %s does not exist',
                TG_TABLE_NAME,
                NEW.scope_type,
                NEW.scope_id
            );
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_user_scopes_validate_target
BEFORE INSERT OR UPDATE OF scope_type, scope_id ON user_scopes
FOR EACH ROW EXECUTE FUNCTION validate_polymorphic_scope_target();

CREATE TRIGGER trg_chat_subscriptions_validate_target
BEFORE INSERT OR UPDATE OF scope_type, scope_id ON chat_subscriptions
FOR EACH ROW EXECUTE FUNCTION validate_polymorphic_scope_target();

CREATE FUNCTION cleanup_polymorphic_scope_targets()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    DELETE FROM user_scopes
    WHERE scope_type = TG_ARGV[0] AND scope_id = OLD.id;

    DELETE FROM chat_subscriptions
    WHERE scope_type = TG_ARGV[0] AND scope_id = OLD.id;

    RETURN NULL;
END;
$$;

CREATE TRIGGER trg_organizations_cleanup_scope_targets
AFTER DELETE ON organizations
FOR EACH ROW EXECUTE FUNCTION cleanup_polymorphic_scope_targets('ORG');

CREATE TRIGGER trg_threads_cleanup_scope_targets
AFTER DELETE ON threads
FOR EACH ROW EXECUTE FUNCTION cleanup_polymorphic_scope_targets('THR');

CREATE TRIGGER trg_deadlines_cleanup_scope_targets
AFTER DELETE ON deadlines
FOR EACH ROW EXECUTE FUNCTION cleanup_polymorphic_scope_targets('DDL');
