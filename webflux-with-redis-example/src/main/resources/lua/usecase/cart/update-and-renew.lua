if tonumber(ARGV[2]) == 0 then
    redis.call('HDEL', KEYS[1], ARGV[1])
else
    redis.call('HSET', KEYS[1], ARGV[1], ARGV[2])
end
redis.call('EXPIRE', KEYS[1], ARGV[3])
return redis.call('HLEN', KEYS[1])
