
package com.zhuoan.service.cache.impl;

import com.zhuoan.service.cache.RedisService;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisConnectionUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class RedisServiceImpl implements RedisService {
    private static final Logger logger = LoggerFactory.getLogger(RedisServiceImpl.class);
    @Resource
    private RedisTemplate redisTemplate;

    public RedisServiceImpl() {
    }

    public long getExpire(String key) {
        return this.redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    public boolean hasKey(String key) {
        boolean var3;
        try {
            boolean var2 = this.redisTemplate.hasKey(key);
            return var2;
        } catch (Exception var7) {
            logger.error("", var7);
            var3 = false;
        } finally {
            this.close();
        }

        return var3;
    }

    public void del(String... key) {
        try {
            if (key != null && key.length > 0) {
                if (key.length == 1) {
                    this.redisTemplate.delete(key[0]);
                } else {
                    this.redisTemplate.delete(CollectionUtils.arrayToList(key));
                }
            }
        } catch (Exception var6) {
            logger.error("", var6);
        } finally {
            this.close();
        }

    }

    public Object get(String key) {
        Object var3;
        try {
            Object var2 = key == null ? null : this.redisTemplate.opsForValue().get(key);
            return var2;
        } catch (Exception var7) {
            logger.error("", var7);
            var3 = null;
        } finally {
            this.close();
        }

        return var3;
    }

    public boolean set(String key, Object value) {
        boolean var4;
        try {
            this.redisTemplate.opsForValue().set(key, value);
            boolean var3 = true;
            return var3;
        } catch (Exception var8) {
            logger.error("", var8);
            var4 = false;
        } finally {
            this.close();
        }

        return var4;
    }

    public boolean set(String key, Object value, long time) {
        boolean var6;
        try {
            if (time > 0L) {
                this.redisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS);
            } else {
                this.set(key, value);
            }

            boolean var5 = true;
            return var5;
        } catch (Exception var10) {
            logger.error("", var10);
            var6 = false;
        } finally {
            this.close();
        }

        return var6;
    }

    public long incr(String key, long delta) {
        long var5;
        try {
            if (delta < 0L) {
                throw new RuntimeException("递增因子必须大于0");
            }

            long var4 = this.redisTemplate.opsForValue().increment(key, delta);
            return var4;
        } catch (Exception var10) {
            logger.error("", var10);
            var5 = 0L;
        } finally {
            this.close();
        }

        return var5;
    }

    public long decr(String key, long delta) {
        long var5;
        try {
            if (delta < 0L) {
                throw new RuntimeException("递减因子必须大于0");
            }

            long var4 = this.redisTemplate.opsForValue().increment(key, -delta);
            return var4;
        } catch (Exception var10) {
            logger.error("", var10);
            var5 = 0L;
        } finally {
            this.close();
        }

        return var5;
    }

    public Object hget(String key, String item) {
        Object var4;
        try {
            Object var3 = this.redisTemplate.opsForHash().get(key, item);
            return var3;
        } catch (Exception var8) {
            logger.error("", var8);
            var4 = null;
        } finally {
            this.close();
        }

        return var4;
    }

    public Map<Object, Object> hmget(String key) {
        Object var3;
        try {
            Map var2 = this.redisTemplate.opsForHash().entries(key);
            return var2;
        } catch (Exception var7) {
            logger.error("", var7);
            var3 = null;
        } finally {
            this.close();
        }

        return (Map)var3;
    }

    public boolean hmset(String key, Map<Object, Object> map) {
        boolean var4;
        try {
            this.redisTemplate.opsForHash().putAll(key, map);
            boolean var3 = true;
            return var3;
        } catch (Exception var8) {
            logger.error("", var8);
            var4 = false;
        } finally {
            this.close();
        }

        return var4;
    }

    public boolean hmset(String key, Map<String, Object> map, long time) {
        boolean var6;
        try {
            this.redisTemplate.opsForHash().putAll(key, map);
            if (time > 0L) {
                this.expire(key, time);
            }

            boolean var5 = true;
            return var5;
        } catch (Exception var10) {
            logger.error("", var10);
            var6 = false;
        } finally {
            this.close();
        }

        return var6;
    }

    public boolean hset(String key, String item, Object value) {
        boolean var5;
        try {
            this.redisTemplate.opsForHash().put(key, item, value);
            boolean var4 = true;
            return var4;
        } catch (Exception var9) {
            logger.error("向Redis中添加数据发生异常key=[" + key + "]", var9);
            var5 = false;
        } finally {
            this.close();
        }

        return var5;
    }

    public boolean hset(String key, String item, Object value, long time) {
        boolean var7;
        try {
            this.redisTemplate.opsForHash().put(key, item, value);
            if (time > 0L) {
                this.expire(key, time);
            }

            boolean var6 = true;
            return var6;
        } catch (Exception var11) {
            logger.error("", var11);
            var7 = false;
        } finally {
            this.close();
        }

        return var7;
    }

    public void hdel(String key, Object... item) {
        try {
            this.redisTemplate.opsForHash().delete(key, item);
        } catch (Exception var7) {
            logger.error("", var7);
        } finally {
            this.close();
        }

    }

    public boolean hHasKey(String key, String item) {
        boolean var4;
        try {
            boolean var3 = this.redisTemplate.opsForHash().hasKey(key, item);
            return var3;
        } catch (Exception var8) {
            logger.error("", var8);
            var4 = false;
        } finally {
            this.close();
        }

        return var4;
    }

    public double hincr(String key, String item, double by) {
        double var6;
        try {
            double var5 = this.redisTemplate.opsForHash().increment(key, item, by);
            return var5;
        } catch (Exception var11) {
            logger.error("", var11);
            var6 = 0.0D;
        } finally {
            this.close();
        }

        return var6;
    }

    public double hdecr(String key, String item, double by) {
        double var6;
        try {
            double var5 = this.redisTemplate.opsForHash().increment(key, item, -by);
            return var5;
        } catch (Exception var11) {
            logger.error("", var11);
            var6 = 0.0D;
        } finally {
            this.close();
        }

        return var6;
    }

    public Set<Object> sGet(String key) {
        Object var3;
        try {
            Set var2 = this.redisTemplate.opsForSet().members(key);
            return var2;
        } catch (Exception var7) {
            logger.error("", var7);
            var3 = null;
        } finally {
            this.close();
        }

        return (Set)var3;
    }

    public boolean sHasKey(String key, Object value) {
        boolean var4;
        try {
            boolean var3 = this.redisTemplate.opsForSet().isMember(key, value);
            return var3;
        } catch (Exception var8) {
            logger.error("", var8);
            var4 = false;
        } finally {
            this.close();
        }

        return var4;
    }

    public long sSet(String key, Object... values) {
        long var4;
        try {
            long var3 = this.redisTemplate.opsForSet().add(key, values);
            return var3;
        } catch (Exception var9) {
            logger.error("", var9);
            var4 = 0L;
        } finally {
            this.close();
        }

        return var4;
    }

    public long sSetAndTime(String key, long time, Object... values) {
        long var6;
        try {
            Long count = this.redisTemplate.opsForSet().add(key, values);
            if (time > 0L) {
                this.expire(key, time);
            }

            var6 = count;
            return var6;
        } catch (Exception var11) {
            logger.error("", var11);
            var6 = 0L;
        } finally {
            this.close();
        }

        return var6;
    }

    public long sGetSetSize(String key) {
        long var3;
        try {
            long var2 = this.redisTemplate.opsForSet().size(key);
            return var2;
        } catch (Exception var8) {
            logger.error("", var8);
            var3 = 0L;
        } finally {
            this.close();
        }

        return var3;
    }

    public long setRemove(String key, Object... values) {
        long var4;
        try {
            Long count = this.redisTemplate.opsForSet().remove(key, values);
            var4 = count;
            return var4;
        } catch (Exception var9) {
            logger.error("", var9);
            var4 = 0L;
        } finally {
            this.close();
        }

        return var4;
    }

    public List<Object> lGet(String key, long start, long end) {
        Object var7;
        try {
            List var6 = this.redisTemplate.opsForList().range(key, start, end);
            return var6;
        } catch (Exception var11) {
            logger.error("", var11);
            var7 = null;
        } finally {
            this.close();
        }

        return (List)var7;
    }

    public long lGetListSize(String key) {
        long var3;
        try {
            long var2 = this.redisTemplate.opsForList().size(key);
            return var2;
        } catch (Exception var8) {
            logger.error("从Redis中获取指定key数据key=[" + key + "]", var8);
            var3 = 0L;
        } finally {
            this.close();
        }

        return var3;
    }

    public Object lGetIndex(String key, long index) {
        Object var5;
        try {
            Object var4 = this.redisTemplate.opsForList().index(key, index);
            return var4;
        } catch (Exception var9) {
            logger.error("", var9);
            var5 = null;
        } finally {
            this.close();
        }

        return var5;
    }

    public boolean lSet(String key, Object value) {
        boolean var4;
        try {
            this.redisTemplate.opsForList().rightPush(key, value);
            boolean var3 = true;
            return var3;
        } catch (Exception var8) {
            logger.error("", var8);
            var4 = false;
        } finally {
            this.close();
        }

        return var4;
    }

    public boolean lSet(String key, Object value, long time) {
        boolean var6;
        try {
            this.redisTemplate.opsForList().rightPush(key, value);
            if (time > 0L) {
                this.expire(key, time);
            }

            boolean var5 = true;
            return var5;
        } catch (Exception var10) {
            logger.error("向Redis中添加数据发生异常key=[" + key + "]", var10);
            var6 = false;
        } finally {
            this.close();
        }

        return var6;
    }

    public boolean lSet(String key, List<Object> value) {
        boolean var4;
        try {
            this.redisTemplate.opsForList().rightPushAll(key, value);
            boolean var3 = true;
            return var3;
        } catch (Exception var8) {
            logger.error("", var8);
            var4 = false;
        } finally {
            this.close();
        }

        return var4;
    }

    public boolean lSet(String key, List<Object> value, long time) {
        boolean var6;
        try {
            this.redisTemplate.opsForList().rightPushAll(key, value);
            if (time > 0L) {
                this.expire(key, time);
            }

            boolean var5 = true;
            return var5;
        } catch (Exception var10) {
            logger.error("", var10);
            var6 = false;
        } finally {
            this.close();
        }

        return var6;
    }

    public boolean lUpdateIndex(String key, long index, Object value) {
        boolean var6;
        try {
            this.redisTemplate.opsForList().set(key, index, value);
            boolean var5 = true;
            return var5;
        } catch (Exception var10) {
            logger.error("", var10);
            var6 = false;
        } finally {
            this.close();
        }

        return var6;
    }

    public long lRemove(String key, long count, Object value) {
        long var6;
        try {
            Long remove = this.redisTemplate.opsForList().remove(key, count, value);
            var6 = remove;
            return var6;
        } catch (Exception var11) {
            logger.error("", var11);
            var6 = 0L;
        } finally {
            this.close();
        }

        return var6;
    }

    public void insertKey(String key, String value, Long timeout) {
        try {
            if (null != timeout) {
                this.redisTemplate.opsForValue().set(key, value, timeout, TimeUnit.SECONDS);
            } else {
                this.redisTemplate.opsForValue().set(key, value);
            }
        } catch (Exception var8) {
            logger.error("向Redis中添加数据发生异常key=[" + key + "]", var8);
        } finally {
            this.close();
        }

    }

    public void deleteByKey(String key) {
        try {
            Boolean exists = this.redisTemplate.hasKey(key);
            if (exists) {
                this.redisTemplate.delete(key);
                return;
            }
        } catch (Exception var6) {
            logger.error("从Redis中删除指定key数据key=[" + key + "],发生异常", var6);
            return;
        } finally {
            this.close();
        }

    }

    public void deleteByLikeKey(String key) {
        try {
            Set<String> keys = this.redisTemplate.keys(key);
            Iterator var3 = keys.iterator();

            while(var3.hasNext()) {
                String s = (String)var3.next();
                this.redisTemplate.delete(s);
            }
        } catch (Exception var8) {
            logger.error("从Redis中删除指定key数据key=[" + key + "],发生异常", var8);
        } finally {
            this.close();
        }

    }

    public Object queryValueByKey(String key) {
        Object var3;
        try {
            Boolean exists = this.redisTemplate.hasKey(key);
            if (!exists) {
                var3 = null;
                return var3;
            }

            var3 = this.redisTemplate.opsForValue().get(key);
        } catch (Exception var7) {
            logger.error("从Redis中获取指定key数据key=[" + key + "]发生异常", var7);
            var3 = null;
            return var3;
        } finally {
            this.close();
        }

        return var3;
    }

    public boolean expire(String key, long seconds) {
        try {
            if (seconds > 0L) {
                this.redisTemplate.expire(key, seconds, TimeUnit.SECONDS);
            }

            boolean var4 = true;
            return var4;
        } catch (Exception var8) {
            logger.info("对指定的key=[" + key + "]设置时间[" + seconds + "s]发生异常", var8);
        } finally {
            this.close();
        }

        return false;
    }

    private void close() {
        RedisConnectionUtils.unbindConnection(this.redisTemplate.getConnectionFactory());
    }
}
