package com.easycache.core;

/**
 * Cache metadata.
 * @author frederico.pantuzza
 */
public interface CacheMetadata {

    /**
     * @return the size of the cache in number of entities.
     *         <p>
     *         The returned value may differ slightly from the reality if the GC decides to collect entities during the
     *         calculation process.
     */
    int size();

    // TODO 30/05/2016 - int size(boolean safe)?
}
