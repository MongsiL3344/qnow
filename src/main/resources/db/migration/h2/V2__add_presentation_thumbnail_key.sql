ALTER TABLE presentations
  ADD COLUMN thumbnail_s3_key VARCHAR(1024) NULL;

ALTER TABLE presentations
  ADD CONSTRAINT uq_presentations_thumbnail_s3_key
    UNIQUE (thumbnail_s3_key);
