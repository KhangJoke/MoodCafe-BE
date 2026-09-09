local current = redis.call('INCR', KEYS[1])
-- ARGV[1] = window duration in seconds
if current == 1 then
    redis.call('EXPIRE', KEYS[1], ARGV[1])
end
-- ARGV[2] = max allowed requests
if current > tonumber(ARGV[2]) then
    return 0
else
    return 1
end
