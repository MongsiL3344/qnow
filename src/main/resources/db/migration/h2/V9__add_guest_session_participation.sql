CREATE TABLE session_participate_codes
(
  id         UUID                     NOT NULL,
  session_id UUID                     NOT NULL,
  code       VARCHAR(64)              NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  deleted_at TIMESTAMP WITH TIME ZONE NULL,
  PRIMARY KEY (id)
);

ALTER TABLE session_participate_codes
  ADD CONSTRAINT uq_session_participate_codes_code
    UNIQUE (code);

ALTER TABLE session_participate_codes
  ADD CONSTRAINT uq_session_participate_codes_active_session
    UNIQUE NULLS NOT DISTINCT (session_id, deleted_at);

ALTER TABLE session_participate_codes
  ADD CONSTRAINT FK_sessions_TO_session_participate_codes
    FOREIGN KEY (session_id)
    REFERENCES sessions (id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION;

ALTER TABLE participants
  ALTER COLUMN user_id DROP NOT NULL;

ALTER TABLE participants
  ADD COLUMN guest_nickname VARCHAR(30) NULL;

ALTER TABLE participants
  ADD CONSTRAINT chk_participants_member_or_guest
    CHECK (
      (user_id IS NOT NULL AND guest_nickname IS NULL)
      OR
      (user_id IS NULL AND guest_nickname IS NOT NULL)
    );
