-- Atomically checks if bot count is below cap, increments if allowed.
-- KEYS[1] : the bot_count key for the post
-- ARGV[1] : the cap value (e.g., "100")
-- Returns : 1 if reservation succeeded, 0 if cap reached

local key = KEYS[1]
local cap = tonumber(ARGV[1])
local current = tonumber(redis.call('GET', key) or '0')

if current >= cap then
    return 0
end

redis.call('INCR', key)
return 1
