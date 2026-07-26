-- ---------------------------------------------------------------------------------------------------------------------
-- Spring Modulith 이벤트 발행 테이블 생성
-- ---------------------------------------------------------------------------------------------------------------------
CREATE TABLE event_publication
(
  id                     UUID          NOT NULL,
  listener_id            VARCHAR(512)  NOT NULL,
  event_type              VARCHAR(512)  NOT NULL,
  serialized_event       VARCHAR(4000) NOT NULL,
  publication_date       TIMESTAMPTZ   NOT NULL,
  completion_date        TIMESTAMPTZ   NULL,
  status                  VARCHAR(20)   NULL,
  completion_attempts    INT           NULL,
  last_resubmission_date TIMESTAMPTZ   NULL,
  PRIMARY KEY (id)
);

-- ---------------------------------------------------------------------------------------------------------------------
-- 유저 테이블 생성
-- ---------------------------------------------------------------------------------------------------------------------
CREATE TABLE users
(
  id                UUID         NOT NULL,
  email             VARCHAR(255) NOT NULL,
  nickname          VARCHAR(30)  NOT NULL,
  password          VARCHAR(255) NOT NULL,
  email_verified_at TIMESTAMPTZ  NULL,
  created_at        TIMESTAMPTZ  NOT NULL,
  deleted_at        TIMESTAMPTZ  NULL,
  PRIMARY KEY (id)
);

-- ---------------------------------------------------------------------------------------------------------------------
-- 조직 테이블 생성
-- ---------------------------------------------------------------------------------------------------------------------
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

-- ---------------------------------------------------------------------------------------------------------------------
-- 발표 세션 테이블 생성
-- ---------------------------------------------------------------------------------------------------------------------
CREATE TABLE sessions
(
  id                   UUID         NOT NULL,
  organization_id      UUID         NOT NULL,
  creator_id           UUID         NOT NULL,
  title                VARCHAR(255) NOT NULL,
  start_at             TIMESTAMPTZ  NULL,
  end_at               TIMESTAMPTZ  NULL,
  guest_upvote_allowed BOOLEAN      NOT NULL DEFAULT FALSE,
  created_at           TIMESTAMPTZ  NOT NULL,
  deleted_at           TIMESTAMPTZ  NULL,
  PRIMARY KEY (id)
);

-- ---------------------------------------------------------------------------------------------------------------------
-- 유저-조직 참여 테이블 생성
-- ---------------------------------------------------------------------------------------------------------------------
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

-- ---------------------------------------------------------------------------------------------------------------------
-- 세션 참여자 테이블 생성
-- ---------------------------------------------------------------------------------------------------------------------
CREATE TABLE participants
(
  id             UUID        NOT NULL,
  user_id        UUID        NULL,
  guest_nickname VARCHAR(30) NULL,
  session_id     UUID        NOT NULL,
  created_at     TIMESTAMPTZ NOT NULL,
  deleted_at     TIMESTAMPTZ NULL,
  PRIMARY KEY (id)
);

-- ---------------------------------------------------------------------------------------------------------------------
-- 발표 자료 테이블 생성
-- ---------------------------------------------------------------------------------------------------------------------
CREATE TABLE presentations
(
  id               UUID          NOT NULL,
  session_id       UUID          NOT NULL,
  presenter_id     UUID          NOT NULL,
  title            VARCHAR(255)  NOT NULL,
  content_type     VARCHAR(100)  NOT NULL DEFAULT 'application/pdf',
  s3_key           VARCHAR(1024) NOT NULL,
  thumbnail_s3_key VARCHAR(1024) NULL,
  page_count       INT           NOT NULL DEFAULT 1,
  upload_status    VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
  created_at       TIMESTAMPTZ   NOT NULL,
  deleted_at       TIMESTAMPTZ   NULL,
  PRIMARY KEY (id)
);

-- ---------------------------------------------------------------------------------------------------------------------
-- 질문 테이블 생성
-- ---------------------------------------------------------------------------------------------------------------------
CREATE TABLE questions
(
  id                     UUID          NOT NULL,
  presentation_id        UUID          NOT NULL,
  questioner_id          UUID          NOT NULL,
  content                TEXT          NOT NULL,
  is_anonymous           BOOLEAN       NOT NULL DEFAULT FALSE,
  page_start             INT           NOT NULL,
  page_end               INT           NOT NULL,
  upvote_count           INT           NOT NULL DEFAULT 0,
  selection_left_ratio   NUMERIC(6, 5) NULL,
  selection_top_ratio    NUMERIC(6, 5) NULL,
  selection_width_ratio  NUMERIC(6, 5) NULL,
  selection_height_ratio NUMERIC(6, 5) NULL,
  created_at             TIMESTAMPTZ   NOT NULL,
  deleted_at             TIMESTAMPTZ   NULL,
  PRIMARY KEY (id)
);

