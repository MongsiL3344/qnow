-- KEYS[1]: 인증번호 키 (codeKey)
-- KEYS[2]: 인증번호 확인 실패 횟수 키 (attemptsKey)
-- KEYS[3]: 반복 요청 방지 키 (cooldownKey)
-- KEYS[4]: 인증번호 발급 횟수 키 (requestCountKey)

-- ARGV[1]: 취소할 인증번호 (code)

if redis.call('GET', KEYS[1]) ~= ARGV[1] then
    return 0
end

redis.call('DEL', KEYS[1], KEYS[2], KEYS[3])

local requestCount = tonumber(redis.call('GET', KEYS[4]) or '0')
if requestCount <= 1 then
    redis.call('DEL', KEYS[4])
else
    redis.call('DECR', KEYS[4])
end

return 1
