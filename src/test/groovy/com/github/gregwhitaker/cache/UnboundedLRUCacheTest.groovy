/*
 * Copyright 2017 Greg Whitaker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.gregwhitaker.cache

import spock.lang.Specification

class UnboundedLRUCacheTest extends Specification {

    def "items can be added to the cache"() {
        given:
        UnboundedLRUCache<String, String> cache = new UnboundedLRUCache<>()

        when:
        cache.put("key1", "value1")
        cache.put("key2", "value2")

        then:
        cache.get("key1") == "value1"
        cache.get("key2") == "value2"
    }

    def "least recently used item can be evicted from the cache"() {
        given:
        UnboundedLRUCache<String, String> cache = new UnboundedLRUCache<>()
        cache.put("key1", "value1")
        cache.put("key2", "value2")
        cache.get("key1")

        when:
        def evictedEntry = cache.evictLeastRecentlyUsed()

        then:
        evictedEntry.getKey() == "key2"
    }
}