-- ---------------------------------------------------------------------------------------------------------------------
-- 질문 공감 테이블 생성
-- ---------------------------------------------------------------------------------------------------------------------
CREATE TABLE question_upvotes
(
  id                         UUID        NOT NULL,
  question_id                UUID        NOT NULL,
  voter_user_id              UUID        NULL,
  voter_guest_participant_id UUID        NULL,
  created_at                 TIMESTAMPTZ NOT NULL,
  PRIMARY KEY (id)
);

-- ---------------------------------------------------------------------------------------------------------------------
-- 비회원 세션 참가 코드 테이블 생성
-- ---------------------------------------------------------------------------------------------------------------------
CREATE TABLE session_participate_codes
(
  id         UUID         NOT NULL,
  session_id UUID         NOT NULL,
  code       VARCHAR(64)  NOT NULL,
  created_at TIMESTAMPTZ  NOT NULL,
  deleted_at TIMESTAMPTZ  NULL,
  PRIMARY KEY (id)
);

-- ---------------------------------------------------------------------------------------------------------------------
-- 컬럼 주석
-- ---------------------------------------------------------------------------------------------------------------------
COMMENT ON TABLE event_publication IS 'Spring Modulith event publication log';

COMMENT ON TABLE users IS '유저 테이블';
COMMENT ON COLUMN users.id IS 'PK';
COMMENT ON COLUMN users.email IS '이메일';
COMMENT ON COLUMN users.nickname IS '닉네임';
COMMENT ON COLUMN users.password IS '해시 된 비밀번호';
COMMENT ON COLUMN users.email_verified_at IS '이메일 인증 완료 시각';
COMMENT ON COLUMN users.created_at IS '계정 생성 시각';
COMMENT ON COLUMN users.deleted_at IS '계정 삭제 시각';

COMMENT ON TABLE organizations IS '조직 테이블';
COMMENT ON COLUMN organizations.id IS 'PK';
COMMENT ON COLUMN organizations.name IS '조직 명';
COMMENT ON COLUMN organizations.detail IS '조직 상세설명';
COMMENT ON COLUMN organizations.password IS '조직 입장 비밀번호';
COMMENT ON COLUMN organizations.created_at IS '조직 생성 시각';
COMMENT ON COLUMN organizations.deleted_at IS '조직 삭제 시각';

COMMENT ON TABLE sessions IS '조직에 속한 발표 세션 테이블';
COMMENT ON COLUMN sessions.id IS 'PK';
COMMENT ON COLUMN sessions.organization_id IS '조직 PK';
COMMENT ON COLUMN sessions.creator_id IS '세션 개설자 PK';
COMMENT ON COLUMN sessions.title IS '세션 제목';
COMMENT ON COLUMN sessions.start_at IS '세션 시작 시간';
COMMENT ON COLUMN sessions.end_at IS '세션 종료 시간';
COMMENT ON COLUMN sessions.guest_upvote_allowed IS '비회원 질문 공감 허용 여부';
COMMENT ON COLUMN sessions.created_at IS '세션 생성 시각';
COMMENT ON COLUMN sessions.deleted_at IS '세션 삭제 시각';

COMMENT ON TABLE user_groups IS '유저가 참여하고있는 조직 목록 테이블';
COMMENT ON COLUMN user_groups.id IS 'PK';
COMMENT ON COLUMN user_groups.user_id IS '유저 테이블 PK';
COMMENT ON COLUMN user_groups.organization_id IS '조직 테이블 PK';
COMMENT ON COLUMN user_groups.role IS '조직 내 유저 역할 (관리자/유저)';
COMMENT ON COLUMN user_groups.created_at IS '조직 참여 생성 시각';
COMMENT ON COLUMN user_groups.deleted_at IS '조직 참여 삭제 시각';

