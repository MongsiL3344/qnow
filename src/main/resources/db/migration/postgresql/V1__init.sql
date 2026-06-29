CREATE TABLE event_publication
(
  id                     UUID          NOT NULL,
  listener_id            VARCHAR(512)  NOT NULL,
  event_type             VARCHAR(512)  NOT NULL,
  serialized_event       VARCHAR(4000) NOT NULL,
  publication_date       TIMESTAMPTZ   NOT NULL,
  completion_date        TIMESTAMPTZ   NULL,
  status                 VARCHAR(20)   NULL,
  completion_attempts    INT           NULL,
  last_resubmission_date TIMESTAMPTZ   NULL,
  PRIMARY KEY (id)
);

COMMENT ON TABLE event_publication IS 'Spring Modulith event publication log';

CREATE INDEX event_publication_by_listener_id_and_serialized_event_idx
  ON event_publication (listener_id, serialized_event);

CREATE INDEX event_publication_by_completion_date_idx
  ON event_publication (completion_date);

CREATE TABLE users
(
  id         UUID         NOT NULL,
  email      VARCHAR(255) NOT NULL,
  nickname   VARCHAR(30)  NOT NULL,
  password   VARCHAR(255) NOT NULL,
  created_at TIMESTAMPTZ  NOT NULL,
  deleted_at TIMESTAMPTZ  NULL,
  PRIMARY KEY (id)
);

COMMENT ON TABLE users IS '유저 테이블';
COMMENT ON COLUMN users.id IS 'PK';
COMMENT ON COLUMN users.email IS '이메일';
COMMENT ON COLUMN users.nickname IS '닉네임';
COMMENT ON COLUMN users.password IS '해시 된 비밀번호';
COMMENT ON COLUMN users.created_at IS '계정 생성 시각';
COMMENT ON COLUMN users.deleted_at IS '계정 삭제 시각';

CREATE TABLE organizations
(
  id         UUID         NOT NULL,
  name       VARCHAR(30)  NOT NULL,
  detail     VARCHAR(255) NULL,
  password   VARCHAR(255) NULL,
  created_at TIMESTAMPTZ  NOT NULL,
  deleted_at TIMESTAMPTZ  NULL,
  PRIMARY KEY (id)
);

COMMENT ON TABLE organizations IS '조직 테이블';
COMMENT ON COLUMN organizations.id IS 'PK';
COMMENT ON COLUMN organizations.name IS '조직 명';
COMMENT ON COLUMN organizations.detail IS '조직 상세설명';
COMMENT ON COLUMN organizations.password IS '조직 입장 비밀번호';
COMMENT ON COLUMN organizations.created_at IS '조직 생성 시각';
COMMENT ON COLUMN organizations.deleted_at IS '조직 삭제 시각';

CREATE TABLE sessions
(
  id              UUID         NOT NULL,
  organization_id UUID         NOT NULL,
  creator_id      UUID         NOT NULL,
  title           VARCHAR(255) NOT NULL,
  start_at        TIMESTAMPTZ  NULL,
  end_at          TIMESTAMPTZ  NULL,
  created_at      TIMESTAMPTZ  NOT NULL,
  deleted_at      TIMESTAMPTZ  NULL,
  PRIMARY KEY (id)
);

COMMENT ON TABLE sessions IS '조직에 속한 발표 세션 테이블';
COMMENT ON COLUMN sessions.id IS 'PK';
COMMENT ON COLUMN sessions.organization_id IS '조직 PK';
COMMENT ON COLUMN sessions.creator_id IS '세션 개설자 PK';
COMMENT ON COLUMN sessions.title IS '세션 제목';
COMMENT ON COLUMN sessions.start_at IS '세션 시작 시간';
COMMENT ON COLUMN sessions.end_at IS '세션 종료 시간';
COMMENT ON COLUMN sessions.created_at IS '세션 생성 시각';
COMMENT ON COLUMN sessions.deleted_at IS '세션 삭제 시각';

CREATE TABLE user_groups
(
  id              UUID        NOT NULL,
  user_id         UUID        NOT NULL,
  organization_id UUID        NOT NULL,
  role            VARCHAR(20) NOT NULL DEFAULT 'USER',
  created_at      TIMESTAMPTZ NOT NULL,
  deleted_at      TIMESTAMPTZ NULL,
  PRIMARY KEY (id)
);

