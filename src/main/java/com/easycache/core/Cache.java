package com.easycache.core;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import com.easycache.core.defaults.DefaultCacheObject;
import com.easycache.core.defaults.DefaultCacheObjectFactory;
import com.googlecode.concurentlocks.ReadWriteUpdateLock;
import com.googlecode.concurentlocks.ReentrantReadWriteUpdateLock;

/**
 * Thread-safe cache implementation. It only supports entities that can be identified by an <b>unique</b> key.
 * <p>
 * All entities are added to the cache by a {@link CacheLoader}, which is triggered whenever
 * {@link Cache#refresh(Object)} is called. This method might be called directly or inside {@link Cache#get(Object)}
 * method, depending on the configured {@link CacheMissBehaviour}.
 * <p>
 * Once loaded into cache, the entity will be referred by an {@link CacheObject} (with a {@link SoftReference}). This is
 * the class that is actually managed by the cache. Besides the stored entity, it may contain metadata relevant to the
 * cache implementation, for example, the last access time or the number of accesses to an object. By default, this
 * cache stores instances of {@link DefaultCacheObject}, a type that already saves some metadata about the object. You
 * may want to use another type of {@link CacheObject}, just set your own {@link CacheObjectFactory} when constructing
 * the cache.
 * <p>
 * An entity may be removed from the cache in two different ways. One happens when {@link Cache#cleanup()} is called. In
 * this case, the defined {@link CacheObjectMaintainer} will be triggered for every cached entity to decide whether it
 * stays on cache or not. This method may be periodically called by an clean up thread automatically created by the
 * cache, if {@link #cleanupInterval} is defined, or you may want to call it manually at any time.
 * <p>
 * The other way is by the garbage collector. Since this cache does not hold any strong references to the cached
 * objects, the GC may decide to collect them at any time. When this happens, the {@link CacheObject} that was holding
 * the reference to the collected object will be removed from the cache by a special thread called
 * "References cache cleanup". Notice that even before it gets removed, the object will not be retrievable by the cache
 * (since it was collected). If you want to prevent an entity to be collected when cached, simply create a strong
 * reference to it. You could use this to, for example, guarantee that the most recently used objects will never be
 * removed.
 * @param <K> Type of the unique identifier for the cache entities
 * @param <T> Type of the cache entity
 * @author frederico.pantuzza
 */
public class Cache<K, T> implements CacheMetadata {

    /** Default value for {@link #cleanupInterval}. */
    private static final long DEFAULT_CLEANUP_INTERVAL = TimeUnit.SECONDS.toMillis(30L);
    /** Default value for {@link #cacheMissBehaviour}. */
    private static final CacheMissBehaviour DEFAULT_CACHE_MISS_BEHAVIOUR = CacheMissBehaviour.LOAD_WHENEVER_NOT_AVAILABLE_BEFORE;

    /** {@link Map} that holds this cache's entities. */
    private final Map<K, CacheObject<T>> entitiesMap = new HashMap<>();

    /**
     * {@link Map} that holds the reference to the cached entities and their respective keys. It is used to identify the
     * {@link CacheObject}s which the entity have already been collected.
     */
    private final Map<Reference<T>, K> keysByEntityReferenceMap = new HashMap<>();
    private final ReferenceQueue<T> referenceQueue = new ReferenceQueue<>();

    /** The lock that will be used to synchronize this cache operations. */
    private final ReadWriteUpdateLock lock = new ReentrantReadWriteUpdateLock();

    /** The object's factory. */
    private final CacheObjectFactory<T> cacheObjectFactory;

    /** The object's maintainer. */
    private final CacheObjectMaintainer<K, T> cacheObjectMaintainer;

    /** The object's loader. */
    private final CacheLoader<K, T> cacheLoader;

    /**
     * Interval (in milliseconds) between automatic calls to {@link #cleanup()}. <code>null</code> if calls should not
     * be done automatically.
     */
    private Long cleanupInterval = DEFAULT_CLEANUP_INTERVAL;

    /** @see CacheMissBehaviour */
    private CacheMissBehaviour cacheMissBehaviour = DEFAULT_CACHE_MISS_BEHAVIOUR;

    private Thread cleanupThread;
    private Thread referencesCleanupThread;

    private boolean running;

    /**
     * Same as {@link #Cache(CacheObjectFactory, CacheLoader, CacheObjectMaintainer)}, but uses the
     * {@link DefaultCacheObjectFactory}.
     * @param cacheLoader (mandatory) see {@link #cacheLoader}
     * @param cacheObjectMaintainer (mandatory) see {@link #cacheObjectMaintainer}
     * @throws IllegalArgumentException if any of the mandatory parameters is <code>null</code>
     * @see CacheObjectMaintainer
     */
    public Cache(CacheLoader<K, T> cacheLoader, CacheObjectMaintainer<K, T> cacheObjectManager) {
        this(new DefaultCacheObjectFactory<>(), cacheLoader, cacheObjectManager);
    }

