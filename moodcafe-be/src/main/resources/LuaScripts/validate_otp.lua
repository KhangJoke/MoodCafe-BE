local key = KEYS[1]
local input = ARGV[1]

local stored = redis.call("GET", key)

if stored == input then
    redis.call("DEL", key)
    return 1
else
    return 0
end