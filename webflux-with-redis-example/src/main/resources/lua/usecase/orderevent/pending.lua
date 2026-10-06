-- 커서 경계를 제외하고 pending 메타데이터만 조회한다.
return cjson.encode(redis.call('XPENDING', KEYS[1], ARGV[1], ARGV[2], '+', ARGV[3]))
