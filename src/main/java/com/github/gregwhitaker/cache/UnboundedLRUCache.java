package com.github.gregwhitaker.cache;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class UnboundedLRUCache<K, V> {

    private final HashMap<K, Node<K, V>> cache;
    private Node<K, V> leastRecentlyUsed;
    private Node<K, V> mostRecentlyUsed;
    private AtomicLong currentSize = new AtomicLong(0);

    public UnboundedLRUCache() {
        cache = new HashMap<>();
        leastRecentlyUsed = new Node<>(null, null, null, null);
        mostRecentlyUsed = leastRecentlyUsed;
    }

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

    public synchronized void put(K key, V value) {
        if (cache.containsKey(key)) {
            Node<K, V> node = cache.get(key);

            if (node == mostRecentlyUsed) {
                node.value = value;
                cache.put(key, node);
            } else {
                Node<K, V> previousNode = node.previous;
                Node<K, V> nextNode = node.next;

                if (node.key == leastRecentlyUsed.key) {
                    // Left-most scenario
                    nextNode.previous = null;
                    leastRecentlyUsed = nextNode;
                } else if (node != mostRecentlyUsed) {
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
        } else {
            Node<K, V> newNode = new Node<>(mostRecentlyUsed, null, key, value);
            mostRecentlyUsed.next = newNode;
            cache.put(key, newNode);
            mostRecentlyUsed = newNode;

            if (currentSize.get() == 0) {
                leastRecentlyUsed = newNode;
            }

            currentSize.addAndGet(1);
        }
    }

    public synchronized EvictedEntry<K, V> evictLeastRecentlyUsed() {
        if (leastRecentlyUsed != null) {
            Node<K, V> evictedNode = cache.remove(leastRecentlyUsed.key);
            currentSize.decrementAndGet();

            Node<K, V> nextNode = leastRecentlyUsed.next;
            nextNode.previous = null;
            leastRecentlyUsed = nextNode;

            return new EvictedEntry<>(evictedNode.key, evictedNode.value);
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

}
