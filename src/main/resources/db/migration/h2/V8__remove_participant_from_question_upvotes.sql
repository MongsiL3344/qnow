ALTER TABLE question_upvotes
  DROP CONSTRAINT FK_participants_TO_question_upvotes;

ALTER TABLE question_upvotes
  DROP COLUMN participant_id;
