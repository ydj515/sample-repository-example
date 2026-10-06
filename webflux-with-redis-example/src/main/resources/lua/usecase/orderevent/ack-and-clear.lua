local acknowledged = redis.call('XACK', KEYS[1], ARGV[1], ARGV[2])
redis.call('HDEL', KEYS[2], ARGV[2], ARGV[2] .. ':error')
return acknowledged
