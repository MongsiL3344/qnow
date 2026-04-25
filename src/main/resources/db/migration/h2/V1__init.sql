CREATE TABLE event_publication
(
  id                     UUID                     NOT NULL,
  listener_id            VARCHAR(512)             NOT NULL,
  event_type             VARCHAR(512)             NOT NULL,
  serialized_event       VARCHAR(4000)            NOT NULL,
  publication_date       TIMESTAMP WITH TIME ZONE NOT NULL,
  completion_date        TIMESTAMP WITH TIME ZONE NULL,
  status                 VARCHAR(20)              NULL,
  completion_attempts    INT                      NULL,
  last_resubmission_date TIMESTAMP WITH TIME ZONE NULL,
  PRIMARY KEY (id)
);

CREATE INDEX event_publication_by_listener_id_and_serialized_event_idx
  ON event_publication (listener_id, serialized_event);

CREATE INDEX event_publication_by_completion_date_idx
  ON event_publication (completion_date);

CREATE TABLE users
(
  id         UUID                     NOT NULL,
  email      VARCHAR(255)             NOT NULL,
  nickname   VARCHAR(30)              NOT NULL,
  username   VARCHAR(30)              NOT NULL,
  password   VARCHAR(255)             NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  deleted_at TIMESTAMP WITH TIME ZONE NULL,
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
  id              UUID                     NOT NULL,
  organization_id UUID                     NOT NULL,
  creator_id      UUID                     NOT NULL,
  title           VARCHAR(255)             NOT NULL,
  start_at        TIMESTAMP WITH TIME ZONE NULL,
  end_at          TIMESTAMP WITH TIME ZONE NULL,
  created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
  deleted_at      TIMESTAMP WITH TIME ZONE NULL,
  PRIMARY KEY (id)
);

CREATE TABLE user_groups
(
  id              UUID        NOT NULL,
  user_id         UUID        NOT NULL,
  organization_id UUID        NOT NULL,
  role            VARCHAR(20) NOT NULL DEFAULT 'USER',
  created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
  deleted_at      TIMESTAMP WITH TIME ZONE NULL,
  PRIMARY KEY (id)
);

CREATE TABLE participants
(
  id         UUID                     NOT NULL,
  user_id    UUID                     NOT NULL,
  session_id UUID                     NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  deleted_at TIMESTAMP WITH TIME ZONE NULL,
  PRIMARY KEY (id)
);

CREATE TABLE presentations
(
  id                 UUID                     NOT NULL,
  session_id         UUID                     NOT NULL,
  presenter_id       UUID                     NOT NULL,
  presentation_order INT                      NULL,
  title              VARCHAR(255)             NOT NULL,
  created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
  deleted_at         TIMESTAMP WITH TIME ZONE NULL,
  PRIMARY KEY (id)
);

ALTER TABLE users
  ADD CONSTRAINT uq_users_email
    UNIQUE (email);

ALTER TABLE users
  ADD CONSTRAINT uq_users_username
    UNIQUE (username);

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
  ADD CONSTRAINT uq_presentations_session_order
    UNIQUE (session_id, presentation_order);

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
  ADD CONSTRAINT FK_presentations_TO_participants
    FOREIGN KEY (session_id, presenter_id)
    REFERENCES participants (session_id, id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION;
