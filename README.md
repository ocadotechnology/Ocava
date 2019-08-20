# Ocava

This project contains the Ocado Technology simulation team Discrete Event Simulation (DES) framework and tools, plus a number of additional tools and utilites we found useful and generic enough to share.
This project allows for more than 'just' an isolated, deterministic DES application, however.  By switching which EventScheduler implementation is used, it is possible to run the exact same code in a relatime production environment or a DES testing environment with a fairly small set of changes.

Still to come: OcavaScenarioTest will contain our Notification-based full-simulation testing framework.  ExampleApp will be added as a demonstration of how the components can be used to control a simple conveyor system.

# Key Classes

NotificationRouter/NotificationBus/Notification - These allow your code to broadcast events around the system such that physical events can be observed at arbitrary points in the system.  At its simplest, this represents an abstraction of the Listener pattern where neither the broadcaster nor the listener need to have any reference to the other.

EventScheduler/TimeProvider - These interfaces are the core of the DES system.  For simple systems, the SimpleDiscreteEventScheduler is sufficient, while for more complex systems with multiple simulated threads, the SourceTrackingEventScheduler works with the NotificationRouter to simulate thread handover between different parts of the system.  The RealtimeEventScheduler can be passed into the system to test real time, multi-threaded instances or to be used in a production environment.
The framework was designed to support a system with a number of isolated, single threaded sub-components communicating via immutable events (Notifications).  It is perfectly possible to use features such as parralel streams to speed up the sub-component processing, or to branch off entire additional threads, and the system is designed to support using Notifications to pass data and control back into the main sub-component thread once a branch has completed its processing.

