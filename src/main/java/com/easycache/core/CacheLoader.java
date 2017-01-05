package com.easycache.core;

/**
 * Loader of entities that should be inserted in the cache.
 * @param <K> Type of the entity's key stored by the cache
 * @param <T> Type of the entity stored by the cache
 * @author frederico.pantuzza
 */
public interface CacheLoader<K, T> {

    /**
     * Loads an entity to be inserted in cache.
     * @param key Entity's key
     * @return The loaded entity or <code>null</code> if it does not exist
     * @throws Exception If an unexpected error occurred during the loading. This error may be propagated outside the
     *             cache
     */
    T load(K key) throws Exception;
}