COMMENT ON TABLE participants IS '세션에 속한 세션참여자';
COMMENT ON COLUMN participants.id IS 'PK';
COMMENT ON COLUMN participants.user_id IS '유저 PK';
COMMENT ON COLUMN participants.guest_nickname IS '비회원 세션 참여자 닉네임';
COMMENT ON COLUMN participants.session_id IS '세션 PK';
COMMENT ON COLUMN participants.created_at IS '세션 참여 생성 시각';
COMMENT ON COLUMN participants.deleted_at IS '세션 참여 삭제 시각';

COMMENT ON TABLE presentations IS '세션에 속한 발표 테이블';
COMMENT ON COLUMN presentations.id IS 'PK';
COMMENT ON COLUMN presentations.session_id IS '세션 PK';
COMMENT ON COLUMN presentations.presenter_id IS '발표자 유저 PK';
COMMENT ON COLUMN presentations.title IS '발표 제목';
COMMENT ON COLUMN presentations.content_type IS '업로드 파일 Content-Type';
COMMENT ON COLUMN presentations.s3_key IS 'S3 object key';
COMMENT ON COLUMN presentations.thumbnail_s3_key IS 'S3 thumbnail object key';
COMMENT ON COLUMN presentations.page_count IS 'PDF page count';
COMMENT ON COLUMN presentations.upload_status IS '업로드 상태 (PENDING | UPLOADED | FAILED)';
COMMENT ON COLUMN presentations.created_at IS '발표 생성 시각';
COMMENT ON COLUMN presentations.deleted_at IS '발표 삭제 시각';

COMMENT ON TABLE questions IS '발표 자료에 연결된 질문 테이블';
COMMENT ON COLUMN questions.id IS 'PK';
COMMENT ON COLUMN questions.presentation_id IS '발표 PK';
COMMENT ON COLUMN questions.questioner_id IS '질문자 세션 참여자 PK';
COMMENT ON COLUMN questions.content IS '질문 내용';
COMMENT ON COLUMN questions.is_anonymous IS '질문 익명 여부';
COMMENT ON COLUMN questions.page_start IS '질문이 연결된 시작 페이지';
COMMENT ON COLUMN questions.page_end IS '질문이 연결된 끝 페이지';
COMMENT ON COLUMN questions.upvote_count IS '질문 좋아요 수';
COMMENT ON COLUMN questions.selection_left_ratio IS '선택 영역의 왼쪽 위치 비율';
COMMENT ON COLUMN questions.selection_top_ratio IS '선택 영역의 위쪽 위치 비율';
COMMENT ON COLUMN questions.selection_width_ratio IS '선택 영역의 너비 비율';
COMMENT ON COLUMN questions.selection_height_ratio IS '선택 영역의 높이 비율';
COMMENT ON COLUMN questions.created_at IS '질문 생성 시각';
COMMENT ON COLUMN questions.deleted_at IS '질문 삭제 시각';

COMMENT ON TABLE question_upvotes IS '질문 좋아요 테이블';
COMMENT ON COLUMN question_upvotes.id IS 'PK';
COMMENT ON COLUMN question_upvotes.question_id IS '질문 PK';
COMMENT ON COLUMN question_upvotes.voter_user_id IS '좋아요를 누른 유저 PK';
COMMENT ON COLUMN question_upvotes.voter_guest_participant_id IS '공감을 누른 비회원 세션 참여자 PK';
COMMENT ON COLUMN question_upvotes.created_at IS '좋아요 생성 시각';

COMMENT ON TABLE session_participate_codes IS '비회원 세션 참가 코드 테이블';
COMMENT ON COLUMN session_participate_codes.id IS 'PK';
COMMENT ON COLUMN session_participate_codes.session_id IS '세션 PK';
COMMENT ON COLUMN session_participate_codes.code IS '비회원 세션 참가 코드';
COMMENT ON COLUMN session_participate_codes.created_at IS '참가 코드 생성 시각';
COMMENT ON COLUMN session_participate_codes.deleted_at IS '참가 코드 삭제 시각';

-- ---------------------------------------------------------------------------------------------------------------------
-- event_publication 테이블 인덱스
-- ---------------------------------------------------------------------------------------------------------------------
CREATE INDEX event_publication_by_listener_id_and_serialized_event_idx
  ON event_publication (listener_id, serialized_event);

CREATE INDEX event_publication_by_completion_date_idx
  ON event_publication (completion_date);

