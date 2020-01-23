/*
 * Copyright © 2017-2020 Ocado (Ocava)
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
package com.ocadotechnology.indexedcache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.UnmodifiableIterator;
import com.ocadotechnology.id.Id;
import com.ocadotechnology.id.Identified;
import com.ocadotechnology.id.Identity;

class HashMapObjectStoreTest {

    private final TestObject AN_OBJECT = new TestObject(1, 1);
    private final ImmutableSet<TestObject> MANY_OBJECTS = ImmutableSet.of(
            new TestObject(1, 1),
            new TestObject(2, 1),
            new TestObject(3, 1),
            new TestObject(4, 1),
            new TestObject(5, 1)
    );

    private HashMapObjectStore<TestObject, TestObject> testStore;

    @BeforeEach
    void setUp() {
        testStore = new HashMapObjectStore<>();
    }

    /**
     * Black-box tests which verify the behaviour of a HashMapObjectStore as defined by the public API.
     */
    @Nested
    class BehaviourTests {
        @Test
        void addAll_whenCollectionIsEmpty_thenCacheIsUnchanged() {
            testStore.addAll(ImmutableList.of());
            assertEquals(0, testStore.size());
        }

        @Test
        void addAll_whenCollectionIsNotEmpty_thenCacheUpdated() {
            testStore.addAll(MANY_OBJECTS);
            assertEquals(MANY_OBJECTS.size(), testStore.size());
            for (TestObject testObject : MANY_OBJECTS) {
                assertEquals(testObject, testStore.get(testObject.id));
            }
        }

        @Test
        void addAll_whenSomeElementsAlreadyAdded_thenThrowsExceptionAndCacheUnchanged() {
            testStore.add(AN_OBJECT);
            Assertions.assertThrows(CacheUpdateException.class, () -> testStore.addAll(MANY_OBJECTS));
            assertEquals(1, testStore.size());
            assertEquals(AN_OBJECT, testStore.get(AN_OBJECT.id));
        }

        @Test
        void updateAll_whenCollectionIsEmpty_thenCacheNotUpdated() {
            testStore.updateAll(ImmutableList.of());
            assertEquals(0, testStore.size());
        }

        @Test
        void updateAll_whenCollectionIsNotEmpty_thenCacheUpdated() {
            testStore.addAll(MANY_OBJECTS);
            ImmutableSet<Change<TestObject>> objectUpdates = MANY_OBJECTS.stream()
                    .map(o -> Change.update(o, new TestObject(o.getId().id, o.data + 1)))
                    .collect(ImmutableSet.toImmutableSet());
            testStore.updateAll(objectUpdates);
            ImmutableSet<TestObject> newObjects = objectUpdates.stream()
                    .map(u -> u.newObject)
                    .collect(ImmutableSet.toImmutableSet());

            for (TestObject newObject : newObjects) {
                assertEquals(newObject, testStore.get(newObject.id));
            }
        }

        @Test
        void updateAll_whenCollectionHasAdditions_thenCacheUpdated() {
            ImmutableSet<Change<TestObject>> objectUpdates = MANY_OBJECTS.stream()
                    .map(Change::add)
                    .collect(ImmutableSet.toImmutableSet());
            testStore.updateAll(objectUpdates);

            for (TestObject newObject : MANY_OBJECTS) {
                assertEquals(newObject, testStore.get(newObject.id));
            }
        }

        @Test
        void updateAll_whenCollectionHasDeletions_thenCacheUpdated() {
            testStore.addAll(MANY_OBJECTS);
            ImmutableSet<Change<TestObject>> objectUpdates = MANY_OBJECTS.stream()
                    .map(Change::delete)
                    .collect(ImmutableSet.toImmutableSet());
            testStore.updateAll(objectUpdates);
            assertEquals(0, testStore.size());
        }

        @Test
        void updateAll_whenSomeElementsDifferent_thenCacheNotUpdated() {
            testStore.addAll(MANY_OBJECTS);
            ImmutableList<TestObject> changedObjects = MANY_OBJECTS.stream()
                    .map(object -> {
                        TestObject newObject = object.id.id < 2 ? new TestObject(object.id.id, object.data - 2) : object;
                        testStore.update(object, newObject);
                        return newObject;
                    }).collect(ImmutableList.toImmutableList());
            ImmutableSet<Change<TestObject>> objectUpdates = MANY_OBJECTS.stream()
                    .map(o -> Change.update(o, new TestObject(o.getId().id, o.data + 1)))
                    .collect(ImmutableSet.toImmutableSet());
            assertThrows(CacheUpdateException.class, () -> testStore.updateAll(objectUpdates));

            for (TestObject newObject : changedObjects) {
                assertEquals(newObject, testStore.get(newObject.id), "Incorrect object returned for id " + newObject.id);
            }
        }

        @Test
        void add_whenObjectNew_thenCacheUpdated() {
            testStore.add(AN_OBJECT);
            assertEquals(AN_OBJECT, testStore.get(AN_OBJECT.id));
        }

        @Test
        void add_whenObjectPresent_thenThrowsExceptionAndCacheUnchanged() {
            testStore.add(AN_OBJECT);
            assertThrows(CacheUpdateException.class, () -> testStore.add(new TestObject(AN_OBJECT.id.id, -3)));
            assertEquals(AN_OBJECT, testStore.get(AN_OBJECT.id));
        }

        @Test
        void update_whenUpdatingOld_thenCacheUpdated() {
            testStore.add(AN_OBJECT);
            TestObject newObject = new TestObject(AN_OBJECT.getId().id, -3);
            testStore.update(AN_OBJECT, newObject);
            assertEquals(newObject, testStore.get(AN_OBJECT.id));
        }

        @Test
        void update_whenAddingNew_thenCacheUpdated() {
            testStore.update(null, AN_OBJECT);
            assertEquals(AN_OBJECT, testStore.get(AN_OBJECT.id));
        }

        @Test
        void update_whenDeletingOld_thenCacheUpdated() {
            testStore.add(AN_OBJECT);
            testStore.update(AN_OBJECT, null);
            assertNull(testStore.get(AN_OBJECT.id));
        }

        @Test
        void update_whenUpdatingOutOfDate_thenThrowsExceptionAndCacheNotUpdated() {
            testStore.add(AN_OBJECT);
            TestObject oldObject = new TestObject(AN_OBJECT.getId().id, -1);
            TestObject newObject = new TestObject(AN_OBJECT.getId().id, -3);
            assertThrows(CacheUpdateException.class, () -> testStore.update(oldObject, newObject));
            assertEquals(AN_OBJECT, testStore.get(AN_OBJECT.id));
        }

        @Test
        void delete_whenObjectInCache_thenObjectReturnedAndCacheChanged() {
            testStore.add(AN_OBJECT);
            assertEquals(AN_OBJECT, testStore.delete(AN_OBJECT.id));
            assertEquals(0, testStore.size());
        }

        @Test
        void delete_whenObjectNotInCache_thenThrowsExceptionAndCacheUnChanged() {
            testStore.add(AN_OBJECT);
            assertThrows(CacheUpdateException.class, () -> testStore.delete(Id.create(AN_OBJECT.getId().id + 1)));
            assertEquals(AN_OBJECT, testStore.get(AN_OBJECT.id));
            assertEquals(1, testStore.size());
        }

        @Test
        void deleteAll_whenObjectsInCache_thenObjectsReturnedAndCacheChanged() {
            testStore.addAll(MANY_OBJECTS);
            ImmutableCollection<TestObject> deletedObjects = testStore.deleteAll(MANY_OBJECTS.stream().map(TestObject::getId).collect(ImmutableList.toImmutableList()));
            assertEquals(MANY_OBJECTS, ImmutableSet.copyOf(deletedObjects));
            assertEquals(0, testStore.size());
        }

        @Test
        void deleteAll_whenSomeObjectsNotInCache_thenThrowsExceptionAndCacheUnChanged() {
            testStore.addAll(MANY_OBJECTS);
            assertThrows(CacheUpdateException.class, () -> testStore.deleteAll(MANY_OBJECTS.stream().map(o -> Id.<TestObject>create(o.id.id + 1)).collect(ImmutableList.toImmutableList())));
            assertEquals(MANY_OBJECTS.size(), testStore.size());
            for (TestObject object : MANY_OBJECTS) {
                assertEquals(object, testStore.get(object.id));
            }
        }

        @Test
        void get_whenANonStoredIdIsProvided_thenNullIsReturned() {
            TestObject retrieved = testStore.get(AN_OBJECT.getId());
            assertThat(retrieved).isNull();
        }

        @Test
        void get_whenAStoredIdIsProvided_thenTheStoredObjectIsReturned() {
            testStore.add(AN_OBJECT);
            TestObject retrieved = testStore.get(AN_OBJECT.getId());
            assertThat(retrieved).isSameAs(AN_OBJECT);
        }

        @Test
        void containsId_whenANonStoredIdIsProvided_thenFalseIsReturned() {
            assertThat(testStore.containsId(AN_OBJECT.getId())).isFalse();
        }

        @Test
        void containsId_whenAStoredIdIsProvided_thenTrueIsReturned() {
            testStore.add(AN_OBJECT);
            assertThat(testStore.containsId(AN_OBJECT.getId())).isTrue();
        }

        @Test
        void stream_whenAStreamIsRequestedOnAnEmptyStore_thenAnEmptyStreamIsProvided() {
            ImmutableSet<TestObject> streamedObjects = testStore.stream().collect(ImmutableSet.toImmutableSet());
            assertTrue(streamedObjects.isEmpty());
        }

        @Test
        void stream_whenAStreamIsRequested_thenAStreamOfStoredObjectsIsProvided() {
            testStore.addAll(MANY_OBJECTS);
            ImmutableSet<TestObject> streamedObjects = testStore.stream().collect(ImmutableSet.toImmutableSet());
            assertThat(streamedObjects).isEqualTo(MANY_OBJECTS);
        }

        @Test
        void forEach_whenForEachAppliedOnAnEmptyStore_thenAnNoElementsAreAccepted() {
            List<TestObject> list = new ArrayList<>();
            testStore.forEach(list::add);
            assertTrue(list.isEmpty());
        }

        @Test
        void forEach_whenForEachApplied_thenElementsAreAccepted() {
            testStore.addAll(MANY_OBJECTS);
            List<TestObject> list = new ArrayList<>();
            testStore.forEach(list::add);
            assertThat(list).containsExactlyElementsOf(MANY_OBJECTS);
        }

        @Test
        void clear_whenAStoreContainingObjectsIsCleared_thenTheStoreIsEmptied() {
            testStore.addAll(MANY_OBJECTS);
            assertThat(testStore.size()).isNotEqualTo(0);
            testStore.clear();
            assertThat(testStore.size()).isEqualTo(0);
        }

        @Test
        void size_whenAStoreIsEmpty_thenTheSizeIsZero() {
            assertThat(testStore.size()).isEqualTo(0);
        }

        @Test
        void size_whenAStoreIsNotEmpty_thenTheSizeIsNonZero() {
            testStore.addAll(MANY_OBJECTS);
            assertThat(testStore.size()).isNotEqualTo(0);
            assertThat(testStore.size()).isEqualTo(MANY_OBJECTS.size());
        }

        @Test
        void snapshot_whenASnapshotIsRequestedAfter_aNewStoreIsCreated_thenAnEmptyCopyIsProvided() {
            assertTrue(testStore.snapshot().isEmpty());
        }

        @Test
        void snapshot_whenASnapshotIsRequestedAfter_addAll_thenAnUpToDateCopyIsProvided() {
            testStore.snapshot(); // Snapshot before change to show change invalidates snapshot. This snapshot tested elsewhere.
            testStore.addAll(MANY_OBJECTS);
            assertEquals(MANY_OBJECTS, ImmutableSet.copyOf(testStore.snapshot().values()));
        }

        @Test
        void snapshot_whenASnapshotIsRequestedAfter_updateAllWithExistingObjects_thenAnUpToDateCopyIsProvided() {
            testStore.addAll(MANY_OBJECTS);
            testStore.snapshot(); // Snapshot before change to show change invalidates snapshot. This snapshot tested elsewhere.
            ImmutableSet<Change<TestObject>> objectUpdates = MANY_OBJECTS.stream()
                    .map(o -> Change.update(o, new TestObject(o.getId().id, o.data + 1)))
                    .collect(ImmutableSet.toImmutableSet());
            testStore.updateAll(objectUpdates);
            ImmutableSet<TestObject> newObjects = objectUpdates.stream()
                    .map(u -> u.newObject)
                    .collect(ImmutableSet.toImmutableSet());
            assertEquals(newObjects, ImmutableSet.copyOf(testStore.snapshot().values()));
        }

        @Test
        void snapshot_whenASnapshotIsRequestedAfter_add_thenAnUpToDateCopyIsProvided() {
            testStore.snapshot(); // Snapshot before change to show change invalidates snapshot. This snapshot tested elsewhere.
            testStore.add(AN_OBJECT);
            assertThat(testStore.snapshot().values()).isEqualTo(ImmutableSet.of(AN_OBJECT));
        }

        @Test
        void snapshot_whenASnapshotIsRequestedAfter_deleteAll_thenAnUpToDateCopyIsProvided() {
            testStore.addAll(MANY_OBJECTS);
            testStore.snapshot(); // Snapshot before change to show change invalidates snapshot. This snapshot tested elsewhere.
            testStore.deleteAll(MANY_OBJECTS.stream().map(TestObject::getId).collect(ImmutableSet.toImmutableSet()));
            assertTrue(testStore.snapshot().isEmpty());
        }

        @Test
        void snapshot_whenASnapshotIsRequestedAfter_delete_thenAnUpToDateCopyIsProvided() {
            testStore.add(AN_OBJECT);
            testStore.snapshot(); // Snapshot before change to show change invalidates snapshot. This snapshot tested elsewhere.
            testStore.delete(AN_OBJECT.getId());
            assertTrue(testStore.snapshot().isEmpty());
        }

        @Test
        void snapshot_whenASnapshotIsRequestedAfter_update_thenAnUpToDateCopyIsProvided() {
            testStore.add(AN_OBJECT);
            testStore.snapshot(); // Snapshot before change to show change invalidates snapshot. This snapshot tested elsewhere.
            TestObject updatedObject = new TestObject(AN_OBJECT.getId().id, AN_OBJECT.data + 1);
            testStore.update(AN_OBJECT, updatedObject);
            assertThat(testStore.snapshot().values()).isEqualTo(ImmutableSet.of(updatedObject));
        }

        @Test
        void snapshot_whenASnapshotIsRequestedAfter_updateToAdd_thenAnUpToDateCopyIsProvided() {
            testStore.snapshot(); // Snapshot before change to show change invalidates snapshot. This snapshot tested elsewhere.
            testStore.update(null, AN_OBJECT);
            assertThat(testStore.snapshot().values()).isEqualTo(ImmutableSet.of(AN_OBJECT));
        }

        @Test
        void snapshot_whenASnapshotIsRequestedAfter_updateToDelete_thenAnUpToDateCopyIsProvided() {
            testStore.add(AN_OBJECT);
            testStore.snapshot(); // Snapshot before change to show change invalidates snapshot. This snapshot tested elsewhere.
            testStore.update(AN_OBJECT, null);
            assertThat(testStore.snapshot().values()).isEmpty();
        }

        @Test
        void snapshot_whenASnapshotIsRequestedAfter_clear_thenAnUpToDateCopyIsProvided() {
            testStore.addAll(MANY_OBJECTS);
            testStore.snapshot(); // Snapshot before change to show change invalidates snapshot. This snapshot tested elsewhere.
            testStore.clear();
            assertTrue(testStore.snapshot().isEmpty());
        }

        @Test
        void iterator_whenRemovalIsAttempted_thenUnsupportedOperationExceptionIsThrown() {
            testStore.add(AN_OBJECT);
            UnmodifiableIterator<TestObject> it = testStore.iterator();
            assertThrows(UnsupportedOperationException.class, it::remove);
        }

        @Test
        void iterator_whenCacheIsIterated_thenAllElementsAreReturned() {
            testStore.addAll(MANY_OBJECTS);
            UnmodifiableIterator<TestObject> it = testStore.iterator();

            assertTrue(it.hasNext());
            MANY_OBJECTS.forEach(testObject -> assertEquals(testObject, it.next()));
            assertFalse(it.hasNext());
        }
    }

    /**
     * White-box tests which verify implementation details of HashMapObjectStore that do not form part of the public API.
     * The behaviours verified by these tests are subject to change and should not be relied upon by users of the
     * HashMapObjectStore class.
     */
    @Nested
    class ImplementationTests {

        private Object firstSnapshot;

        @BeforeEach
        void initialiseStoreAndFirstSnapshot() {
            // First snapshot is non-empty, to guarantee that any test which regenerates the snapshot will fail.
            // This will not happen with an empty snapshot as ImmutableMap.of() is a singleton.
            testStore.addAll(MANY_OBJECTS);
            assertThat(testStore.get(AN_OBJECT.getId())).isNotNull();  // some tests expect this object to be present
            firstSnapshot = testStore.snapshot();
        }

        @Test
        void snapshot_whenASnapshotIsRequestedAfter_addAllWithEmptyCollection_thenSameSnapshotIsProvided() {
            testStore.addAll(ImmutableSet.of());
            assertThat(testStore.snapshot()).isSameAs(firstSnapshot);
        }

        @Test
        void snapshot_whenASnapshotIsRequestedAfter_addAllWithRollback_thenSameSnapshotIsProvided() {
            assertThrows(CacheUpdateException.class, () -> testStore.addAll(MANY_OBJECTS));  // addAll() fails and triggers roll-back
            assertThat(testStore.snapshot()).isSameAs(firstSnapshot);
        }

        @Test
        void snapshot_whenASnapshotIsRequestedAfter_updateAllWithEmptyCollection_thenSameSnapshotIsProvided() {
            testStore.updateAll(ImmutableSet.of());
            assertThat(testStore.snapshot()).isSameAs(firstSnapshot);
        }

        @Test
        void snapshot_whenASnapshotIsRequestedAfter_updateAllWithRollback_thenSameSnapshotIsProvided() {
            ImmutableSet<Change<TestObject>> wrongObjectUpdates = MANY_OBJECTS.stream()
                    .map(o -> Change.update(new TestObject(o.getId().id + 1, o.data), new TestObject(o.getId().id + 1, o.data + 1)))
                    .collect(ImmutableSet.toImmutableSet());
            assertThrows(CacheUpdateException.class, () -> testStore.updateAll(wrongObjectUpdates));  // updateAll() fails and triggers roll-back
            assertThat(testStore.snapshot()).isSameAs(firstSnapshot);
        }

        @Test
        void snapshot_whenASnapshotIsRequestedAfter_addWithRollback_thenSameSnapshotIsProvided() {
            assertThrows(CacheUpdateException.class, () -> testStore.add(AN_OBJECT));  // add() fails and triggers roll-back
            assertThat(testStore.snapshot()).isSameAs(firstSnapshot);
        }

        @Test
        void snapshot_whenASnapshotIsRequestedAfter_deleteAllWithEmptyCollection_thenSameSnapshotIsProvided() {
            testStore.deleteAll(ImmutableSet.of());
            assertThat(testStore.snapshot()).isSameAs(firstSnapshot);
        }

        @Test
        void snapshot_whenASnapshotIsRequestedAfter_deleteAllWithRollback_thenSameSnapshotIsProvided() {
            ImmutableSet<Identity<? extends TestObject>> wrongObjectIds = MANY_OBJECTS.stream()
                    .map(TestObject::getId)
                    .map(id -> Id.<TestObject>create(id.id + 1))
                    .collect(ImmutableSet.toImmutableSet());
            assertThrows(CacheUpdateException.class, () -> testStore.deleteAll(wrongObjectIds));  // deleteAll() fails and triggers roll-back
            assertThat(testStore.snapshot()).isSameAs(firstSnapshot);
        }

        @Test
        void snapshot_whenASnapshotIsRequestedAfter_deleteWithRollback_thenSameSnapshotIsProvided() {
            Identity<? extends TestObject> wrongId = Id.create(1_000_000);
            assertThrows(CacheUpdateException.class, () -> testStore.delete(wrongId));  // delete() fails and triggers roll-back
            assertThat(testStore.snapshot()).isSameAs(firstSnapshot);
        }

        @Test
        void snapshot_whenASnapshotIsRequestedAfter_updateWithRollback_thenSameSnapshotIsProvided() {
            TestObject updatedObject = new TestObject(AN_OBJECT.getId().id, AN_OBJECT.data + 1);
            TestObject wrongExistingObject = new TestObject(AN_OBJECT.getId().id + 1, AN_OBJECT.data);
            assertThrows(CacheUpdateException.class, () -> testStore.update(wrongExistingObject, updatedObject));  // update() fails and triggers roll-back
            assertThat(testStore.snapshot()).isSameAs(firstSnapshot);
        }

        @Test
        void snapshot_whenASnapshotIsRequestedAfter_updateToAddWithRollback_thenSameSnapshotIsProvided() {
            assertThrows(CacheUpdateException.class, () -> testStore.update(null, AN_OBJECT));  // update() fails and triggers roll-back
            assertThat(testStore.snapshot()).isSameAs(firstSnapshot);
        }

        @Test
        void snapshot_whenASnapshotIsRequestedAfter_updateToDeleteWithRollback_thenSameSnapshotIsProvided() {
            TestObject wrongExistingObject = new TestObject(AN_OBJECT.getId().id + 1, AN_OBJECT.data);
            assertThrows(CacheUpdateException.class, () -> testStore.update(wrongExistingObject, null));  // update() fails and triggers roll-back
            assertThat(testStore.snapshot()).isSameAs(firstSnapshot);
        }
    }

    private class TestObject implements Identified<TestObject> {

        private final Id<TestObject> id;
        final long data;

        TestObject(long id, long data) {
            this.id = Id.create(id);
            this.data = data;
        }

        @Override
        public Id<TestObject> getId() {
            return id;
        }
    }
}
