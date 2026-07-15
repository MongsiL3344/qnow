CREATE TABLE session_participate_codes
(
  id         UUID         NOT NULL,
  session_id UUID         NOT NULL,
  code       VARCHAR(64)  NOT NULL,
  created_at TIMESTAMPTZ  NOT NULL,
  deleted_at TIMESTAMPTZ  NULL,
  PRIMARY KEY (id)
);

COMMENT ON TABLE session_participate_codes IS '비회원 세션 참가 코드 테이블';
COMMENT ON COLUMN session_participate_codes.id IS 'PK';
COMMENT ON COLUMN session_participate_codes.session_id IS '세션 PK';
COMMENT ON COLUMN session_participate_codes.code IS '비회원 세션 참가 코드';
COMMENT ON COLUMN session_participate_codes.created_at IS '참가 코드 생성 시각';
COMMENT ON COLUMN session_participate_codes.deleted_at IS '참가 코드 삭제 시각';

ALTER TABLE session_participate_codes
  ADD CONSTRAINT uq_session_participate_codes_code
    UNIQUE (code);

CREATE UNIQUE INDEX uq_session_participate_codes_active_session
  ON session_participate_codes (session_id)
  WHERE deleted_at IS NULL;

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

COMMENT ON COLUMN participants.guest_nickname IS '비회원 세션 참여자 닉네임';

ALTER TABLE participants
  ADD CONSTRAINT chk_participants_member_or_guest
    CHECK (
      (user_id IS NOT NULL AND guest_nickname IS NULL)
      OR
      (user_id IS NULL AND guest_nickname IS NOT NULL)
    );
