local key = KEYS[1]
local otp = ARGV[1]
local ttl = tonumber(ARGV[2])

redis.call('SETEX', key, ttl, otp)

return 1