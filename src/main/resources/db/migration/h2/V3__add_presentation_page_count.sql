ALTER TABLE presentations
  ADD COLUMN page_count INT NOT NULL DEFAULT 1;

ALTER TABLE presentations
  ADD CONSTRAINT chk_presentations_page_count_positive
    CHECK (page_count > 0);
