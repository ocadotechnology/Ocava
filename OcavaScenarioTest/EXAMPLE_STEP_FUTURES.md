# Step Future Worked Examples

## Simple case

StepFutures are a useful tool for extracting data from one step and making it available to a subsequent step.
They are usually returned by then steps where the specifics of an event are not important, but the consistency of subsequent events is important.

### Declaration

Consider an example where we want to validate that the first car to enter a junction is
the same car which exits it:

```
@Test
void scenario() {
  when.simulation.starts();

  StepFuture<Id<Car>> carId = then.car.entersJunction(JUNCTION_ID);
  then.car.exitsJunction(JUNCTION_ID, carId);
}
```

The steps could be declared as:

```
StepFuture<Id<Car>> entersJunction(Id<Junction> junctionId) {
  MutableStepFuture<Id<Car>> future = new MutableStepFuture<>();
  addCheckStep(CarEntersJunctionNotification.class, notification -> {
    if (junctionId.equals(notification.junctionId)) {
      future.populate(notification.carId);
    }
    return false;
  });
  return future;
}

void exitsJunction(Id<Junction> junctionId, StepFuture<Id<Car>> carId) {
  addCheckStep(CarExitsJunctionNotification.class, notification -> {
    if (junctionId.equals(notification.junctionId)) {
      Assertions.assertEquals(carId.get(), notification.carId);
      return true;
    }
    return false;
  });
}
```

### State after @Test method has executed

After the @Test method is executed, there is no data stored in the StepFuture, as no steps have been executed.
This means that any code attempting to query or directly access the value of a StepFuture must be called from a step's runnable.

```
Step queue:
[
1 - Execute Start simulation
2 - Check Car Enters Junction
3 - Check Car Exits Junction
]

carId=[]
```

### Execution Path

Once the simulation starts to run, it will wait for the first car to enter the junction:

```
Step queue:
[
1 - Check Car Enters Junction
2 - Check Car Exits Junction
]

carId=[]
```

After a car enters, it will wait for the car to exit:

```
Step queue:
[
1 - Check Car Exits Junction
]

carId=[CAR]
```

Now, the carId StepFuture is populated, and can be queried.
If the wrong car exits the junction, the step would fail with an AssertionError.
Otherwise, when the correct car exits, the last step completes and the test passes.

## Manipulating a StepFuture

Sometimes checking consistency requires manipulating the returned data. This is supported via the `map` function.

### Declaration

Consider a test where two cars are known to the scenario, and we want to validate that one car and then the other enters a junction:

```
@Test
void scenario() {
  when.simulation.starts();

  StepFuture<Id<Car>> carId = then.car.entersJunction(JUNCTION_ID);
  StepFuture<Id<Car>> otherCarId = carId.map(id -> id.equals(CAR_1) ? CAR_2 : CAR_1);
  then.car.entersJunction(JUNCTION_ID, carId);
}
```

Where the second method could be declared as:

```
void entersJunction(Id<Junction> junctionId, StepFuture<Id<Car>> carId) {
  addCheckStep(CarEntersJunctionNotification.class, notification -> {
    if (junctionId.equals(notification.junctionId)) {
      Assertions.assertEquals(carId.get(), notification.carId);
      return true;
    }
    return false;
  });
}
```

The map function is used to create a new StepFuture which wraps the original with a mapping function.

### State after @Test method has executed

As before, neither of the StepFutures would be populated prior to executing the steps:

```
Step queue:
[
1 - Execute Start simulation
2 - Check Car Enters Junction
3 - Check Other Car Enters Junction
]

carId=[]
otherCarId=[]
```

### Execution Path

The test starts the same way, starting the simulation then waiting for the first car to enter:

```
Step queue:
[
1 - Check Car Enters Junction
2 - Check Other Car Enters Junction
]

carId=[]
otherCarId=[]
```

When a car (say `CAR_2`) enters the junction, the original StepFuture is populated, and the mapped instance is also considered populated:

```
Step queue:
[
1 - Check Other Car Enters Junction
]

carId=[CAR_2]
otherCarId=[CAR_1]
```

If CAR_2 enters the junction again, the test would fail with an AssertionError, otherwise when CAR_1 enters the junction, the final step completes and the test passes. 