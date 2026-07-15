ALTER TABLE sessions
  ADD COLUMN guest_upvote_allowed BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN sessions.guest_upvote_allowed IS '비회원 질문 공감 허용 여부';
