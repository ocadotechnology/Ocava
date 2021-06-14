# Scenario Test Worked Examples

## Simple case

### Declaration
Consider a simple test to check that some error behaviour is handled correctly by your system.
A simple Scenario Test might take the form:

```
@Test
void scenario() {
    when.simulation.starts();
    then.event.startupEventOccured();

    when.system.triggerErroneousBehaviour(TEST_SPECIFIC_PARAMETERS);
    then.errorHandling.errorIsGracefullyHandled(MORE_TEST_PARAMETERS);
}
```

### State after @Test method has executed

Once the test method has been executed, all the steps have been queued, but none of them have been executed.
The step queue can be envisioned in the form:

```
Step queue:
[
1 - Execute Start simulation
2 - Check Startup Complete Notification
3 - Execute Error triggers
4 - Check Error Handled Notification
]
```

### Execution Path

Immediately after the @Test method is executed, the framework will begin to execute these events in order.
Almost all `when` steps are Execute steps which run as soon as they are at the top of the list.
Almost all `then` steps are Check steps which wait for a matching Notification to be received.

First, the framework will execute the start event, then wait for the startup complete notification to arrive.

```
Step queue:
[
1 - Check Startup Complete Notification
2 - Execute Error triggers
3 - Check Error Handled Notification
]
```

Once this happens, the check step completes and the following execute step is executed *immediately*:

```
Step queue:
[
1 - Check Error Handled Notification
]
``` 

And finally, when the error handled Notification is received and verified, the step queue is empty and the test has passed. 

## Step Future

See [here](EXAMPLE_STEP_FUTURES.md)

## Unordered Steps

See [here](EXAMPLE_UNORDERED_STEPS.md)