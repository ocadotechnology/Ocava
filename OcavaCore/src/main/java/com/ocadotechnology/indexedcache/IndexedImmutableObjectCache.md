# Indexed Immutable Object Cache

An IndexedImmutableObjectCache is an object store which combines
indexes and listeners to form a powerful framework.

## Requirements

Objects stored in the cache need to be immutable. They also must
implement the `Identified` interface. The cache uses the returned
`Identity` to determine which objects are equivalent.

## Update Methods

The basic update methods on the cache are `add`, `update` and
`delete`.  All of them enforce that the state prior to the change
is as expected (eg, calling `add` when a matching object already
exists in the cache is an error case). Note that, for the update
method, the code expects that the exact object stored in the cache
to be passed in as the "initial value". This has been shown to
help identify cases where later changes were inadvertently
overwriting earlier ones.

There are also atomic methods which batch multiple updates into one
method call and apply the changes to the caches indices as a single action.

## Indices

A lot of the power of this framework comes from the indices.
Most indices can be defined with a simple lambda eg:

`OptionalOneToOneIndex<Driver, Car> byDriver = carCache.addOptionalOneToOneIndex(car -> Optional.ofNullable(car.getDriver());`

And then allow easy querying of the underlying cache:

`Car kitt = byDriver.get(michaelKnight);`

There are a wide range of index types available in the library,
and calling code can also define their own custom indices.

## Listeners

The last element of this framework are the listeners. These are essentially
BiConsumers of the form

`(oldValue, newValue) -> action`

Note that either one (but not both) of the supplied values may be null if the
values is being added to or deleted from the cache.

A common gotcha with listeners occurs if they trigger logic which can itself modify
the underlying cache. This originally caused the second update to interrupt the
first, resulting in later listeners receiving the second update trigger before the
first one. To expose these issues, this will now trigger a
ConcurrentModificationException to be thrown by the update method.

If the calling code uses the `EventScheduler` framework, it is simple to make a 
scheduler available to the listener, and call the doNow method to allow the
current update to complete before triggering additional changes.

# Advanced Concepts

## Cache Update Exception

This exception is thrown when something goes wrong during an update method.
It is unchecked, so the calling code does not need to handle it - such
errors likely indicate fundamental errors in code paths or logic - but if
handling is desired, the code guarantees that when this exception is thrown,
no changes will have been made to the cache or its indices, and the listeners
will not have been called.

## Index Names

When creating an index, the caller can provide a String name to identify it.
This name will be attached to the CacheUpdateException if that index throws
a handled error during an update. This field can then be used by the calling
code to control the recovery behaviour.

Extending the example above, imagine that the caller wanted to recover from
an error where the same driver was assinged to multiple cars:

```
private static final String DRIVER_INDEX = "DRIVER_INDEX";

.....

OptionalOneToOneIndex<Driver, Car> byDriver = carCache.addOptionalOneToOneIndex(DRIVER_INDEX, car -> Optional.ofNullable(car.getDriver()));

.....

try {
  carCache.update(oldState, newState);
} catch (CacheUpdateException e) {
  if (DRIVER_INDEX.equals(e.getFailingIndex().orElse(null))) {
    vacateAllCars();
    assignAvailableCars();
    return;
  }
  throw e;
}
```

In this case, the author decided to recover from a failure in one index,
while retaining the original fail-fast behaviour for all others.

In most small- and even mid-scale applications, this kind of error
handling might seem overkill. Surely trying to put a single driver in two
cars indicates a significant failure in the system's logic, so a fail-fast
approach is best. We have found that, as the system gets larger, and the
cost of failure rises, it can be more appealing to recover from some of these
errors, and investigate them asynchronously later on.

## Index Update Exception

This exception is internal to the framework, and so should only be of interest
to those adding custom indices. Many of the methods that can or must be
implemented by a custom index can throw this exception.

As with the CacheUpdateException, throwing this exception must guarantee that
the throwing method has made no changes to the state of the index. This applies
whether the method being written throws the exception itself or allows it to
propagate from a method it itself has called. When writing code which uses this
mechanism, thorough testing is advised.