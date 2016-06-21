package com.easycache.core;

/**
 * Responsible for decide whether an object stays on cache or not.
 * @param <K> Cache's key type
 * @param <T> Cache's object type
 */
public interface CacheObjectMaintainer<K, T> {

    /**
     * Determines whether an object stays on cache or not.
     * <p>
     * <b>Careful!</b> This function should not be too time expensive since it will be called several times by the
     * cache.
     *
     * @param entity Entity being evaluated
     * @param cacheObject The object that is actually maintained by the cache
     * @param cacheMetadata Metadata from the cache
     * @return <code>true</code> if it must stay on cache
     * @see CacheObjectFactory
     */
    boolean isMaintainedByCache(T entity, CacheObject<T> cacheObject, CacheMetadata cacheMetadata);
}
