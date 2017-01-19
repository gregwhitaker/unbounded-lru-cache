package com.github.gregwhitaker.cache;

public final class EvictedEntry<K, V> {
    private final K key;
    private final V value;
    private final long evictedTimestamp;

    public EvictedEntry(K key, V value) {
        this.key = key;
        this.value = value;
        this.evictedTimestamp = System.currentTimeMillis();
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    public long getEvictedTimestamp() {
        return evictedTimestamp;
    }
}
