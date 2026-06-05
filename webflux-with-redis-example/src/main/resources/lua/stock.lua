local key = KEYS[1]
local quantity = tonumber(ARGV[1])

local current = tonumber(redis.call("GET", key))

if current == nil then
    return -1
end

if current < quantity then
    return -1
end

local newStock = current - quantity
redis.call("SET", key, newStock)

return newStock
