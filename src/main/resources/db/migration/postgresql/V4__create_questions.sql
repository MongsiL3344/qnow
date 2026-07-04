CREATE TABLE questions
(
  id                     UUID        NOT NULL,
  presentation_id        UUID        NOT NULL,
  questioner_id          UUID        NOT NULL,
  content                TEXT        NOT NULL,
  page_start             INT         NOT NULL,
  page_end               INT         NOT NULL,
  upvote_count           INT         NOT NULL DEFAULT 0,
  selection_left_ratio   NUMERIC(6, 5) NULL,
  selection_top_ratio    NUMERIC(6, 5) NULL,
  selection_width_ratio  NUMERIC(6, 5) NULL,
  selection_height_ratio NUMERIC(6, 5) NULL,
  created_at             TIMESTAMPTZ NOT NULL,
  deleted_at             TIMESTAMPTZ NULL,
  PRIMARY KEY (id)
);

COMMENT ON TABLE questions IS '발표 자료에 연결된 질문 테이블';
COMMENT ON COLUMN questions.id IS 'PK';
COMMENT ON COLUMN questions.presentation_id IS '발표 PK';
COMMENT ON COLUMN questions.questioner_id IS '질문자 세션 참여자 PK';
COMMENT ON COLUMN questions.content IS '질문 내용';
COMMENT ON COLUMN questions.page_start IS '질문이 연결된 시작 페이지';
COMMENT ON COLUMN questions.page_end IS '질문이 연결된 끝 페이지';
COMMENT ON COLUMN questions.upvote_count IS '질문 좋아요 수';
COMMENT ON COLUMN questions.selection_left_ratio IS '선택 영역의 왼쪽 위치 비율';
COMMENT ON COLUMN questions.selection_top_ratio IS '선택 영역의 위쪽 위치 비율';
COMMENT ON COLUMN questions.selection_width_ratio IS '선택 영역의 너비 비율';
COMMENT ON COLUMN questions.selection_height_ratio IS '선택 영역의 높이 비율';
COMMENT ON COLUMN questions.created_at IS '질문 생성 시각';
COMMENT ON COLUMN questions.deleted_at IS '질문 삭제 시각';

ALTER TABLE questions
  ADD CONSTRAINT FK_presentations_TO_questions
    FOREIGN KEY (presentation_id)
    REFERENCES presentations (id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION;

ALTER TABLE questions
  ADD CONSTRAINT FK_participants_TO_questions_questioner
    FOREIGN KEY (questioner_id)
    REFERENCES participants (id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION;

ALTER TABLE questions
  ADD CONSTRAINT chk_questions_page_range
    CHECK (
      page_start >= 1
      AND page_end >= page_start
    );

ALTER TABLE questions
  ADD CONSTRAINT chk_questions_upvote_count_non_negative
    CHECK (upvote_count >= 0);

ALTER TABLE questions
  ADD CONSTRAINT chk_questions_selection_single_page_only
    CHECK (
      (
        selection_left_ratio IS NULL
        AND selection_top_ratio IS NULL
        AND selection_width_ratio IS NULL
        AND selection_height_ratio IS NULL
      )
      OR
      (
        page_start = page_end
        AND selection_left_ratio IS NOT NULL
        AND selection_top_ratio IS NOT NULL
        AND selection_width_ratio IS NOT NULL
        AND selection_height_ratio IS NOT NULL
        AND selection_left_ratio >= 0
        AND selection_top_ratio >= 0
        AND selection_width_ratio > 0
        AND selection_height_ratio > 0
        AND selection_left_ratio + selection_width_ratio <= 1
        AND selection_top_ratio + selection_height_ratio <= 1
      )
    );

CREATE INDEX idx_questions_active_presentation_created_at
  ON questions (presentation_id, created_at DESC, id DESC)
  WHERE deleted_at IS NULL;

CREATE INDEX idx_questions_active_presentation_page
  ON questions (presentation_id, page_start ASC, page_end ASC, created_at ASC, id ASC)
  WHERE deleted_at IS NULL;

CREATE INDEX idx_questions_active_presentation_upvote_count
  ON questions (presentation_id, upvote_count DESC, created_at ASC, id ASC)
  WHERE deleted_at IS NULL;
