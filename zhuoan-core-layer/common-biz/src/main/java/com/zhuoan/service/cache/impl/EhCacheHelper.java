package com.zhuoan.service.cache.impl;

import java.io.Serializable;
import java.util.Collection;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EhCacheHelper {
    private static final Logger logger = LoggerFactory.getLogger(EhCacheHelper.class);
    private static CacheManager manager = null;

    public EhCacheHelper() {
    }

    public static <T extends Serializable> void put(String cacheName, String key, T value) {
        Cache cache = checkCache(cacheName);
        Element e = new Element(key, value);
        cache.put(e);
        cache.flush();
    }

    public static <T extends Serializable> void put(String cacheName, String key, T value, boolean eternal) {
        Cache cache = checkCache(cacheName);
        Element element = new Element(key, value);
        element.setEternal(eternal);
        cache.put(element);
        cache.flush();
    }

    public static <T extends Serializable> void put(String cacheName, String key, T value, int timeToLiveSeconds, int timeToIdleSeconds) {
        Cache cache = checkCache(cacheName);
        Element element = new Element(key, value);
        element.setTimeToLive(timeToLiveSeconds);
        element.setTimeToIdle(timeToIdleSeconds);
        cache.put(element);
        cache.flush();
    }

    public static Object get(String cacheName, String key) {
        Cache cache = checkCache(cacheName);
        Element e = cache.get(key);
        return e != null ? e.getObjectValue() : null;
    }

    public static void remove(String cacheName, String key) {
        Cache cache = checkCache(cacheName);
        cache.remove(key);
    }

    public static void removeAll(String cacheName, Collection<String> keys) {
        Cache cache = checkCache(cacheName);
        cache.removeAll(keys);
    }

    public static void clearAll() {
        manager.clearAll();
    }

    private static Cache checkCache(String cacheName) {
        Cache cache = manager.getCache(cacheName);
        if (null == cache) {
            throw new IllegalArgumentException("不存在Name=[" + cacheName + "]对应的缓存组,请查看ehcache.xml配置");
        } else {
            return cache;
        }
    }

    static {
        try {
            manager = CacheManager.create(EhCacheHelper.class.getClassLoader().getResourceAsStream("ehcache.xml"));
        } catch (CacheException var1) {
            logger.error("获取ehcache.xml失败", var1);
        }

    }
}
