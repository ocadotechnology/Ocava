# Sequenced Steps Worked Examples

Sequenced steps allow for the execution of multiple sequences of steps, ordered within each sequence
but each sequence is able to run independently of the others.

## Simple case

Imagine a scenario where we have two cars which need to reach a number of junctions in order, but we
do not mind which order the cars pass through each junction.

### Declaration

```
@Test
void scenario() {
  when.simulation.starts();
  
  then.car.sequenced(CAR_1_ROUTE).strictlyNextJunction(JUNCTION_ONE, CAR_1);
  then.car.sequenced(CAR_1_ROUTE).strictlyNextJunction(JUNCTION_TWO, CAR_1);
  then.car.sequenced(CAR_1_ROUTE).strictlyNextJunction(JUNCTION_THREE, CAR_1);
  
  then.car.sequenced(CAR_2_ROUTE).strictlyNextJunction(JUNCTION_ONE, CAR_2);
  then.car.sequenced(CAR_2_ROUTE).strictlyNextJunction(JUNCTION_TWO, CAR_2);
  then.car.sequenced(CAR_2_ROUTE).strictlyNextJunction(JUNCTION_THREE, CAR_2);
  
  then.unordered.waitForSteps(CAR_1_ROUTE, CAR_2_ROUTE);
}
```

Where the steps could be declared as:

```
void strictlyNextJunction(Id<Junction> junctionId, Id<Car> carId) {
  addCheckStep(CarEntersJunctionNotification.class, notification -> {
      if (carId.equals(notification.carId)) {
          Assertions.assertEquals(junctionId, notification.junctionId,
                String.format("Car %s reached junction %s before expected next junction %s",
                    carId, notification.junctionId, junctionId);
          return true;
      }
      return false;
}
```

Note that the step only asserts the next junction for the selected car. If another car arrives, it
will simply return `false`. 

### State after @Test method has executed

After the @Test method is executed, all the events are queued, but no sequenced steps are active.

```
Step queue:
[
1 - Execute Start simulation
3 - Execute Insert Check Car 1 Enters Junction 1
4 - Execute Insert Check Car 1 Enters Junction 2
5 - Execute Insert Check Car 1 Enters Junction 3
6 - Execute Insert Check Car 2 Enters Junction 1
7 - Execute Insert Check Car 2 Enters Junction 2
8 - Execute Insert Check Car 2 Enters Junction 3
9 - Wait for Car 1 and Car 2 
]

Active Sequenced Steps:
{
}
```

### Execution Path

Once the simulation starts to run, the sequenced steps will be queued up:

```
Step queue:
[
1 - Wait for Car 1 and Car 2
]

Active Sequenced Steps:
{
CAR_1_ROUTE : [
    1 - Check Car 1 Enters Junction 1
    2 - Check Car 1 Enters Junction 2
    3 - Check Car 1 Enters Junction 3
]
CAR_2_ROUTE : [
    1 - Check Car 2 Enters Junction 1
    2 - Check Car 2 Enters Junction 2
    3 - Check Car 2 Enters Junction 3 
]
}
```

Now, say that `CAR_2` arrives at the first junction first. The head step for `CAR_1` would be
executed, find that the notification does not match its conditions and return false. Next, the head
step for `CAR_2` would be executed, be matched and removed.

```
Step queue:
[
1 - Wait for Car 1 and Car 2
]

Active Sequenced Steps:
{
CAR_1_ROUTE : [
    1 - Check Car 1 Enters Junction 1
    2 - Check Car 1 Enters Junction 2
    3 - Check Car 1 Enters Junction 3
]
CAR_2_ROUTE : [
    1 - Check Car 2 Enters Junction 2
    2 - Check Car 2 Enters Junction 3 
]
}
```

