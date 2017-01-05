package com.easycache.core.maintainer;

import com.easycache.core.CacheMetadata;
import com.easycache.core.CacheObject;
import com.easycache.core.CacheObjectMaintainer;
import com.easycache.core.Entity;
import com.easycache.core.defaults.DefaultCacheObject;

/**
 * {@link CacheObjectMaintainer} that remove an {@link Entity} that hasn't been accessed for a time.
 * @param <T> Type of {@link Entity} stored in the cache
 */
public class AccessTimeBasedCacheObjectMaintainer<T extends Entity> implements CacheObjectMaintainer<Long, T> {

    private final int maxAccessTimeElapsed;

    /**
     * Constructor.
     * @param maxAccessTimeElapsed Maximum elapsed time since the last access to the entity to remove it from the cache
     */
    public AccessTimeBasedCacheObjectMaintainer(int maxAccessTimeElapsed) {
        this.maxAccessTimeElapsed = maxAccessTimeElapsed;
    }

    @Override
    public boolean isMaintainedByCache(T entity, CacheObject<T> cacheObject, CacheMetadata cacheMetadata) {
        assert cacheObject instanceof DefaultCacheObject;
        DefaultCacheObject<T> defaultCacheObject = (DefaultCacheObject<T>) cacheObject;

        int elapsedTime = (int) (System.currentTimeMillis() - defaultCacheObject.getLastAccessTime());
        return elapsedTime < this.maxAccessTimeElapsed;
    }
}
