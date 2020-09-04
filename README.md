# Ocava

Ocava is an open-source Java project for writing and testing simulations. It provides a number of useful tools for either adding a simulation to an existing production environment or creating one from scratch.

Ocava was created by [Ocado Technology]'s Simulation Team after they successfully implemented a simulation and testing framework in order to test a radical proof of concept fulfilment warehouse proposal. Simulations were used not only to prove the potential of the proposed warehouse design but it was later used to test the production control system for thousands of hours before the code ever reached production.
As Ocava is not domain-specific it can be used to create simulations of any event-driven process.

[Ocado Technology]: https://www.ocadotechnology.com/

For a brief overview on how to attach a simulation to production code look at section [Attaching a Simulation to Production Code](#Attaching-a-Simulation-to-Production-Code) below.

This project contains a Discrete Event Simulation (DES) framework and tools. However, simulations created by Ocava can run more than just isolated, deterministic DES applications.
By switching which `EventScheduler` implementation is used, it is possible to run the same code in a realtime production environment or a DES testing environment with minimal configuration changes.

Ocava is comprised of three primary components:

## OcavaCore

This component provides the DES framework in combination with a number of additional tools and utilities that are are useful both in and out of simulations.
The key classes within OcavaCore are:

`EventScheduler/TimeProvider` - These interfaces are the core of the DES system.  For simple systems, the SimpleDiscreteEventScheduler is sufficient, while for more complex systems with multiple simulated threads, the SourceTrackingEventScheduler works with the `NotificationRouter` to simulate thread handover between different parts of the system.  The RealtimeEventScheduler can be passed into the system to test real time, multi-threaded instances, or to be used in a production environment.

`NotificationRouter/NotificationBus/Notification` - These classes, which are built upon the [`EventBus`] from [Guava], allow your code to broadcast events around the system such that physical events can be observed at arbitrary points in the system.  At its simplest, this represents an abstraction of the Listener pattern where neither the broadcaster nor the listener need to have any reference to each other.
 
[`EventBus`]: https://github.com/google/guava/tree/master/guava/src/com/google/common/eventbus
[Guava]: https://github.com/google/guava

Some of the other utilities included are:
* Tools to make randomness deterministic between program runs
* Physics utilities to help deal with acceleration
* Immutable object cache for efficient lookups of immutable objects

## OcavaS3

This component provides a number of useful utilities to connect to AWS services including caches to reduce costs from repeated connections.

## OcavaScenarioTest

This component provides a scenario test framework designed to work with an event-driven process.
The framework enables scenario tests to be written in code rather than in text which allows for both type safety and easy code re-use between scenario tests.

## Contributing

See [here](CONTRIBUTING.md)

## Attaching a Simulation to Production Code

There are multiple ways in which you can attach a simulation to production code. Two ways Ocado has done it are:

*  Develop your production code using Ocava's `EventScheduler`, as described above, and use it to schedule events at specific times. The simulation can then schedule these events along with its own simulation specific events.
*  If you already have an event-driven system then your production codebase doesn't need to use the `EventScheduler`. The simulation can schedule simulated events, representing someone using your system, which will then call endpoints on your production code base.

## Examples

TrafficLightSimulation is an example project which utilises the framework to create a simulated environment for a traffic light controller that is able to run in both discrete event and realtime. 
This simulation also makes use of the scenario test framework.

## License

[Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0.html)