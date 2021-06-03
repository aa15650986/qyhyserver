package com.zhuoan.service.cache;

import java.util.Map;
import java.util.Set;

public interface RedisService {
    void insertKey(String var1, String var2, Long var3);

    void deleteByKey(String var1);

    void deleteByLikeKey(String var1);

    Object queryValueByKey(String var1);

    boolean expire(String var1, long var2);

    long incr(String var1, long var2);

    long decr(String var1, long var2);

    boolean hset(String var1, String var2, Object var3);

    boolean hset(String var1, String var2, Object var3, long var4);

    Map<Object, Object> hmget(String var1);

    Object hget(String var1, String var2);

    void hdel(String var1, Object... var2);

    boolean hmset(String var1, Map<Object, Object> var2);

    boolean sHasKey(String var1, Object var2);

    boolean hasKey(String var1);

    long sSet(String var1, Object... var2);

    long sSetAndTime(String var1, long var2, Object... var4);

    long setRemove(String var1, Object... var2);

    Set<Object> sGet(String var1);
}
