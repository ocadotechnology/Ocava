# Unordered Steps Worked Examples

## Simple case

Unordered steps are a way of asserting that a thing happens, without asserting that it happens before or after other steps.

### Declaration

Consider an example where we want to validate that three cars pass through a junction in any order:

```
@Test
void scenario() {
  when.simulation.starts();
  
  then.trafficLight.changesColour(JUNCTION_ID, GREEN)

  then.car.unordered().entersJunction(JUNCTION_ID, CAR_1);
  then.car.unordered().entersJunction(JUNCTION_ID, CAR_2);
  then.car.unordered().entersJunction(JUNCTION_ID, CAR_3);
}
```

Where the steps could be declared as:

```
void changesColour(Id<Junction> junctionId, LightColour colour) {
  addCheckStep(TrafficLightChangedNotification.class, notification -> {
        junctionId.equals(notification.junctionId) && colour.equals(notification.lightColour));
}

void entersJunction(Id<Junction> junctionId, Id<Car> carId) {
  addCheckStep(CarEntersJunctionNotification.class, notification -> {
      junctionId.equals(notification.junctionId) && carId.equals(notification.carId));
}
```

Note that in this case, there is no assertion in the check steps.

### State after @Test method has executed

After the @Test method is executed, all the events are queued, but no unordered steps are active.

```
Step queue:
[
1 - Execute Start simulation
2 - Check Light turns green
3 - Execute Insert Check Car1 Enters Junction
4 - Execute Insert Check Car2 Enters Junction
5 - Execute Insert Check Car3 Enters Junction
]

Active Unordered Steps:
[
]
```

### Execution Path

Once the simulation starts to run, it will wait for the traffic light to change colour:

```
Step queue:
[
1 - Check Light turns green
2 - Execute Insert Check Car1 Enters Junction
3 - Execute Insert Check Car2 Enters Junction
4 - Execute Insert Check Car3 Enters Junction
]

Active Unordered Steps:
[
]
```

After the light changes, the next two Execute steps are immediately executed, activating the unordered check steps:

```
Step queue:
[
]

Active Unordered Steps:
[
1 - Check Car1 Enters Junction
2 - Check Car2 Enters Junction
3 - Check Car3 Enters Junction
]
```

Now, say that `CAR_2` arrives first. In this case, the first unordered check step would receive the notification first.
* The first step would determine that it is not completed by this notification, and allow the notification to be passed on.
* The second step would determine that it *is* completed by this notification, and it is removed.
* Because the notification has completed one step, it is not passed to the third step at all.

```
Step queue:
[
]

Active Unordered Steps:
[
1 - Check Car1 Enters Junction
2 - Check Car3 Enters Junction
]
```

Once all three cars have entered the junction, the last step completes and the test passes. 