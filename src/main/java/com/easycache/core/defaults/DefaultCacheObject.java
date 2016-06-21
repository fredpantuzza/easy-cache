package com.easycache.core.defaults;

import java.lang.ref.ReferenceQueue;

import com.easycache.core.CacheObject;

public class DefaultCacheObject<T> extends CacheObject<T> {

    private long accesses;
    private long lastUpdateTime;
    private long lastAccessTime;

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

    public long getAccesses() {
        return this.accesses;
    }

    public long getLastUpdateTime() {
        return this.lastUpdateTime;
    }

    public long getLastAccessTime() {
        return this.lastAccessTime;
    }
}
