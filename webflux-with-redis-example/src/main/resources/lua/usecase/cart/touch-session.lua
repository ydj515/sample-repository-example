local user = redis.call('HGET', KEYS[1], 'userId')
if not user then return nil end
redis.call('EXPIRE', KEYS[1], ARGV[1])
return user
