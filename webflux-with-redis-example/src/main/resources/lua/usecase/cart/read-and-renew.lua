local items = redis.call('HGETALL', KEYS[1])
redis.call('EXPIRE', KEYS[1], ARGV[1])
return items
