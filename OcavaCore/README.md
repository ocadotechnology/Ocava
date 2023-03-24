# OcavaCore

This component provides the DES framework in combination with a number of additional tools and utilities that are are useful both in and out of simulations.
The key classes within OcavaCore are:

`EventScheduler/TimeProvider` - These interfaces are the core of the DES system.  For simple systems, the SimpleDiscreteEventScheduler is sufficient, while for more complex systems with multiple simulated threads, the SourceTrackingEventScheduler works with the `NotificationRouter` to simulate thread handover between different parts of the system.  The RealtimeEventScheduler can be passed into the system to test real time, multi-threaded instances, or to be used in a production environment.

`NotificationRouter/NotificationBus/Notification` - These classes, which are built upon the [`EventBus`] from [Guava], allow your code to broadcast events around the system such that physical events can be observed at arbitrary points in the system.  At its simplest, this represents an abstraction of the Listener pattern where neither the broadcaster nor the listener need to have any reference to each other.

[`EventBus`]: https://github.com/google/guava/tree/master/guava/src/com/google/common/eventbus
[Guava]: https://github.com/google/guava

Some of the other utilities included are:
* Tools to make randomness deterministic between program runs
* Physics utilities to help deal with acceleration
* [Immutable object cache](src/main/java/com/ocadotechnology/indexedcache/IndexedImmutableObjectCache.md) for efficient lookups of immutable objects
* API for different modes of file access implemented through [Java Service Provider Interface (SPI)]
* Tools for [loading and retrieving program configuration](src/main/java/com/ocadotechnology/config/Config.md)

[Java Service Provider Interface (SPI)]: https://docs.oracle.com/javase/tutorial/ext/basics/spi.html
### File Access through SPI
In order to access files through a specific mode, the service provider for that mode, should be available in the classpath. For example, if the file needs to be accessed through mode S3, then OcavaS3 should be available in the classpath of the application.

OcavaCore also has a `CredentialProvider` class that allows the credentials for file access to be located in a file named `.ocava_access_credentials` within the `credentials` folder inside the home directory.

So credentials can be stored in `<HOME_DIR>/.ocava_access_credentials/credentials`

### Configuration for maven-shade plugin
If maven-shade plugin is used in the project to build an uber-jar, then this will not enable multiple service providers for file access by default, even if all the required libraries are in classpath. A transformer configuration that will add all the service providers will be required. This can done by adding the following configuration to the maven-shade plugin.

`<transformer implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"/>`
