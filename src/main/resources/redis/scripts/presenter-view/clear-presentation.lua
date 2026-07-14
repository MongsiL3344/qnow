-- ARGV[1]: 비울 발표 자료 ID (presentationId)
-- ARGV[2]: 비운 시각 (updatedAt)
-- ARGV[3]: Redis 키 만료 시간(밀리초) (ttl)
-- ARGV[4]: 이벤트를 발행할 Redis 채널 (channel)
-- ARGV[5]: 세션 ID (sessionId)
-- ARGV[6]: 발표자 화면을 비운 이유 (reason)

local current_presentation = redis.call('HGET', KEYS[1], 'presentationId')
if not current_presentation or current_presentation ~= ARGV[1] then
    return ''
end

local next_revision = tonumber(redis.call('HGET', KEYS[1], 'revision') or '0') + 1
redis.call('HDEL', KEYS[1], 'presentationId', 'pageNumber')
redis.call('HSET', KEYS[1], 'revision', next_revision, 'updatedAt', ARGV[2])
redis.call('PEXPIRE', KEYS[1], ARGV[3])

local event = {
    type = 'PRESENTER_VIEW_CLEARED',
    sessionId = ARGV[5],
    presentationId = cjson.null,
    pageNumber = cjson.null,
    revision = next_revision,
    updatedAt = ARGV[2],
    reason = ARGV[6]
}
local encoded = cjson.encode(event)
redis.call('PUBLISH', ARGV[4], encoded)
return encoded
