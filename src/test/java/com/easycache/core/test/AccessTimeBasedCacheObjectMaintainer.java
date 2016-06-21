package com.easycache.core.test;

import com.easycache.core.CacheMetadata;
import com.easycache.core.CacheObject;
import com.easycache.core.CacheObjectMaintainer;
import com.easycache.core.Entity;
import com.easycache.core.defaults.DefaultCacheObject;

/**
 * {@link CacheObjectMaintainer} that removes entities that hasn't been accessed
 * for a time.
 */
public class AccessTimeBasedCacheObjectMaintainer implements CacheObjectMaintainer<Long, Entity> {

    private final int maxAccessTimeElapsed;

    public AccessTimeBasedCacheObjectMaintainer(int maxAccessTimeElapsed) {
        this.maxAccessTimeElapsed = maxAccessTimeElapsed;
    }

    @Override
    public boolean isMaintainedByCache(Entity entity, CacheObject<Entity> cacheObject, CacheMetadata cacheMetadata) {
        int elapsedTime = (int) (System.currentTimeMillis() - ((DefaultCacheObject<Entity>) cacheObject).getLastAccessTime());
        return elapsedTime < this.maxAccessTimeElapsed;
    }
}