    /**
     * Constructor.
     * @param cacheObjectFactory (mandatory) see {@link #cacheObjectFactory}
     * @param cacheLoader (mandatory) see {@link #cacheLoader}
     * @param cacheObjectMaintainer (mandatory) see {@link #cacheObjectMaintainer}
     * @throws IllegalArgumentException if any of the mandatory parameters is <code>null</code>
     * @see CacheObjectMaintainer
     */
    public Cache(CacheObjectFactory<T> cacheObjectFactory, CacheLoader<K, T> cacheLoader,
            CacheObjectMaintainer<K, T> cacheObjectManager) throws IllegalArgumentException {
        if (cacheObjectFactory == null || cacheLoader == null || cacheObjectManager == null) {
            throw new IllegalArgumentException(
                    "Neither cacheObjectFactory, cacheLoader nor cacheObjectMaintainer can be null");
        }
        this.cacheObjectFactory = cacheObjectFactory;
        this.cacheLoader = cacheLoader;
        this.cacheObjectMaintainer = cacheObjectManager;
    }

    /**
     * @return the {@link #cleanupInterval}
     */
    public Long getCleanupInterval() {
        return this.cleanupInterval;
    }

    /**
     * Sets the {@link #cleanupInterval}.
     * <p>
     * Can only be called when the cache is not running.
     * @param cleanupInterval new value for {@link #cleanupInterval}. Must be greater than zero
     * @throws IllegalArgumentException if <code>cleanupInterval</code> is less or equal to zero
     */
    public void setCleanupInterval(Long cleanupInterval) throws IllegalArgumentException {
        this.lock.writeLock().lock();
        try {
            checkNotRunning();

            if (cleanupInterval != null && cleanupInterval <= 0) {
                throw new IllegalArgumentException("cleanupInterval must either be null or greater than zero");
            }
            this.cleanupInterval = cleanupInterval;

        } finally {
            this.lock.writeLock().unlock();
        }
    }

    /**
     * @return the {@link #cacheMissBehaviour}
     */
    public CacheMissBehaviour getCacheMissBehaviour() {
        return this.cacheMissBehaviour;
    }

    /**
     * Sets the {@link #cacheMissBehaviour}.
     * <p>
     * Can only be called when the cache is not running.
     * @param cacheMissBehaviour new value for {@link #cacheMissBehaviour}. Must not be <code>null</code>
     * @throws IllegalArgumentException if <code>cacheMissBehaviour</code> is <code>null</code>
     */
    public void setCacheMissBehaviour(CacheMissBehaviour cacheMissBehaviour) throws IllegalArgumentException {
        this.lock.writeLock().lock();
        try {
            checkNotRunning();

            if (cacheMissBehaviour == null) {
                throw new IllegalArgumentException("cacheMissBehaviour must not be null");
            }
            this.cacheMissBehaviour = cacheMissBehaviour;

        } finally {
            this.lock.writeLock().unlock();
        }
    }

