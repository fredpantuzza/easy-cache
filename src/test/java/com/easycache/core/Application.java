package com.easycache.core;

public class Application {

    public static void main(String[] args) {
        Cache<Long, Foo> cache = new Cache<>(new CacheLoader<Long, Foo>() {
            @Override
            public Foo load(Long key) throws Exception {
                return new Foo(key, String.format("Foo with id=%d", key));
            }
        }, new CacheObjectMaintainer<Long, Foo>() {
            @Override
            public boolean isMaintainedByCache(Foo entity, CacheObject<Foo> cacheObject, CacheMetadata cacheMetadata) {
                return true;
            }
        });
        cache.start();

        try {
            Foo foo1 = cache.get(1L);
            System.out.println(foo1);
            foo1 = null;

            Foo foo2 = cache.get(2L);
            System.out.println(foo2);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        cache.stop();

        System.out.println("Exiting");
    }
}
