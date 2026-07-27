-- KEYS[1]: 인증번호 키 (codeKey)
-- KEYS[2]: 인증번호 확인 실패 횟수 키 (attemptsKey)
-- KEYS[3]: 반복 요청 방지 키 (cooldownKey)
-- KEYS[4]: 인증번호 발급 횟수 키 (requestCountKey)
-- KEYS[5]: 인증 완료 상태 키 (verifiedKey)

-- ARGV[1]: 발급할 인증번호 (code)
-- ARGV[2]: 인증번호 및 확인 실패 횟수 키 만료 시간(밀리초) (codeTtl)
-- ARGV[3]: 반복 요청 방지 키 만료 시간(밀리초) (requestCooldown)
-- ARGV[4]: 인증번호 발급 횟수 제한 시간(밀리초) (requestLimitWindow)
-- ARGV[5]: 제한 시간 내 최대 인증번호 발급 횟수 (requestLimit)

if redis.call('EXISTS', KEYS[5]) == 1 then
    return 'ALREADY_VERIFIED'
end

if redis.call('EXISTS', KEYS[3]) == 1 then
    return 'TOO_FREQUENT'
end

local requestCount = tonumber(redis.call('GET', KEYS[4]) or '0')

if requestCount >= tonumber(ARGV[5]) then
    return 'REQUEST_LIMIT_EXCEEDED'
end

requestCount = redis.call('INCR', KEYS[4])

if requestCount == 1 then
    redis.call('PEXPIRE', KEYS[4], ARGV[4])
end

redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[2])
redis.call('SET', KEYS[2], '0', 'PX', ARGV[2])
redis.call('SET', KEYS[3], '1', 'PX', ARGV[3])

return 'CREATED'
