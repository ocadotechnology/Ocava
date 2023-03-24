# Ocava

Ocava is an open-source Java project for writing and testing simulations.

## Getting Started

### With Maven

```xml
<dependencyManagement>
    <dependency>
        <groupId>com.ocadotechnology</groupId>
        <artifactId>OcavaScenarioTest</artifactId>
        <version>${ocava.version}</version>
    </dependency>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>com.ocadotechnology</groupId>
        <artifactId>OcavaScenarioTest</artifactId>
    </dependency>
</dependencies>
```

### Example Simulation

[TrafficLightSimulation](TrafficLightSimulation/README.md) uses the scenario test framework to simulate a traffic light
controller in discrete event and realtime.

Once you are familiar with TrafficLightSimulation, take a look at [OcavaScenarioTest](OcavaScenarioTest/README.md)

### Further Documentation

See [Modules](#modules) for more information about each module. For detailed documentation see 
the [wiki](https://github.com/ocadotechnology/Ocava/wiki). 

## Developing

### Build

```bash
mvn verify
```

## Modules

### OcavaCore

The discrete event simulation framework provides tools and utilities for both in and out of simulations.

See [the module README](OcavaCore/README.md) for further information.

### OcavaS3

Utilities to connect to AWS services.

See [the module README](OcavaS3/README.md) for further information.

### OcavaScenarioTest

A scenario test framework for event-driven processes.

See [the module README](OcavaScenarioTest/README.md) for further information.

### SuiteTestEngine

Support for JUnit 5.

See [the module README](SuiteTestEngine/README.md) for further information.

### TrafficLightSimulation

An example project which utilises the framework to create a simulated environment for a traffic light controller.

See [the module README](TrafficLightSimulation/README.md) for further information.

## Contributing

See [CONTRIBUTING](CONTRIBUTING.md).