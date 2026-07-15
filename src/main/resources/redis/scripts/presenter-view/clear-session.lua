-- ARGV[1]: 세션 화면을 비운 시각 (updatedAt)
-- ARGV[2]: 이벤트를 발행할 Redis 채널 (channel)
-- ARGV[3]: 세션 ID (sessionId)
-- ARGV[4]: 발표자 화면을 비운 이유 (reason)

local next_sequence = tonumber(redis.call('HGET', KEYS[1], 'sequence') or '0') + 1
local event = {
    type = 'PRESENTER_VIEW_CLEARED',
    sessionId = ARGV[3],
    presentationId = cjson.null,
    pageNumber = cjson.null,
    sequence = next_sequence,
    updatedAt = ARGV[1],
    reason = ARGV[4]
}
local encoded = cjson.encode(event)
redis.call('PUBLISH', ARGV[2], encoded)
redis.call('DEL', KEYS[1])
return encoded
