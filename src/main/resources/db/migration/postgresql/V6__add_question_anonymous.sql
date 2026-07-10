ALTER TABLE questions
  ADD COLUMN is_anonymous BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN questions.is_anonymous IS '질문 익명 여부';
