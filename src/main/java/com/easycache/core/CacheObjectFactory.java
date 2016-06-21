package com.easycache.core;

import java.lang.ref.ReferenceQueue;

/**
 * Factory of {@link CacheObject}.
 * @param <T> Type of entity stored by the {@link CacheObject}
 * @author frederico.pantuzza
 */
public interface CacheObjectFactory<T> {

    /**
     * Creates a new {@link CacheObject}.
     * @param entity Entity that is going to be initially stored
     * @param referenceQueue {@link ReferenceQueue} from the cache
     * @return The created {@link CacheObject}
     */
    CacheObject<T> newCacheObject(T entity, ReferenceQueue<T> referenceQueue);
}
