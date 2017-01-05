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

    /**
     * Constructor.
     * @param entity Initial entity
     * @param referenceQueue (mandatory) The {@link ReferenceQueue} where the references will be queued after collected
     */
    public CacheObject(T entity, ReferenceQueue<T> referenceQueue) {
        if (referenceQueue == null) {
            throw new NullPointerException("referenceQueue may not be null");
        }

        this.referenceQueue = referenceQueue;
        this.insertTime = System.currentTimeMillis();
        setEntity(entity);
    }

    /**
     * Gets the entity this {@link CacheObject} holds.
     * @param newAccess <code>true</code> if this call must count as a new access
     * @return The held entity. Can be <code>null</code>
     */
    final T getEntity(boolean newAccess) {
        if (newAccess) {
            beforeAccessEntity();
        }
        return this.entityReference.get();
    }

    /**
     * Updates the entity held by this {@link CacheObject}.
     * @param entity The new entity
     * @return The {@link Reference} created to the new entity
     */
    final Reference<T> setEntity(T entity) {
        this.entityReference = new SoftReference<>(entity, this.referenceQueue);
        afterSetEntity();

        return getEntityReference();
    }

    /**
     * Get the {@link Reference} for the entity this cache holds.
     * @return The {@link Reference} for the entity this cache holds
     */
    final Reference<T> getEntityReference() {
        return this.entityReference;
    }

    /**
     * Method called before every access to the entity.
     */
    protected void beforeAccessEntity() {
    }

    /**
     * Method called after every update to the entity.
     */
    protected void afterSetEntity() {
    }

    /**
     * Gets the time in milliseconds when this {@link CacheObject} was inserted to the cache.
     * @return The time in milliseconds when this {@link CacheObject} was inserted to the cache
     * @see System#currentTimeMillis()
     */
    public long getInsertTime() {
        return this.insertTime;
    }
}
