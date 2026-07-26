CREATE TABLE event_publication
(
  id                     UUID                     NOT NULL,
  listener_id            VARCHAR(512)             NOT NULL,
  event_type              VARCHAR(512)             NOT NULL,
  serialized_event       VARCHAR(4000)            NOT NULL,
  publication_date       TIMESTAMP WITH TIME ZONE NOT NULL,
  completion_date        TIMESTAMP WITH TIME ZONE NULL,
  status                  VARCHAR(20)              NULL,
  completion_attempts    INT                      NULL,
  last_resubmission_date TIMESTAMP WITH TIME ZONE NULL,
  PRIMARY KEY (id)
);

CREATE TABLE users
(
  id                UUID                     NOT NULL,
  email             VARCHAR(255)             NOT NULL,
  nickname          VARCHAR(30)              NOT NULL,
  password          VARCHAR(255)             NOT NULL,
  email_verified_at TIMESTAMP WITH TIME ZONE NULL,
  created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
  deleted_at        TIMESTAMP WITH TIME ZONE NULL,
  PRIMARY KEY (id)
);

CREATE TABLE organizations
(
  id         UUID                     NOT NULL,
  name       VARCHAR(30)              NOT NULL,
  detail     VARCHAR(255)             NULL,
  password   VARCHAR(255)             NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  deleted_at TIMESTAMP WITH TIME ZONE NULL,
  PRIMARY KEY (id)
);

CREATE TABLE sessions
(
  id                   UUID                     NOT NULL,
  organization_id      UUID                     NOT NULL,
  creator_id           UUID                     NOT NULL,
  title                VARCHAR(255)             NOT NULL,
  start_at             TIMESTAMP WITH TIME ZONE NULL,
  end_at               TIMESTAMP WITH TIME ZONE NULL,
  guest_upvote_allowed BOOLEAN                  NOT NULL DEFAULT FALSE,
  created_at           TIMESTAMP WITH TIME ZONE NOT NULL,
  deleted_at           TIMESTAMP WITH TIME ZONE NULL,
  PRIMARY KEY (id)
);

CREATE TABLE user_groups
(
  id              UUID                     NOT NULL,
  user_id         UUID                     NOT NULL,
  organization_id UUID                     NOT NULL,
  role            VARCHAR(20)              NOT NULL DEFAULT 'USER',
  created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
  deleted_at      TIMESTAMP WITH TIME ZONE NULL,
  PRIMARY KEY (id)
);

CREATE TABLE participants
(
  id             UUID                     NOT NULL,
  user_id        UUID                     NULL,
  guest_nickname VARCHAR(30)              NULL,
  session_id     UUID                     NOT NULL,
  created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
  deleted_at     TIMESTAMP WITH TIME ZONE NULL,
  PRIMARY KEY (id)
);

CREATE TABLE presentations
(
  id               UUID                     NOT NULL,
  session_id       UUID                     NOT NULL,
  presenter_id     UUID                     NOT NULL,
  title            VARCHAR(255)             NOT NULL,
  content_type     VARCHAR(100)             NOT NULL DEFAULT 'application/pdf',
  s3_key           VARCHAR(1024)            NOT NULL,
  thumbnail_s3_key VARCHAR(1024)            NULL,
  page_count       INT                      NOT NULL DEFAULT 1,
  upload_status    VARCHAR(20)              NOT NULL DEFAULT 'PENDING',
  created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
  deleted_at       TIMESTAMP WITH TIME ZONE NULL,
  PRIMARY KEY (id)
);

CREATE TABLE questions
(
  id                     UUID                     NOT NULL,
  presentation_id        UUID                     NOT NULL,
  questioner_id          UUID                     NOT NULL,
  content                TEXT                     NOT NULL,
  is_anonymous           BOOLEAN                  NOT NULL DEFAULT FALSE,
  page_start             INT                      NOT NULL,
  page_end               INT                      NOT NULL,
  upvote_count           INT                      NOT NULL DEFAULT 0,
  selection_left_ratio   NUMERIC(6, 5)            NULL,
  selection_top_ratio    NUMERIC(6, 5)            NULL,
  selection_width_ratio  NUMERIC(6, 5)            NULL,
  selection_height_ratio NUMERIC(6, 5)            NULL,
  created_at             TIMESTAMP WITH TIME ZONE NOT NULL,
  deleted_at             TIMESTAMP WITH TIME ZONE NULL,
  PRIMARY KEY (id)
);

