CREATE INDEX idx_sessions_active_organization_created_at
  ON sessions (organization_id, created_at DESC);
