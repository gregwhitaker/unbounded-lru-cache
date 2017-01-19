package com.github.gregwhitaker.cache;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public final class UnboundedLRUCache<K, V> {

    private final HashMap<K, Node<K, V>> cache = new HashMap<>();
    private Node<K, V> leastRecentlyUsed;
    private Node<K, V> mostRecentlyUsed;
    private AtomicLong currentSize = new AtomicLong(0);
    private Consumer<EvictionEvent> evictionListener;
    private Consumer<PutEvent> putListener;

    /**
     * Creates a new unbounded least-recently-used cache.
     */
    public UnboundedLRUCache() {
        leastRecentlyUsed = new Node<>(null, null, null, null);
        mostRecentlyUsed = leastRecentlyUsed;
    }

    /**
     * Creates a new unbounded least-recently-used cache with an eviction event listener.
     *
     * @param evictionListener function that is invoked when an item is evicted from the cache
     */
    public UnboundedLRUCache(Consumer<EvictionEvent> evictionListener) {
        this.evictionListener = evictionListener;
    }

    /**
     * Creates a new unbounded least-recently-used cache with eviction and put event listeners.
     *
     * @param evictionListener function that is invoked when an item is evicted from the cache
     * @param putListener function that is invoked when an item is added to or updated within the cache
     */
    public UnboundedLRUCache(Consumer<EvictionEvent> evictionListener, Consumer<PutEvent> putListener) {
        this.evictionListener = evictionListener;
        this.putListener = putListener;
    }

    /**
     * Retrieve a value from the cache.
     *
     * @param key cache key
     * @return the value for the key if it exists; otherwise <code>null</code>
     */
    public synchronized V get(K key) {
        Node<K, V> node = cache.get(key);

        if (node == null) {
            // Item is not in the cache
            return null;
        } else if (node == mostRecentlyUsed) {
            // Item is already the most recently used so no need to move it
            return mostRecentlyUsed.value;
        }

        Node<K, V> previousNode = node.previous;
        Node<K, V> nextNode = node.next;

        if (node.key == leastRecentlyUsed.key) {
            // Left-most scenario
            nextNode.previous = null;
            leastRecentlyUsed = nextNode;
        } else if (node != mostRecentlyUsed.key) {
            // Middle scenario
            previousNode.next = nextNode;
            nextNode.previous = previousNode;
        }

        // Move the most recently used node to the head of the line
        node.previous = mostRecentlyUsed;
        mostRecentlyUsed.next = node;
        mostRecentlyUsed = node;
        mostRecentlyUsed.next = null;

        return node.value;
    }

    /**
     * Adds a new value to the cache or updates an existing value.
     *
     * @param key cache key
     * @param value the value to add or update
     */
    public synchronized void put(K key, V value) {
        if (cache.containsKey(key)) {
            Node<K, V> node = cache.get(key);
            V previousValue = node.value;
            node.value = value;

            if (node == mostRecentlyUsed) {
                cache.put(key, node);
            } else {
                Node<K, V> previousNode = node.previous;
                Node<K, V> nextNode = node.next;

                if (node.key == leastRecentlyUsed.key) {
                    // Left-most scenario
                    nextNode.previous = null;
                    leastRecentlyUsed = nextNode;
                } else {
                    // Middle scenario
                    previousNode.next = nextNode;
                    nextNode.previous = previousNode;
                }

                // Move the most recently used node to the head of the line
                node.previous = mostRecentlyUsed;
                mostRecentlyUsed.next = node;
                mostRecentlyUsed = node;
                mostRecentlyUsed.next = null;

                cache.put(key, node);
            }

            if (putListener != null) {
                putListener.accept(new PutEvent(key, value, previousValue));
            }
        } else {
            Node<K, V> newNode = new Node<>(mostRecentlyUsed, null, key, value);

            if (currentSize.get() == 0) {
                mostRecentlyUsed = newNode;
                leastRecentlyUsed = newNode;
            } else {
                mostRecentlyUsed.next = newNode;
                mostRecentlyUsed = newNode;
                mostRecentlyUsed.next = null;
            }

            cache.put(key, newNode);

            if (putListener != null) {
                putListener.accept(new PutEvent(key, value));
            }

            currentSize.addAndGet(1);
        }
    }

    /**
     * Evicts the least recently used item in the cache.
     *
     * @return the evicted item
     */
    public synchronized Map.Entry<K, V> evictLeastRecentlyUsed() {
        if (leastRecentlyUsed != null) {
            Node<K, V> evictedNode = cache.remove(leastRecentlyUsed.key);
            currentSize.decrementAndGet();

            Node<K, V> nextNode = leastRecentlyUsed.next;
            nextNode.previous = null;
            leastRecentlyUsed = nextNode;

            if (evictionListener != null) {
                evictionListener.accept(new EvictionEvent(evictedNode.key, evictedNode.value));
            }

            return new AbstractMap.SimpleImmutableEntry<>(evictedNode.key, evictedNode.value);
        }

        return null;
    }

    private class Node<A, B> {
        Node<A, B> next;
        Node<A, B> previous;
        A key;
        B value;

        public Node(Node<A, B> previous, Node<A, B> next, A key, B value) {
            this.previous = previous;
            this.next = next;
            this.key = key;
            this.value = value;
        }
    }

    /**
     * Event raised when an item is evicted from the cache.
     */
    public class EvictionEvent {
        private final K key;
        private final V value;
        private final long timestamp;

        public EvictionEvent(final K key, final V value) {
            this.key = key;
            this.value = value;
            this.timestamp = System.currentTimeMillis();
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public String toString() {
            return "EvictionEvent{" +
                    "key=" + key +
                    ", value=" + value +
                    ", timestamp=" + timestamp +
                    '}';
        }
    }

    /**
     * Event raised when an item is added to the cache.
     */
    public class PutEvent {
        private final K key;
        private final V value;
        private final V previousValue;
        private final long timestamp;

        public PutEvent(final K key, final V value) {
            this(key, value, null);
        }

        public PutEvent(final K key, final V value, final V previousValue) {
            this.key = key;
            this.value = value;
            this.previousValue = previousValue;
            this.timestamp = System.currentTimeMillis();
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        public V getPreviousValue() {
            return previousValue;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public boolean isUpdate() {
            return (value != null && previousValue != null);
        }

        @Override
        public String toString() {
            return "PutEvent{" +
                    "key=" + key +
                    ", value=" + value +
                    ", previousValue=" + previousValue +
                    ", timestamp=" + timestamp +
                    '}';
        }
    }

}
