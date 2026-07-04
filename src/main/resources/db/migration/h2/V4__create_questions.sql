CREATE TABLE questions
(
  id                     UUID                     NOT NULL,
  presentation_id        UUID                     NOT NULL,
  questioner_id          UUID                     NOT NULL,
  content                TEXT                     NOT NULL,
  page_start             INT                      NOT NULL,
  page_end               INT                      NOT NULL,
  upvote_count           INT                      NOT NULL DEFAULT 0,
  selection_left_ratio   NUMERIC(6, 5)            NULL,
  selection_top_ratio    NUMERIC(6, 5)            NULL,
  selection_width_ratio  NUMERIC(6, 5)            NULL,
  selection_height_ratio NUMERIC(6, 5)            NULL,
  created_at             TIMESTAMP WITH TIME ZONE NOT NULL,
  deleted_at             TIMESTAMP WITH TIME ZONE NULL,
  PRIMARY KEY (id)
);

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
  ON questions (presentation_id, created_at DESC, id DESC);

CREATE INDEX idx_questions_active_presentation_page
  ON questions (presentation_id, page_start ASC, page_end ASC, created_at ASC, id ASC);

CREATE INDEX idx_questions_active_presentation_upvote_count
  ON questions (presentation_id, upvote_count DESC, created_at ASC, id ASC);
