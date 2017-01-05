package com.easycache.core;

import com.easycache.core.maintainer.AccessTimeBasedCacheObjectMaintainer;

public class Application {

    private final Cache<Long, Foo> cache;

    public Application() {
        CacheLoader<Long, Foo> cacheLoader = key -> new Foo(key, String.format("My Foo %d", key));
        this.cache = new Cache<>(cacheLoader, new AccessTimeBasedCacheObjectMaintainer<>(500));
    }

    public void start() {
        this.cache.start();

        // TODO start threads

        try {
            Foo foo1 = this.cache.get(1L);
            System.out.println(foo1);
            foo1 = null;

            Foo foo2 = this.cache.get(2L);
            System.out.println(foo2);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        this.cache.stop();

        System.out.println("Exiting");
    }

    public static void main(String[] args) {
        new Application().start();
    }
}