-- ---------------------------------------------------------------------------------------------------------------------
-- users 테이블 인덱스
-- ---------------------------------------------------------------------------------------------------------------------
CREATE UNIQUE INDEX uq_users_active_email
  ON users (email)
  WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uq_users_active_nickname
  ON users (nickname)
  WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------------------------------------------------
-- organizations 테이블 인덱스
-- ---------------------------------------------------------------------------------------------------------------------
CREATE UNIQUE INDEX uq_organizations_active_name
  ON organizations (name)
  WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------------------------------------------------
-- user_groups 테이블 인덱스
-- ---------------------------------------------------------------------------------------------------------------------
CREATE UNIQUE INDEX uq_user_groups_active_user_organization
  ON user_groups (user_id, organization_id)
  WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------------------------------------------------
-- participants 테이블 인덱스
-- ---------------------------------------------------------------------------------------------------------------------
CREATE UNIQUE INDEX uq_participants_active_session_user
  ON participants (session_id, user_id)
  WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------------------------------------------------
-- sessions 테이블 인덱스
-- ---------------------------------------------------------------------------------------------------------------------
CREATE INDEX idx_sessions_active_organization_created_at
  ON sessions (organization_id, created_at DESC)
  WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------------------------------------------------
-- questions 테이블 인덱스
-- ---------------------------------------------------------------------------------------------------------------------
CREATE INDEX idx_questions_active_presentation_created_at
  ON questions (presentation_id, created_at DESC, id DESC)
  WHERE deleted_at IS NULL;

CREATE INDEX idx_questions_active_presentation_page
  ON questions (presentation_id, page_start ASC, page_end ASC, created_at ASC, id ASC)
  WHERE deleted_at IS NULL;

CREATE INDEX idx_questions_active_presentation_upvote_count
  ON questions (presentation_id, upvote_count DESC, created_at ASC, id ASC)
  WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------------------------------------------------
-- question_upvotes 테이블 인덱스
-- ---------------------------------------------------------------------------------------------------------------------
CREATE UNIQUE INDEX uq_question_upvotes_question_guest_voter
  ON question_upvotes (question_id, voter_guest_participant_id)
  WHERE voter_guest_participant_id IS NOT NULL;

CREATE INDEX idx_question_upvotes_voter_question
  ON question_upvotes (voter_user_id, question_id);

CREATE INDEX idx_question_upvotes_guest_voter_question
  ON question_upvotes (voter_guest_participant_id, question_id)
  WHERE voter_guest_participant_id IS NOT NULL;

-- ---------------------------------------------------------------------------------------------------------------------
-- session_participate_codes 테이블 인덱스
-- ---------------------------------------------------------------------------------------------------------------------
CREATE UNIQUE INDEX uq_session_participate_codes_active_session
  ON session_participate_codes (session_id)
  WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------------------------------------------------
-- participants 테이블 제약조건
-- ---------------------------------------------------------------------------------------------------------------------
ALTER TABLE participants
  ADD CONSTRAINT uq_participants_session_id
    UNIQUE (session_id, id);

-- ---------------------------------------------------------------------------------------------------------------------
-- presentations 테이블 제약조건
-- ---------------------------------------------------------------------------------------------------------------------
ALTER TABLE presentations
  ADD CONSTRAINT uq_presentations_s3_key
    UNIQUE (s3_key);

ALTER TABLE presentations
  ADD CONSTRAINT uq_presentations_thumbnail_s3_key
    UNIQUE (thumbnail_s3_key);

ALTER TABLE presentations
  ADD CONSTRAINT chk_presentations_page_count_positive
    CHECK (page_count > 0);

-- ---------------------------------------------------------------------------------------------------------------------
-- questions 테이블 제약조건
-- ---------------------------------------------------------------------------------------------------------------------
ALTER TABLE questions
  ADD CONSTRAINT chk_questions_page_range
    CHECK (
      page_start >= 1
      AND page_end >= page_start
    );

ALTER TABLE questions
  ADD CONSTRAINT chk_questions_upvote_count_non_negative
    CHECK (upvote_count >= 0);

