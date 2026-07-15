ALTER TABLE question_upvotes
  ALTER COLUMN voter_user_id DROP NOT NULL;

ALTER TABLE question_upvotes
  ADD COLUMN voter_guest_participant_id UUID NULL;

COMMENT ON COLUMN question_upvotes.voter_guest_participant_id IS '공감을 누른 비회원 세션 참여자 PK';

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

CREATE UNIQUE INDEX uq_question_upvotes_question_guest_voter
  ON question_upvotes (question_id, voter_guest_participant_id)
  WHERE voter_guest_participant_id IS NOT NULL;

CREATE INDEX idx_question_upvotes_guest_voter_question
  ON question_upvotes (voter_guest_participant_id, question_id)
  WHERE voter_guest_participant_id IS NOT NULL;
