CREATE TABLE question_upvotes
(
  id             UUID        NOT NULL,
  question_id    UUID        NOT NULL,
  participant_id UUID        NOT NULL,
  created_at     TIMESTAMPTZ NOT NULL,
  PRIMARY KEY (id)
);

COMMENT ON TABLE question_upvotes IS '질문 좋아요 테이블';
COMMENT ON COLUMN question_upvotes.id IS 'PK';
COMMENT ON COLUMN question_upvotes.question_id IS '질문 PK';
COMMENT ON COLUMN question_upvotes.participant_id IS '좋아요를 누른 세션 참여자 PK';
COMMENT ON COLUMN question_upvotes.created_at IS '좋아요 생성 시각';

ALTER TABLE question_upvotes
  ADD CONSTRAINT FK_questions_TO_question_upvotes
    FOREIGN KEY (question_id)
    REFERENCES questions (id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION;

ALTER TABLE question_upvotes
  ADD CONSTRAINT FK_participants_TO_question_upvotes
    FOREIGN KEY (participant_id)
    REFERENCES participants (id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION;

ALTER TABLE question_upvotes
  ADD CONSTRAINT uq_question_upvotes_question_participant
    UNIQUE (question_id, participant_id);

CREATE INDEX idx_question_upvotes_participant_question
  ON question_upvotes (participant_id, question_id);
