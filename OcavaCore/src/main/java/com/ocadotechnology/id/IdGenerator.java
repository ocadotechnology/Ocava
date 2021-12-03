/*
 * Copyright © 2017-2021 Ocado (Ocava)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ocadotechnology.id;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Generates per-class-unique type-safe or non-type-safe Ids.
 * This class can be access by multiple threads.
 */
public final class IdGenerator {
    private static final Map<Class<?>, AtomicLong> idCounters = new ConcurrentHashMap<>();

    private IdGenerator() {
    }

    /**
     * Gets a type-safe Id object for convenient. Ids created by calls to this
     * function are not backed by a cache and as such are suitable
     * for use with continually incrementing values.
     * @param classForId class which represents Id
     */
    public static <T> Id<T> getId(Class<T> classForId) {
        return Id.create(getNextId(classForId));
    }

    /**
     * Gets a cache-backed, type-safe Id object. The backing cache shares
     * objects between classes to avoid unnecessary object instantiation.
     * <p>
     * Classes with a fixed pool of objects are a
     * good candidate for cached Ids, but continually incrementing ranges
     * will cause shared objects to be pushed out of
     * the cache when it fills, rendering the cache useless.
     * @param classForId class which represents Id
     */
    public static <T> Id<T> getCachedId(Class<T> classForId) {
        return Id.createCached(getNextId(classForId));
    }

    /**
     * Returns the non-type-safe, per class unique long id generator. For use with classes that are instantiated in
     * large numbers, where the overhead of the map lookup and the creation of the typed id object is too slow.
     * @param classForId class which represents Id
     */
    public static AtomicLong getRawIdGenerator(Class<?> classForId) {
        AtomicLong counter = idCounters.get(classForId);
        //computeIfAbsent is slower (in concurrentHashMap) than get and then computeIfAbsent if null
        return counter != null ? counter : idCounters.computeIfAbsent(classForId, k -> new AtomicLong());
    }

    /**
     * Allows to reset the counter to selected value
     * @param classForId class which represents Id
     * @param lastId new id value
     */
    public static void initialiseIdCounter(Class<?> classForId, long lastId) {
        getRawIdGenerator(classForId).set(lastId);
    }

    /**
     * <b>Discouraged use</b>, see notes<br><br>
     *
     * Sets all cached entries to zero.
     * <br><br>
     *
     * <p>
     *     <b>Note 1:</b>
     *     Should be used only by tests as it does not guarantee atomicity
     * </p>
     * <p>
     *     <b>Note 2:</b>
     *     This class provides globally available ids, based on classes. Extra care needs to be taken when using the
     *     reset method, so as not to lead to unwanted id collisions.
     *     This is particularly important with test engines, and test suites.
     * <br><br>
     *     Test suites usually are composed by some one-time code (constructors, @BeforeAll/@AfterAll callbacks),
     *     and a test loop(@BeforeEach/@AfterEach callback, test itself).
     * <br><br>
     *     <b>Do not use reset</b> in your tests, if the IdGenerator is used outside of the test loop.
     *     Tests using this class should rather be id-agnostic, and not rely on the generator providing specific ids.
     * </p>
     * */
    public static void clear() {
        idCounters.values().forEach(c -> c.set(0));
    }

    private static long getNextId(Class<?> clazz) {
        return getRawIdGenerator(clazz).getAndIncrement();
    }
}