ALTER TABLE questions
  ADD CONSTRAINT chk_questions_selection_single_page_only
    CHECK (
      (
        selection_left_ratio IS NULL
        AND selection_top_ratio IS NULL
        AND selection_width_ratio IS NULL
        AND selection_height_ratio IS NULL
      )
      OR
      (
        page_start = page_end
        AND selection_left_ratio IS NOT NULL
        AND selection_top_ratio IS NOT NULL
        AND selection_width_ratio IS NOT NULL
        AND selection_height_ratio IS NOT NULL
        AND selection_left_ratio >= 0
        AND selection_top_ratio >= 0
        AND selection_width_ratio > 0
        AND selection_height_ratio > 0
        AND selection_left_ratio + selection_width_ratio <= 1
        AND selection_top_ratio + selection_height_ratio <= 1
      )
    );

-- ---------------------------------------------------------------------------------------------------------------------
-- participants 테이블 회원/비회원 구분 제약조건
-- ---------------------------------------------------------------------------------------------------------------------
ALTER TABLE participants
  ADD CONSTRAINT chk_participants_member_or_guest
    CHECK (
      (user_id IS NOT NULL AND guest_nickname IS NULL)
      OR
      (user_id IS NULL AND guest_nickname IS NOT NULL)
    );

-- ---------------------------------------------------------------------------------------------------------------------
-- question_upvotes 테이블 제약조건
-- ---------------------------------------------------------------------------------------------------------------------
ALTER TABLE question_upvotes
  ADD CONSTRAINT chk_question_upvotes_member_or_guest
    CHECK (
      (voter_user_id IS NOT NULL AND voter_guest_participant_id IS NULL)
      OR
      (voter_user_id IS NULL AND voter_guest_participant_id IS NOT NULL)
    );

ALTER TABLE question_upvotes
  ADD CONSTRAINT uq_question_upvotes_question_voter
    UNIQUE (question_id, voter_user_id);

-- ---------------------------------------------------------------------------------------------------------------------
-- session_participate_codes 테이블 제약조건
-- ---------------------------------------------------------------------------------------------------------------------
ALTER TABLE session_participate_codes
  ADD CONSTRAINT uq_session_participate_codes_code
    UNIQUE (code);

-- ---------------------------------------------------------------------------------------------------------------------
-- user_groups 테이블 외래 키
-- ---------------------------------------------------------------------------------------------------------------------
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

-- ---------------------------------------------------------------------------------------------------------------------
-- sessions 테이블 외래 키
-- ---------------------------------------------------------------------------------------------------------------------
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

-- ---------------------------------------------------------------------------------------------------------------------
-- participants 테이블 외래 키
-- ---------------------------------------------------------------------------------------------------------------------
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

-- ---------------------------------------------------------------------------------------------------------------------
-- presentations 테이블 외래 키
-- ---------------------------------------------------------------------------------------------------------------------
ALTER TABLE presentations
  ADD CONSTRAINT FK_sessions_TO_presentations
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

-- ---------------------------------------------------------------------------------------------------------------------
-- questions 테이블 외래 키
-- ---------------------------------------------------------------------------------------------------------------------
ALTER TABLE questions
  ADD CONSTRAINT FK_presentations_TO_questions
    FOREIGN KEY (presentation_id)
    REFERENCES presentations (id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION;

ALTER TABLE questions
  ADD CONSTRAINT FK_participants_TO_questions_questioner
    FOREIGN KEY (questioner_id)
    REFERENCES participants (id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION;

-- ---------------------------------------------------------------------------------------------------------------------
-- question_upvotes 테이블 외래 키
-- ---------------------------------------------------------------------------------------------------------------------
ALTER TABLE question_upvotes
  ADD CONSTRAINT FK_questions_TO_question_upvotes
    FOREIGN KEY (question_id)
    REFERENCES questions (id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION;

ALTER TABLE question_upvotes
  ADD CONSTRAINT FK_users_TO_question_upvotes_voter
    FOREIGN KEY (voter_user_id)
    REFERENCES users (id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION;

ALTER TABLE question_upvotes
  ADD CONSTRAINT FK_participants_TO_question_upvotes_guest_voter
    FOREIGN KEY (voter_guest_participant_id)
    REFERENCES participants (id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION;

-- ---------------------------------------------------------------------------------------------------------------------
-- session_participate_codes 테이블 외래 키
-- ---------------------------------------------------------------------------------------------------------------------
ALTER TABLE session_participate_codes
  ADD CONSTRAINT FK_sessions_TO_session_participate_codes
    FOREIGN KEY (session_id)
    REFERENCES sessions (id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION;
