# OcavaScenarioTest

This component provides a scenario test framework designed to work with an event-driven process.
The framework enables scenario tests to be written in code rather than in text which allows for both type safety and easy code re-use between scenario tests.

## Step creation

Steps are created together during the execution of the Test method, but before any of the simulation objects have been created or run.  This means that some care must be taken to ensure any lookups or queries must be done inside the runnable method passed into the step.

All ordered (default) steps are queued to be executed sequentially.

`CheckSteps` (almost everything inside a `then` object) are triggered by any received notification of the correct type.  As soon as they are completed, the next step becomes available for execution.

`ExecuteSteps` (almost everything inside a `when` object) are run immediately after the previous step is complete.  In this way, `then` steps can be used to gate the execution of later steps in the test eg

```
then.carStopsAtIntersection()
when.lightTurnsGreen()
```

could be used to assert that expected behaviour is shown before changing the state of the system to start the next part of the test.

## Unordered check steps

CheckSteps steps can be decorated with a few modification methods to change their behaviour.  Most of these decorations will make the step behave as an Unordered step.  Unordered steps can occur in any order after the point they are declared in the test. They will not block execution of other steps, but most categories must complete for the test to pass. The only exception are steps decorated with the `never()` modification method, which are required to not complete.

### Sequential creation of unordered steps

As noted above, unordered steps are created differently from normal CheckSteps. It is important to understand that an unordered step only becomes active after the previous ordered steps complete.

As a result, unordered step only receives notifications from the point of time where this step is located in the scenario in relation to ordered steps.

I.e., for unordered step to receive all notifications which happen in a scenario, it has to be defined before all of the ordered steps.

If  any ordered steps are defined before unordered step, then this unordered step will not receive any notifications which happen before all prior ordered steps complete.

Consider the following scenario
```
then.notification2Received()
then.unordered().countNotifications()
```
with a simulation which emits 4 notifications in this order:
```
notification1
notification2
notification3
notification4
```
The unordered step will miss out all notifications up to and including `notification2` (which is the last notification 
received by prior ordered steps) and will only receive `notification3` and `notification4`.

If we change the scenario to become
```
then.unordered().countNotifications()
then.notification2Received()
```
then the unordered step will receive all 4 notifications, and ordered step will receive its expected notification `notification2`.

Another example: the scenario
```
then.eventOccurs()
then.never().eventOccurs()
```
would validate that the event happens exactly once.

### Propagation of notifications between unordered steps

If a notification makes a unordered step complete successfully, then the notification will be destroyed and not passed to
any other unordered step.

If this step is not completed after processing a notification, then the notification will be passed through to
the next unordered step.

## Mutable state in Then steps

We do not recommend storing mutable data as fields in Then steps. If you do, please be aware of the following.

In the [traffic simulation example](TrafficLightSimulation/) included in this library, `TrafficSimulationStory` class creates `given`, `when` and `then` objects
in the constructor, which means these objects are created once per test class and are preserved between
individual scenario tests defined in the same test class
(for example, between `scenarioWithWaitForSteps` and `scenarioWithoutWaitForSteps` in `TrafficLightsChangeTest` class).

It would therefore be the responsibility of the Story class to ensure that these objects are tidied up between test invocations.
A `@BeforeEach` call in the domain-specific Story superclass may be a suitable approach.

On the other hand, when any modifiers are used, such as `unordered()`, `never()`,
`within()`, `failingStep()`, `passAllNotifications()` etc., the value returned by 
this modifier function is a new object which by default does not preserve user-defined mutable
state which could be present in the original object.

To preserve the state (in this example, `myUsefulMutableDataObject`) when using modifiers, pass it to the constructor
in `create` function:
```
 @Override
    protected TrafficLightThenSteps create(StepManager stepManager, NotificationCache notificationCache, CheckStepExecutionType checkStepExecutionType) {
        return new TrafficLightThenSteps(stepManager, notificationCache, checkStepExecutionType, myUsefulMutableDataObject);
    }
```