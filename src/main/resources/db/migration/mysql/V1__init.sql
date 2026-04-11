CREATE TABLE event_publication
(
  id                     CHAR(36)      NOT NULL,
  listener_id            VARCHAR(512)  NOT NULL,
  event_type             VARCHAR(512)  NOT NULL,
  serialized_event       VARCHAR(4000) NOT NULL,
  publication_date       TIMESTAMP(6)  NOT NULL,
  completion_date        TIMESTAMP(6)  NULL     DEFAULT NULL,
  status                 VARCHAR(20)   NULL     DEFAULT NULL,
  completion_attempts    INT           NULL     DEFAULT NULL,
  last_resubmission_date TIMESTAMP(6)  NULL     DEFAULT NULL,
  PRIMARY KEY (id),
  INDEX event_publication_by_completion_date_idx (completion_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Spring Modulith event publication log';

CREATE TABLE users
(
  id         CHAR(36)     NOT NULL COMMENT 'PK',
  email      VARCHAR(255) NOT NULL COMMENT '이메일',
  nickname   VARCHAR(30)  NOT NULL COMMENT '닉네임',
  username   VARCHAR(30)  NOT NULL COMMENT '로그인할때 쓰는 아이디',
  password   VARCHAR(255) NOT NULL COMMENT '해시 된 비밀번호',
  status     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '유저 상태',
  created_at DATETIME     NOT NULL COMMENT '계정 생성 시각',
  deleted_at DATETIME     NULL     DEFAULT NULL COMMENT '계정 삭제 시각',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='유저 테이블';

CREATE TABLE organizations
(
  id         CHAR(36)     NOT NULL COMMENT 'PK',
  name       VARCHAR(30)  NOT NULL COMMENT '조직 명',
  detail     VARCHAR(255) NULL     DEFAULT NULL COMMENT '조직 상세설명',
  password   VARCHAR(255) NULL     DEFAULT NULL COMMENT '조직 입장 비밀번호',
  status     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '조직의 현재 상태',
  created_at DATETIME     NOT NULL COMMENT '조직 생성 시각',
  deleted_at DATETIME     NULL     DEFAULT NULL COMMENT '조직 삭제 시각',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='조직 테이블';

CREATE TABLE sessions
(
  id              CHAR(36)     NOT NULL COMMENT 'PK',
  organization_id CHAR(36)     NOT NULL COMMENT '조직 PK',
  title           VARCHAR(255) NOT NULL COMMENT '세션 제목',
  status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '세션의 상태',
  start_at        DATETIME     NULL     DEFAULT NULL COMMENT '세션 시작 시간',
  end_at          DATETIME     NULL     DEFAULT NULL COMMENT '세션 종료 시간',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='조직에 속한 발표 세션 테이블';

CREATE TABLE user_groups
(
  id              CHAR(36)    NOT NULL COMMENT 'PK',
  user_id         CHAR(36)    NOT NULL COMMENT '유저 테이블 PK',
  organization_id CHAR(36)    NOT NULL COMMENT '조직 테이블 PK',
  role            VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '조직 내 유저 역할 (관리자/유저)',
  status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '조직 내 유저 상태',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='유저가 참여하고있는 조직 목록 테이블';

CREATE TABLE participants
(
  id         CHAR(36)    NOT NULL COMMENT 'PK',
  user_id    CHAR(36)    NOT NULL COMMENT '유저 PK',
  session_id CHAR(36)    NOT NULL COMMENT '세션 PK',
  status     VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '세션 참여자의 상태',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='세션에 속한 세션참여자';

CREATE TABLE presentations
(
  id                 CHAR(36)     NOT NULL COMMENT 'PK',
  session_id         CHAR(36)     NOT NULL COMMENT '세션 PK',
  presenter_id       CHAR(36)     NOT NULL COMMENT '발표자 PK',
  presentation_order INT          NULL     DEFAULT NULL COMMENT '발표 순서',
  title              VARCHAR(255) NOT NULL COMMENT '발표 제목',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='세션에 속한 발표 테이블';

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
