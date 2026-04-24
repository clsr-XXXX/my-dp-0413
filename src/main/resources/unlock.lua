-- 比较锁的值是否与传入的值相同，如果相同则删除锁
if(redis.call('get', KEYS[1]) == ARGV[1]) then
	-- 删除锁
	return redis.call('del', KEYS[1])
else
	return 0
end