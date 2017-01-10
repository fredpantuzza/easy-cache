package com.easycache.core;

import java.util.Random;

import com.easycache.core.maintainer.AccessTimeBasedCacheObjectMaintainer;

public class PerformanceTestApplication {

    public void start() {
        new CacheTestApplication().start();
        new DatabaseTestApplication();

        System.out.println("\n\nStarting cache test");
        new CacheTestApplication().start();
        System.out.println("Exiting cache test");

        System.out.println("\nStarting database test");
        new DatabaseTestApplication().start();
        System.out.println("Exiting database test");
    }

    public static void main(String[] args) {
        new PerformanceTestApplication().start();
    }
}

class CacheTestApplication extends TestApplication {

    private final Cache<Long, Foo> cache;

    public CacheTestApplication() {
        CacheLoader<Long, Foo> cacheLoader = key -> Database.retrieve(key);
        this.cache = new Cache<>(cacheLoader, new AccessTimeBasedCacheObjectMaintainer<>(10000));
    }

    @Override
    protected void beforeStart() {
        this.cache.start();
    }

    @Override
    protected void afterEnd() {
        this.cache.stop();
    }

    @Override
    protected Foo get(Long key) throws Exception {
        return this.cache.get(key);
    }
}

class DatabaseTestApplication extends TestApplication {

    @Override
    protected Foo get(Long key) throws Exception {
        return Database.retrieve(key);
    }
}

abstract class TestApplication {

    public void start() {
        class MyRunnable implements Runnable {
            @Override
            public void run() {
                final int count = 10_000;

                long time = System.currentTimeMillis();

                Random random = new Random();
                for (int i = 0; i < count; i++) {
                    long key = random.nextInt(500);
                    try {
                        @SuppressWarnings("unused")
                        Foo foo = get(key);
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.exit(-1);
                    }
                }

                time = System.currentTimeMillis() - time;

                System.out.println(String.format("Average time: %f", (double) time / (double) count));
            }
        }

        beforeStart();

        Thread thread1 = new Thread(new MyRunnable(), "Thread 1");
        Thread thread2 = new Thread(new MyRunnable(), "Thread 2");
        Thread thread3 = new Thread(new MyRunnable(), "Thread 3");

        thread1.start();
        thread2.start();
        thread3.start();

        try {
            thread1.join();
            thread2.join();
            thread3.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        afterEnd();
    }

    protected void beforeStart() {
    }

    protected void afterEnd() {
    }

    protected abstract Foo get(Long key) throws Exception;
}

final class Database {

    public static final Foo retrieve(Long key) throws InterruptedException {
        Thread.sleep(10);
        return new Foo(key, String.format("My Foo %d", key));
    }
}