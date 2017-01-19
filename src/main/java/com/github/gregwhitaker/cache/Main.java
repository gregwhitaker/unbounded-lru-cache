package com.github.gregwhitaker.cache;

public class Main {

    public static void main(String... args) {
        UnboundedLRUCache<String, String> cache = new UnboundedLRUCache<>(System.out::println, System.out::println);

        cache.put("test", "test");
        cache.put("test1", "test1");
        cache.put("test2", "test2");
        cache.put("test", "newTest");

        cache.evictLeastRecentlyUsed();
    }
}