CREATE TABLE question_upvotes
(
  id                         UUID                     NOT NULL,
  question_id                UUID                     NOT NULL,
  voter_user_id              UUID                     NULL,
  voter_guest_participant_id UUID                     NULL,
  created_at                 TIMESTAMP WITH TIME ZONE NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE session_participate_codes
(
  id         UUID                     NOT NULL,
  session_id UUID                     NOT NULL,
  code       VARCHAR(64)              NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  deleted_at TIMESTAMP WITH TIME ZONE NULL,
  PRIMARY KEY (id)
);

CREATE INDEX event_publication_by_listener_id_and_serialized_event_idx
  ON event_publication (listener_id, serialized_event);

CREATE INDEX event_publication_by_completion_date_idx
  ON event_publication (completion_date);

ALTER TABLE users
  ADD CONSTRAINT uq_users_email
    UNIQUE (email);

ALTER TABLE users
  ADD CONSTRAINT uq_users_nickname
    UNIQUE (nickname);

ALTER TABLE organizations
  ADD CONSTRAINT uq_organizations_name
    UNIQUE (name);

ALTER TABLE user_groups
  ADD CONSTRAINT uq_user_groups_user_organization
    UNIQUE (user_id, organization_id);

ALTER TABLE participants
  ADD CONSTRAINT uq_participants_session_user
    UNIQUE (session_id, user_id);

ALTER TABLE participants
  ADD CONSTRAINT uq_participants_session_id
    UNIQUE (session_id, id);

ALTER TABLE presentations
  ADD CONSTRAINT uq_presentations_s3_key
    UNIQUE (s3_key);

ALTER TABLE presentations
  ADD CONSTRAINT uq_presentations_thumbnail_s3_key
    UNIQUE (thumbnail_s3_key);

ALTER TABLE presentations
  ADD CONSTRAINT chk_presentations_page_count_positive
    CHECK (page_count > 0);

CREATE INDEX idx_sessions_active_organization_created_at
  ON sessions (organization_id, created_at DESC);

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

CREATE INDEX idx_questions_active_presentation_created_at
  ON questions (presentation_id, created_at DESC, id DESC);

CREATE INDEX idx_questions_active_presentation_page
  ON questions (presentation_id, page_start ASC, page_end ASC, created_at ASC, id ASC);

CREATE INDEX idx_questions_active_presentation_upvote_count
  ON questions (presentation_id, upvote_count DESC, created_at ASC, id ASC);

ALTER TABLE participants
  ADD CONSTRAINT chk_participants_member_or_guest
    CHECK (
      (user_id IS NOT NULL AND guest_nickname IS NULL)
      OR
      (user_id IS NULL AND guest_nickname IS NOT NULL)
    );

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

ALTER TABLE question_upvotes
  ADD CONSTRAINT uq_question_upvotes_question_guest_voter
    UNIQUE NULLS DISTINCT (question_id, voter_guest_participant_id);

CREATE INDEX idx_question_upvotes_voter_question
  ON question_upvotes (voter_user_id, question_id);

CREATE INDEX idx_question_upvotes_guest_voter_question
  ON question_upvotes (voter_guest_participant_id, question_id);

ALTER TABLE session_participate_codes
  ADD CONSTRAINT uq_session_participate_codes_code
    UNIQUE (code);

ALTER TABLE session_participate_codes
  ADD CONSTRAINT uq_session_participate_codes_active_session
    UNIQUE NULLS NOT DISTINCT (session_id, deleted_at);

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

ALTER TABLE session_participate_codes
  ADD CONSTRAINT FK_sessions_TO_session_participate_codes
    FOREIGN KEY (session_id)
    REFERENCES sessions (id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION;