Say that next, `CAR_2` arrives at junction 2.  The same would happen (note that the head step of
`CAR_1`'s sequence is written to not fail on the other car arriving at the "wrong" junction) and 
`CAR_2`'s sequence would move on to wait for the car to arrive at the third junction

```
Step queue:
[
1 - Wait for Car 1 and Car 2
]

Active Sequenced Steps:
{
CAR_1_ROUTE : [
    1 - Check Car 1 Enters Junction 1
    2 - Check Car 1 Enters Junction 2
    3 - Check Car 1 Enters Junction 3
]
CAR_2_ROUTE : [
    1 - Check Car 2 Enters Junction 3 
]
}
```

Next, let's say that `CAR_1` reaches all 3 junctions in quick succession. At each stage, the
Notification would be offered first to `CAR_1`'s head step, which would be completed, and the
notification would be removed. `CAR_2`'s head step would not see these notifications at all.

```
Step queue:
[
1 - Wait for Car 1 and Car 2
]

Active Sequenced Steps:
{
CAR_2_ROUTE : [
    1 - Check Car 2 Enters Junction 3 
]
}
```

Finally, `CAR_2` arrives at the last junction, which would allow the wait step to complete.
If there were subsequent steps in the test, those could now be processed.

## Advanced case

It has been noted that the modifier `sequenced` can be applied to a range of steps other than then
(check) steps. Let us consider a case where a sequence contains a when (execute) step which causes
`CAR_2` to divert to a garage.

### Declaration

```
@Test
void scenario() {
  when.simulation.starts();
  
  then.car.sequenced(CAR_1_ROUTE).strictlyNextJunction(JUNCTION_ONE, CAR_1);
  then.car.sequenced(CAR_1_ROUTE).strictlyNextJunction(JUNCTION_TWO, CAR_1);
  
  then.car.never().reachesJunction(JUNCTION_TWO, CAR_2)
  then.car.sequenced(CAR_2_ROUTE).strictlyNextJunction(JUNCTION_ONE, CAR_2);
  when.car.sequenced(CAR_2_ROUTE).triggerEngineFailure(CAR_2)
  then.car.sequenced(CAR_2_ROUTE).afterAtLeast(10, MINUTES).reachesGarage(CAR_2);
  
  then.unordered.waitForSteps(CAR_1_ROUTE, CAR_2_ROUTE);
}
```

Note that the `never()` step does not use the same check step. This is because the assertion within
the strict check step would trigger and incorrectly fail the test, as will be explained below.

### State after @Test method has executed

After the @Test method is executed, all the events are queued, but no sequenced steps are active.

```
Step queue:
[
1 - Execute Start simulation
3 - Execute Insert Check Car 1 Enters Junction 1
4 - Execute Insert Check Car 1 Enters Junction 2
5 - Execute Insert Check Never Car 2 Reaches Junction 2
6 - Execute Insert Check Car 2 Enters Junction 1
7 - Execute Insert Execute Car 2 Engine Failure
8 - Execute Insert Check Car 2 Reaches Garage After At Least 10 Minutes
9 - Wait for Car 1 and Car 2 
]

Active Unordered Steps:
[
]

Active Sequenced Steps:
{
}
```

### Execution Path

Once the simulation starts to run, the sequenced steps will be queued up:

```
Step queue:
[
1 - Wait for Car 1 and Car 2
]

Active Unordered Steps:
[
1 - Check Never Car 2 Reaches Junction 2
]

Active Sequenced Steps:
{
CAR_1_ROUTE : [
    1 - Check Car 1 Enters Junction 1
    2 - Check Car 1 Enters Junction 2
]
CAR_2_ROUTE : [
    1 - Check Car 2 Enters Junction 1
    2 - Execute Car 2 Enters Junction 2
    3 - Check Car 2 Reaches Garage After At Least 10 Minutes
]
}
```

Say, as before, `CAR_2` reaches the first junction first.
- The never step would receive this notification. If it was using a strict check, it would see that the "correct" car had reached the "wrong" junction and trigger a failure. With the less strict check step, it merely does not complete.
- The head step for `CAR_1` would then receive this notification, and not complete because the car does not match.
- The head step for `CAR_2` would then receive the notification and complete.

```
Step queue:
[
1 - Wait for Car 1 and Car 2
]

Active Unordered Steps:
[
1 - Check Never Car 2 Reaches Junction 2
]

Active Sequenced Steps:
{
CAR_1_ROUTE : [
    1 - Check Car 1 Enters Junction 1
    2 - Check Car 1 Enters Junction 2
]
CAR_2_ROUTE : [
    1 - Execute Car 2 Enters Junction 2
    2 - Check Car 2 Reaches Garage After At Least 10 Minutes
]
}
```

Since the head step of `CAR_2`'s sequence is now an Execute step, it completes immediately, and the
timer for the following check step is started.

```
Step queue:
[
1 - Wait for Car 1 and Car 2
]

Active Unordered Steps:
[
1 - Check Never Car 2 Reaches Junction 2
]

Active Sequenced Steps:
{
CAR_1_ROUTE : [
    1 - Check Car 1 Enters Junction 1
    2 - Check Car 1 Enters Junction 2
]
CAR_2_ROUTE : [
    1 - Check Car 2 Reaches Garage After At Least 10 Minutes
]
}
```

Car 1 can then proceed to reach each junction in turn, sending each notification as before

```
Step queue:
[
1 - Wait for Car 1 and Car 2
]

Active Unordered Steps:
[
1 - Check Never Car 2 Reaches Junction 2
]

Active Sequenced Steps:
{
CAR_2_ROUTE : [
    1 - Check Car 2 Reaches Garage 
]
}
```

Finally, Car 2 broadcasts the appropriate notification to indicate that it has reached the garage.
If not enough time has passed to satisfy the `afterAtLeast()` modifier, the test will fail here.
Otherwise, it will continue and the step will be marked as complete.

```
Step queue:
[
]

Active Unordered Steps:
[
1 - Check Never Car 2 Reaches Junction 2
]

Active Sequenced Steps:
{
}
```

At this stage, the only remaining active step is the `never()` step. Since these are marked as not
required to complete within the framework, the test would be completed.