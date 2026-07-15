ALTER TABLE question_upvotes
  ALTER COLUMN voter_user_id DROP NOT NULL;

ALTER TABLE question_upvotes
  ADD COLUMN voter_guest_participant_id UUID NULL;

ALTER TABLE question_upvotes
  ADD CONSTRAINT FK_participants_TO_question_upvotes_guest_voter
    FOREIGN KEY (voter_guest_participant_id)
    REFERENCES participants (id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION;

ALTER TABLE question_upvotes
  ADD CONSTRAINT chk_question_upvotes_member_or_guest
    CHECK (
      (voter_user_id IS NOT NULL AND voter_guest_participant_id IS NULL)
      OR
      (voter_user_id IS NULL AND voter_guest_participant_id IS NOT NULL)
    );

ALTER TABLE question_upvotes
  ADD CONSTRAINT uq_question_upvotes_question_guest_voter
    UNIQUE NULLS DISTINCT (question_id, voter_guest_participant_id);

CREATE INDEX idx_question_upvotes_guest_voter_question
  ON question_upvotes (voter_guest_participant_id, question_id);