COMMENT ON TABLE user_groups IS '유저가 참여하고있는 조직 목록 테이블';
COMMENT ON COLUMN user_groups.id IS 'PK';
COMMENT ON COLUMN user_groups.user_id IS '유저 테이블 PK';
COMMENT ON COLUMN user_groups.organization_id IS '조직 테이블 PK';
COMMENT ON COLUMN user_groups.role IS '조직 내 유저 역할 (관리자/유저)';
COMMENT ON COLUMN user_groups.created_at IS '조직 참여 생성 시각';
COMMENT ON COLUMN user_groups.deleted_at IS '조직 참여 삭제 시각';

CREATE TABLE participants
(
  id         UUID        NOT NULL,
  user_id    UUID        NOT NULL,
  session_id UUID        NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  deleted_at TIMESTAMPTZ NULL,
  PRIMARY KEY (id)
);

COMMENT ON TABLE participants IS '세션에 속한 세션참여자';
COMMENT ON COLUMN participants.id IS 'PK';
COMMENT ON COLUMN participants.user_id IS '유저 PK';
COMMENT ON COLUMN participants.session_id IS '세션 PK';
COMMENT ON COLUMN participants.created_at IS '세션 참여 생성 시각';
COMMENT ON COLUMN participants.deleted_at IS '세션 참여 삭제 시각';

CREATE TABLE presentations
(
  id            UUID         NOT NULL,
  session_id    UUID         NOT NULL,
  presenter_id  UUID         NOT NULL,
  title         VARCHAR(255) NOT NULL,
  content_type  VARCHAR(100) NOT NULL DEFAULT 'application/pdf',
  s3_key        VARCHAR(1024) NOT NULL,
  upload_status VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
  created_at    TIMESTAMPTZ  NOT NULL,
  deleted_at    TIMESTAMPTZ  NULL,
  PRIMARY KEY (id)
);

COMMENT ON TABLE presentations IS '세션에 속한 발표 테이블';
COMMENT ON COLUMN presentations.id IS 'PK';
COMMENT ON COLUMN presentations.session_id IS '세션 PK';
COMMENT ON COLUMN presentations.presenter_id IS '발표자 유저 PK';
COMMENT ON COLUMN presentations.title IS '발표 제목';
COMMENT ON COLUMN presentations.content_type IS '업로드 파일 Content-Type';
COMMENT ON COLUMN presentations.s3_key IS 'S3 object key';
COMMENT ON COLUMN presentations.upload_status IS '업로드 상태 (PENDING | UPLOADED | FAILED)';
COMMENT ON COLUMN presentations.created_at IS '발표 생성 시각';
COMMENT ON COLUMN presentations.deleted_at IS '발표 삭제 시각';

-- 소프트딜리트 제약조건
CREATE UNIQUE INDEX uq_users_active_email
  ON users (email)
  WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uq_organizations_active_name
  ON organizations (name)
  WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uq_user_groups_active_user_organization
  ON user_groups (user_id, organization_id)
  WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uq_participants_active_session_user
  ON participants (session_id, user_id)
  WHERE deleted_at IS NULL;

ALTER TABLE participants
  ADD CONSTRAINT uq_participants_session_id
    UNIQUE (session_id, id);

ALTER TABLE presentations
  ADD CONSTRAINT uq_presentations_s3_key
    UNIQUE (s3_key);

CREATE INDEX idx_sessions_active_organization_created_at
  ON sessions (organization_id, created_at DESC)
  WHERE deleted_at IS NULL;

-- 외래키 연결
ALTER TABLE user_groups
  ADD CONSTRAINT FK_users_TO_user_groups
    FOREIGN KEY (user_id)
    REFERENCES users (id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION;

ALTER TABLE user_groups
  ADD CONSTRAINT FK_organizations_TO_user_groups
    FOREIGN KEY (organization_id)
    REFERENCES organizations (id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION;

ALTER TABLE sessions
  ADD CONSTRAINT FK_organizations_TO_sessions
    FOREIGN KEY (organization_id)
    REFERENCES organizations (id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION;

ALTER TABLE sessions
  ADD CONSTRAINT FK_users_TO_sessions
    FOREIGN KEY (creator_id)
    REFERENCES users (id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION;

ALTER TABLE presentations
  ADD CONSTRAINT FK_sessions_TO_presentations
    FOREIGN KEY (session_id)
    REFERENCES sessions (id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION;

ALTER TABLE participants
  ADD CONSTRAINT FK_users_TO_participants
    FOREIGN KEY (user_id)
    REFERENCES users (id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION;

ALTER TABLE participants
  ADD CONSTRAINT FK_sessions_TO_participants
    FOREIGN KEY (session_id)
    REFERENCES sessions (id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION;

ALTER TABLE presentations
  ADD CONSTRAINT FK_users_TO_presentations_presenter
    FOREIGN KEY (presenter_id)
    REFERENCES users (id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION;
