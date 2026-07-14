-- ARGV[1]: 변경할 발표 자료 ID (presentationId)
-- ARGV[2]: 변경할 페이지 번호 (pageNumber)
-- ARGV[3]: 변경 시각 (updatedAt)
-- ARGV[4]: Redis 키 만료 시간(밀리초) (ttl)
-- ARGV[5]: 이벤트를 발행할 Redis 채널 (channel)
-- ARGV[6]: 세션 ID (sessionId)

local current_presentation = redis.call('HGET', KEYS[1], 'presentationId')
local current_page = redis.call('HGET', KEYS[1], 'pageNumber')
local current_revision = tonumber(redis.call('HGET', KEYS[1], 'revision') or '0')
local current_updated_at = redis.call('HGET', KEYS[1], 'updatedAt')

-- 만약 같은 페이지로 변경을 요청받았으면 updatedAt만 재설정하고 Json 반환
if current_presentation == ARGV[1] and current_page == ARGV[2] then
    redis.call('PEXPIRE', KEYS[1], ARGV[4])
    local existing_event = {
        type = 'PRESENTER_VIEW_UPDATED',
        sessionId = ARGV[6],
        presentationId = current_presentation,
        pageNumber = tonumber(current_page),
        revision = current_revision,
        updatedAt = current_updated_at,
        reason = cjson.null
    }
    return cjson.encode({changed = false, event = existing_event})
end

-- revision 1 증가, Redis Hash에 저장
local next_revision = current_revision + 1
redis.call('HSET', KEYS[1],
    'presentationId', ARGV[1],
    'pageNumber', ARGV[2],
    'revision', next_revision,
    'updatedAt', ARGV[3]
)
redis.call('PEXPIRE', KEYS[1], ARGV[4]) -- Key TTL을 재설정

-- 발행할 이벤트 생성
local event = {
    type = 'PRESENTER_VIEW_UPDATED',
    sessionId = ARGV[6],
    presentationId = ARGV[1],
    pageNumber = tonumber(ARGV[2]),
    revision = next_revision,
    updatedAt = ARGV[3],
    reason = cjson.null
}
redis.call('PUBLISH', ARGV[5], cjson.encode(event)) -- 이벤트 객체를 직렬화해서 Pub/Sub 발행
return cjson.encode({changed = true, event = event}) -- Json 변환해서 반환
