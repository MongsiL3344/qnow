-- ---------------------------------------------------------------------------------------------------------------------
-- 질문 발표 제어 요청 컬럼 추가
-- ---------------------------------------------------------------------------------------------------------------------
ALTER TABLE questions
  ADD COLUMN kind VARCHAR(30) NOT NULL DEFAULT 'QUESTION';

ALTER TABLE questions
  ADD COLUMN approved_at TIMESTAMP WITH TIME ZONE NULL;

COMMENT ON COLUMN questions.kind IS '질문 종류 (QUESTION | CONTROL_REQUEST)';
COMMENT ON COLUMN questions.approved_at IS '발표 제어 요청 승인 시각';