    /**
     * Starts this cache activity.
     * <p>
     * When called, this method may start at most two threads. One will be started only if {@link #cleanupInterval} is
     * defined and call {@link #cleanup()} periodically. The other will always be started to clean any useless
     * references left by the garbage collector. Both threads will stop whenever {@link #stop()} is called.
     */
    public void start() {
        this.lock.writeLock().lock();
        try {
            checkNotRunning();
            this.running = true;

            /* Starts a cleanup thread, if required. */
            if (this.cleanupInterval != null) {
                this.cleanupThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            while (!Thread.interrupted()) {
                                Cache.this.lock.writeLock().lock();
                                try {
                                    /* It could have been interrupted while waiting for the lock. */
                                    if (Thread.interrupted()) {
                                        break;
                                    }

                                    doCleanup();
                                } finally {
                                    Cache.this.lock.writeLock().unlock();
                                }

                                assert Cache.this.cleanupInterval != null;
                                Thread.sleep(Cache.this.cleanupInterval);
                            }
                        } catch (InterruptedException e) {
                            /* Allow thread to exit. */
                        }
                    }
                }, "Cache cleanup");
                this.cleanupThread.setPriority(Thread.MIN_PRIORITY);
                this.cleanupThread.start();
            }

            /* Starts a reference cleanup thread (always). */
            this.referencesCleanupThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (!Thread.interrupted()) {
                            Reference<? extends T> removed = Cache.this.referenceQueue.remove();

                            Cache.this.lock.writeLock().lock();
                            try {
                                /* It could have been interrupted while waiting for the lock. */
                                if (Thread.interrupted()) {
                                    break;
                                }

                                K key = Cache.this.keysByEntityReferenceMap.get(removed);
                                if (key != null) {
                                    Cache.this.entitiesMap.remove(key);
                                }
                            } finally {
                                Cache.this.lock.writeLock().unlock();
                            }
                        }
                    } catch (InterruptedException e) {
                        /* Allow thread to exit. */
                    }
                }
            }, "References cache cleanup");
            this.referencesCleanupThread.start();

        } finally {
            this.lock.writeLock().unlock();
        }
    }

    /**
     * Stops this cache activity.
     * <p>
     * Whatever extra thread started by this cache implementation will not be waited to stop here in order to simplify
     * the lock operations. Anyway, it is guaranteed that after exiting this method, no operation will be executed by
     * any of the created threads.
     */
    public void stop() {
        this.lock.writeLock().lock();
        try {
            checkRunning();
            this.running = false;

            /*
             * Once interrupted, we are positive that the threads will stop. However, we can't call join because we are
             * holding the lock.
             */
            this.cleanupThread.interrupt();
            this.referencesCleanupThread.interrupt();

            this.keysByEntityReferenceMap.clear();
            this.entitiesMap.clear();

        } finally {
            this.lock.writeLock().unlock();
        }
    }

    /**
     * Gets an entity from the cache by its key.
     * @param key Key of the desired entity
     * @throws Exception if there was an error when loading a new entity
     * @see CacheLoader
     * @see CacheMissBehaviour
     */
    public T get(K key) throws Exception {
        this.lock.updateLock().lock();
        try {
            checkRunning();

            CacheObject<T> cacheObject = this.entitiesMap.get(key);
            if (cacheObject != null) {
                T entity = cacheObject.getEntity(true);
                if (entity != null) {
                    /*
                     * Guarantees that only maintained entities are returned. Otherwise, it will be removed on the next
                     * cleanup.
                     */
                    if (this.cacheObjectMaintainer.isMaintainedByCache(entity, cacheObject, this)) {
                        return entity;
                    }

                } else if (this.cacheMissBehaviour == CacheMissBehaviour.LOAD_WHENEVER_NOT_AVAILABLE) {
                    /* It might have been garbage collected. */
                    return refresh(key);
                }

            } else if (this.cacheMissBehaviour == CacheMissBehaviour.LOAD_WHENEVER_NOT_AVAILABLE
                    || this.cacheMissBehaviour == CacheMissBehaviour.LOAD_WHENEVER_NOT_AVAILABLE_BEFORE) {
                return refresh(key);
            }
            return null;

        } finally {
            this.lock.updateLock().unlock();
        }
    }

    /**
     * Refresh an entity (or insert it, if not on cache already).
     * @param key Key of the entity to refresh
     * @return refreshed entity
     * @throws Exception if there was an error when loading a new entity
     * @see CacheLoader
     */
    public T refresh(K key) throws Exception {
        T loadedEntity = this.cacheLoader.load(key);

        this.lock.writeLock().lock();
        try {
            checkRunning();

            if (this.entitiesMap.containsKey(key)) {
                CacheObject<T> c = this.entitiesMap.get(key);

                this.keysByEntityReferenceMap.remove(c.getEntityReference());
                Reference<T> newReference = c.setEntity(loadedEntity);
                this.keysByEntityReferenceMap.put(newReference, key);

            } else {
                CacheObject<T> c = this.cacheObjectFactory.newCacheObject(loadedEntity, this.referenceQueue);
                this.entitiesMap.put(key, c);
                this.keysByEntityReferenceMap.put(c.getEntityReference(), key);
            }
            return loadedEntity;

        } finally {
            this.lock.writeLock().lock();
        }
    }

    /**
     * Remove all the entities that are no longer maintained by the cache.
     * @see CacheObjectMaintainer
     */
    public void cleanup() {
        this.lock.writeLock().lock();
        try {
            checkRunning();

            doCleanup();
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    @Override
    public int size() {
        this.lock.writeLock().lock();
        try {
            checkRunning();

            doCleanup();
            return this.entitiesMap.size();
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    private void doCleanup() {
        /* Must be write-locked here (unfortunately, there is no graceful way to assert this). */

        for (Iterator<Entry<K, CacheObject<T>>> it = this.entitiesMap.entrySet().iterator(); it.hasNext();) {
            CacheObject<T> c = it.next().getValue();

            T entity = c.getEntity(false);
            if (entity == null || !this.cacheObjectMaintainer.isMaintainedByCache(entity, c, this)) {
                it.remove();
            }
        }
    }

    private void checkRunning() {
        if (!this.running) {
            throw new IllegalStateException("Cache is not running.");
        }
    }

    private void checkNotRunning() {
        if (this.running) {
            throw new IllegalStateException("Cache is already running.");
        }
    }

    /**
     * Determines when the method {@link CacheObjectMaintainer#load(Object)} should be called when
     * {@link Cache#get(Object)} fails to find the desired object.
     */
    public static enum CacheMissBehaviour {
        /** If the object is not available (even if it was collected by GC), tries to load it. */
        LOAD_WHENEVER_NOT_AVAILABLE,
        /** If the object was not available before (i.e. no entry for the key), tries to load it. */
        LOAD_WHENEVER_NOT_AVAILABLE_BEFORE,
        /** Just return <code>null</code>. */
        DO_NOTHING
    }
}
