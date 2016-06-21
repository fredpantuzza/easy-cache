package com.easycache.core;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;

/**
 * Most basic object that will be managed by the cache.
 * @param <T> Type of entity stored in the cache
 * @author frederico.pantuzza
 */
public class CacheObject<T> {

    private SoftReference<T> entityReference;
    private final ReferenceQueue<T> referenceQueue;

    private final long insertTime;

    public CacheObject(T entity, ReferenceQueue<T> referenceQueue) {
        if (referenceQueue == null) {
            throw new NullPointerException("referenceQueue may not be null");
        }

        this.referenceQueue = referenceQueue;
        this.insertTime = System.currentTimeMillis();
        setEntity(entity);
    }

    final T getEntity(boolean newAccess) {
        if (newAccess) {
            beforeAccessEntity();
        }
        return this.entityReference.get();
    }

    final Reference<T> setEntity(T entity) {
        this.entityReference = new SoftReference<>(entity, this.referenceQueue);
        afterSetEntity();

        return getEntityReference();
    }

    final Reference<T> getEntityReference() {
        return this.entityReference;
    }

    protected void beforeAccessEntity() {
    }

    protected void afterSetEntity() {
    }

    public long getInsertTime() {
        return this.insertTime;
    }
}
