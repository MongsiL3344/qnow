-- ARGV[1]: 변경할 발표 자료 ID (presentationId)
-- ARGV[2]: 변경할 페이지 번호 (pageNumber)
-- ARGV[3]: 변경 시각 (updatedAt)
-- ARGV[4]: Redis 키 만료 시간(밀리초) (ttl)
-- ARGV[5]: 이벤트를 발행할 Redis 채널 (channel)
-- ARGV[6]: 세션 ID (sessionId)

local current_values = redis.call(
    'HMGET',
    KEYS[1],
    'presentationId',
    'pageNumber',
    'sequence',
    'updatedAt'
)
local current_presentation = current_values[1]
local current_page = current_values[2]
local current_sequence = tonumber(current_values[3] or '0')
local current_updated_at = current_values[4]

-- 같은 페이지로 변경을 요청받았으면 TTL만 갱신하고 기존 상태를 반환
if current_presentation == ARGV[1] and current_page == ARGV[2] then
    redis.call('PEXPIRE', KEYS[1], ARGV[4]) -- TTL 설정
    local existing_event = {
        type = 'PRESENTER_VIEW_UPDATED',
        sessionId = ARGV[6],
        presentationId = current_presentation,
        pageNumber = tonumber(current_page),
        sequence = current_sequence,
        updatedAt = current_updated_at,
        reason = cjson.null
    }
    return cjson.encode(existing_event)
end

-- sequence 1 증가, Redis Hash에 저장
local next_sequence = current_sequence + 1

-- Redis Hash에 저장 (HSET <키> <필드> <값>)
redis.call('HSET', KEYS[1],
    'presentationId', ARGV[1],
    'pageNumber', ARGV[2],
    'sequence', next_sequence,
    'updatedAt', ARGV[3]
)

-- Key TTL을 재설정
redis.call('PEXPIRE', KEYS[1], ARGV[4])

-- 발행할 이벤트 생성
local event = {
    type = 'PRESENTER_VIEW_UPDATED',
    sessionId = ARGV[6],
    presentationId = ARGV[1],
    pageNumber = tonumber(ARGV[2]),
    sequence = next_sequence,
    updatedAt = ARGV[3],
    reason = cjson.null
}

-- 이벤트 객체를 직렬화해서 Pub/Sub 발행
local encoded = cjson.encode(event)
redis.call('PUBLISH', ARGV[5], encoded)

-- Json 변환해서 반환
return encoded
