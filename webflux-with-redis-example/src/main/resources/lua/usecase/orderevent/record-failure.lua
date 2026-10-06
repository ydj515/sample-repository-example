-- 소유권을 확인하고 처리 실패 횟수를 기록한다.
local pending = redis.call('XPENDING', KEYS[1], ARGV[1], ARGV[2], ARGV[2], 1)
if #pending == 0 then return {'NOT_PENDING', '0', '', '0'} end
if pending[1][2] ~= ARGV[3] then return {'OWNERSHIP_CHANGED', '0', '', '0'} end
local attempts = tonumber(redis.call('HGET', KEYS[2], ARGV[2]) or '0')
if attempts < tonumber(ARGV[4]) then
    attempts = redis.call('HINCRBY', KEYS[2], ARGV[2], 1)
    redis.call('HSET', KEYS[2], ARGV[2] .. ':error', ARGV[5])
end
local errorType = redis.call('HGET', KEYS[2], ARGV[2] .. ':error') or ARGV[5]
if attempts < tonumber(ARGV[4]) then
    return {'RETRY_PENDING', tostring(attempts), '', '0'}
end
local records = redis.call('XRANGE', KEYS[1], ARGV[2], ARGV[2])
if #records == 0 then return redis.error_reply('Source record missing') end
local payload = ''
for i = 1, #records[1][2], 2 do
    if records[1][2][i] == 'event' then payload = records[1][2][i + 1] end
end
-- XADD에 실패하면 XACK를 수행하지 않아 원본이 pending에 남는다.
local dlqId = redis.call('XADD', KEYS[3], '*',
    'sourceStream', KEYS[1], 'group', ARGV[1], 'originalRecordId', ARGV[2],
    'event', payload, 'attempts', tostring(attempts), 'errorType', errorType)
local acknowledged = redis.call('XACK', KEYS[1], ARGV[1], ARGV[2])
redis.call('HDEL', KEYS[2], ARGV[2], ARGV[2] .. ':error')
return {'DEAD_LETTERED', tostring(attempts), dlqId, tostring(acknowledged)}
