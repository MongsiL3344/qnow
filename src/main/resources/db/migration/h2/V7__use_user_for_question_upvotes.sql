ALTER TABLE question_upvotes
  ADD COLUMN voter_user_id UUID NULL;

UPDATE question_upvotes
SET voter_user_id = (
  SELECT participant.user_id
  FROM participants AS participant
  WHERE participant.id = question_upvotes.participant_id
);

DELETE FROM question_upvotes
WHERE EXISTS (
  SELECT 1
  FROM questions AS question
  JOIN participants AS questioner
    ON questioner.id = question.questioner_id
  WHERE question.id = question_upvotes.question_id
    AND questioner.user_id = question_upvotes.voter_user_id
);

DELETE FROM question_upvotes
WHERE id IN (
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
);

UPDATE questions
SET upvote_count = (
  SELECT CAST(COUNT(*) AS INT)
  FROM question_upvotes
  WHERE question_upvotes.question_id = questions.id
);

ALTER TABLE question_upvotes
  ALTER COLUMN voter_user_id SET NOT NULL;

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
