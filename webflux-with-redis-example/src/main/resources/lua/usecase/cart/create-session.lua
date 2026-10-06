redis.call('HSET', KEYS[1], 'userId', ARGV[1])
redis.call('EXPIRE', KEYS[1], ARGV[2])
return 1
