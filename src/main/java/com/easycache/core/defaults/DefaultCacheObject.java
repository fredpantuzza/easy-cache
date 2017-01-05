package com.easycache.core.defaults;

import java.lang.ref.ReferenceQueue;

import com.easycache.core.CacheObject;

/**
 * Default implementation of a {@link CacheObject} with some useful statistics.
 * @param <T> Type of entity stored in the cache
 * @author frederico.pantuzza
 */
public class DefaultCacheObject<T> extends CacheObject<T> {

    private long accesses;
    private long lastUpdateTime;
    private long lastAccessTime;

    /**
     * Constructor.
     * @param entity See {@link CacheObject#CacheObject(Object, ReferenceQueue)}
     * @param referenceQueue See {@link CacheObject#CacheObject(Object, ReferenceQueue)}
     */
    public DefaultCacheObject(T entity, ReferenceQueue<T> referenceQueue) {
        super(entity, referenceQueue);
    }

    @Override
    protected void beforeAccessEntity() {
        this.lastAccessTime = System.currentTimeMillis();
        this.accesses++;
    }

    @Override
    protected void afterSetEntity() {
        this.lastUpdateTime = System.currentTimeMillis();
    }

    /**
     * Gets the number of access to this entity so far.
     * @return The number of access to this entity so far
     */
    public long getAccesses() {
        return this.accesses;
    }

    /**
     * Gets the last update time to this entity, in milliseconds.
     * @return The last update time to this entity, in milliseconds
     * @see System#currentTimeMillis()
     */
    public long getLastUpdateTime() {
        return this.lastUpdateTime;
    }

    /**
     * Gets the last access time to this entity, in milliseconds.
     * @return The last access time to this entity, in milliseconds
     * @see System#currentTimeMillis()
     */
    public long getLastAccessTime() {
        return this.lastAccessTime;
    }
}
