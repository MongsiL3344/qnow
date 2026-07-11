ALTER TABLE question_upvotes
  ADD COLUMN voter_user_id UUID NULL;

UPDATE question_upvotes AS question_upvote
SET voter_user_id = participant.user_id
FROM participants AS participant
WHERE participant.id = question_upvote.participant_id;

DELETE FROM question_upvotes AS question_upvote
USING questions AS question,
      participants AS questioner
WHERE question.id = question_upvote.question_id
  AND questioner.id = question.questioner_id
  AND questioner.user_id = question_upvote.voter_user_id;

DELETE FROM question_upvotes AS question_upvote
USING (
  SELECT id
  FROM (
    SELECT
      id,
      ROW_NUMBER() OVER (
        PARTITION BY question_id, voter_user_id
        ORDER BY created_at ASC, id ASC
      ) AS duplicate_order
    FROM question_upvotes
  ) AS ranked_question_upvotes
  WHERE duplicate_order > 1
) AS duplicate
WHERE question_upvote.id = duplicate.id;

UPDATE questions AS question
SET upvote_count = (
  SELECT COUNT(*)::INT
  FROM question_upvotes AS question_upvote
  WHERE question_upvote.question_id = question.id
);

ALTER TABLE question_upvotes
  ALTER COLUMN voter_user_id SET NOT NULL;

COMMENT ON COLUMN question_upvotes.voter_user_id IS '좋아요를 누른 유저 PK';

ALTER TABLE question_upvotes
  ADD CONSTRAINT FK_users_TO_question_upvotes_voter
    FOREIGN KEY (voter_user_id)
    REFERENCES users (id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION;

ALTER TABLE question_upvotes
  DROP CONSTRAINT uq_question_upvotes_question_participant;

ALTER TABLE question_upvotes
  ADD CONSTRAINT uq_question_upvotes_question_voter
    UNIQUE (question_id, voter_user_id);

DROP INDEX idx_question_upvotes_participant_question;

CREATE INDEX idx_question_upvotes_voter_question
  ON question_upvotes (voter_user_id, question_id);
