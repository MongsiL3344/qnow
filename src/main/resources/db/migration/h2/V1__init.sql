CREATE TABLE event_publication
(
  id                     CHAR(36)                 NOT NULL,
  completion_date        TIMESTAMP(9) WITH TIME ZONE,
  event_type             VARCHAR(512)             NOT NULL,
  listener_id            VARCHAR(512)             NOT NULL,
  publication_date       TIMESTAMP(9) WITH TIME ZONE NOT NULL,
  serialized_event       VARCHAR(4000)            NOT NULL,
  status                 VARCHAR(20),
  completion_attempts    INT,
  last_resubmission_date TIMESTAMP(9) WITH TIME ZONE,
  PRIMARY KEY (id)
);

CREATE INDEX event_publication_by_listener_id_and_serialized_event_idx
  ON event_publication (listener_id, serialized_event);

CREATE INDEX event_publication_by_completion_date_idx
  ON event_publication (completion_date);

CREATE TABLE users
(
  id         CHAR(36)     NOT NULL,
  email      VARCHAR(255) NOT NULL,
  nickname   VARCHAR(30)  NOT NULL,
  username   VARCHAR(30)  NOT NULL,
  password   VARCHAR(255) NOT NULL,
  status     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMP    NOT NULL,
  deleted_at TIMESTAMP    NULL,
  PRIMARY KEY (id)
);

CREATE TABLE organizations
(
  id         CHAR(36)     NOT NULL,
  name       VARCHAR(30)  NOT NULL,
  detail     VARCHAR(255) NULL,
  password   VARCHAR(255) NULL,
  status     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMP    NOT NULL,
  deleted_at TIMESTAMP    NULL,
  PRIMARY KEY (id)
);

CREATE TABLE sessions
(
  id              CHAR(36)     NOT NULL,
  organization_id CHAR(36)     NOT NULL,
  title           VARCHAR(255) NOT NULL,
  status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
  start_at        TIMESTAMP    NULL,
  end_at          TIMESTAMP    NULL,
  PRIMARY KEY (id)
);

CREATE TABLE user_groups
(
  id              CHAR(36)    NOT NULL,
  user_id         CHAR(36)    NOT NULL,
  organization_id CHAR(36)    NOT NULL,
  role            VARCHAR(20) NOT NULL DEFAULT 'USER',
  status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  PRIMARY KEY (id)
);

CREATE TABLE participants
(
  id         CHAR(36)    NOT NULL,
  user_id    CHAR(36)    NOT NULL,
  session_id CHAR(36)    NOT NULL,
  status     VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  PRIMARY KEY (id)
);

CREATE TABLE presentations
(
  id                 CHAR(36)     NOT NULL,
  session_id         CHAR(36)     NOT NULL,
  presenter_id       CHAR(36)     NOT NULL,
  presentation_order INT          NULL,
  title              VARCHAR(255) NOT NULL,
  PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IDX_user_groups
  ON user_groups (user_id, organization_id);

CREATE UNIQUE INDEX uq_participants_session_user
  ON participants (session_id, user_id);

CREATE UNIQUE INDEX uq_participants_session_id
  ON participants (session_id, id);

CREATE UNIQUE INDEX uq_presentations_session_order
  ON presentations (session_id, presentation_order);

ALTER TABLE user_groups
  ADD CONSTRAINT FK_users_TO_user_groups
    FOREIGN KEY (user_id)
    REFERENCES users (id);

ALTER TABLE user_groups
  ADD CONSTRAINT FK_organizations_TO_user_groups
    FOREIGN KEY (organization_id)
    REFERENCES organizations (id);

ALTER TABLE sessions
  ADD CONSTRAINT FK_organizations_TO_sessions
    FOREIGN KEY (organization_id)
    REFERENCES organizations (id);

ALTER TABLE presentations
  ADD CONSTRAINT FK_sessions_TO_presentations
    FOREIGN KEY (session_id)
    REFERENCES sessions (id);

ALTER TABLE participants
  ADD CONSTRAINT FK_users_TO_participants
    FOREIGN KEY (user_id)
    REFERENCES users (id);

ALTER TABLE participants
  ADD CONSTRAINT FK_sessions_TO_participants
    FOREIGN KEY (session_id)
    REFERENCES sessions (id);

ALTER TABLE presentations
  ADD CONSTRAINT FK_presentations_TO_participants
    FOREIGN KEY (session_id, presenter_id)
    REFERENCES participants (session_id, id);
