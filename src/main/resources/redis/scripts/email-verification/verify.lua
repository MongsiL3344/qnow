-- KEYS[1]: 인증번호 키 (codeKey)
-- KEYS[2]: 인증번호 확인 실패 횟수 키 (attemptsKey)
-- KEYS[3]: 인증 완료 상태 키 (verifiedKey)
-- KEYS[4]: 반복 요청 방지 키 (cooldownKey)

-- ARGV[1]: 사용자가 입력한 인증번호 (code)
-- ARGV[2]: 최대 인증번호 확인 실패 횟수 (maxAttempts)
-- ARGV[3]: 인증 완료 상태 키 만료 시간(밀리초) (verifiedTtl)

if redis.call('EXISTS', KEYS[3]) == 1 then
    return 'ALREADY_VERIFIED'
end

local attempts = tonumber(redis.call('GET', KEYS[2]) or '0')
if attempts >= tonumber(ARGV[2]) then
    return 'ATTEMPTS_EXCEEDED'
end

local expectedCode = redis.call('GET', KEYS[1])
if not expectedCode then
    return 'CODE_EXPIRED'
end

if expectedCode ~= ARGV[1] then
    attempts = redis.call('INCR', KEYS[2])
    local codeTtl = redis.call('PTTL', KEYS[1])
    if codeTtl > 0 then
        redis.call('PEXPIRE', KEYS[2], codeTtl)
    end
    if attempts >= tonumber(ARGV[2]) then
        return 'ATTEMPTS_EXCEEDED'
    end
    return 'CODE_MISMATCH'
end

redis.call('SET', KEYS[3], '1', 'PX', ARGV[3])
redis.call('DEL', KEYS[1], KEYS[2], KEYS[4])

return 'VERIFIED'
