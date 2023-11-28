---
--- Created by zcx.
--- DateTime: 2023/11/28 15:44
---

-- 锁的key
local key = KEYS[1]
-- 锁的value，即线程标识
local threadId = ARGV[1]

local id = redis.call('GET', key)

if(id == threadId) then
    -- 锁中的标识和当前的线程标识一致，释放锁
    return redis.call('DEL', key)
end
return 0