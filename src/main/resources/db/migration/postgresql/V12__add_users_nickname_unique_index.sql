CREATE UNIQUE INDEX uq_users_active_nickname
    ON users (nickname)
    WHERE deleted_at IS NULL;
