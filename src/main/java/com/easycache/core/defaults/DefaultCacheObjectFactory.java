package com.easycache.core.defaults;

import java.lang.ref.ReferenceQueue;

import com.easycache.core.CacheObject;
import com.easycache.core.CacheObjectFactory;

public class DefaultCacheObjectFactory<T> implements CacheObjectFactory<T> {

    @Override
    public CacheObject<T> newCacheObject(T entity, ReferenceQueue<T> referenceQueue) {
        return new DefaultCacheObject<>(entity, referenceQueue);
    }
}
