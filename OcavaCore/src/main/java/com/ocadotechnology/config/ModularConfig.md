# Modular Configuration Files

We have found that, for our larger and more complex systems, the deployment-specific
configuration files we were using had become large and unwieldy, requiring us to
either manage large files with duplicated configuration between them, or many 
separate files which required us to remember the full set of files required for each
deployment.

To simplify this, we developed a system of "modular" config files.

## The Basics - File Hierarchy

Continuing the examples in the [Basic Config](Config.md) document, imagine that
we find that we need different config for our Knightrider simulations.

knightrider_chase.properties
```properties
VehicleConfig.ABSOLUTE_SPEED_LIMIT=150,KILOMETERS,HOURS
VehicleConfig.CarSimulation.NUMBER_OF_WHEELS=4
...
```

knightrider_conversation.properties
```properties
VehicleConfig.ABSOLUTE_SPEED_LIMIT=30,KILOMETERS,HOURS
VehicleConfig.CarSimulation.NUMBER_OF_WHEELS=4
...
```

As you can see, there is some config which is shared between these files. With
Modular config we can extract the shared elements into a parent file:

knightrider.properties
```properties
VehicleConfig.ABSOLUTE_SPEED_LIMIT=60,KILOMETERS,HOURS
VehicleConfig.CarSimulation.NUMBER_OF_WHEELS=4
...
```

and then declare the extension in the child files, eg:

knightrider_chase.properties
```properties
EXTENDS knightrider

VehicleConfig.ABSOLUTE_SPEED_LIMIT=150,KILOMETERS,HOURS
```

As you can see, the child file may override config from the parent file, but
otherwise inherits the properties of the parent file unchanged.
The `EXTENDS` line should contain the path or resource location of
the parent file, minus the `.properties` suffix. That suffix is
required to be present for the modular config loading to work.
The parent file could itself have an `EXTENDS` line, and as long
as there are no circular references, this can continue as deep as
desired.

## Advanced Usage - Multiple Inheritance

It is also possible to have multiple inheritances.  Say we had the
following files:

knightrider_chase.properties
```properties
VehicleConfig.ABSOLUTE_SPEED_LIMIT=150,KILOMETERS,HOURS
VehicleConfig.RESPECT_TRAFFIC_LIGHTS=FALSE
VehicleConfig.CarSimulation.NUMBER_OF_WHEELS=4
...
```

knightrider_conversation.properties
```properties
VehicleConfig.ABSOLUTE_SPEED_LIMIT=30,KILOMETERS,HOURS
VehicleConfig.RESPECT_TRAFFIC_LIGHTS=TRUE
VehicleConfig.CarSimulation.NUMBER_OF_WHEELS=4
...
```

a_team_chase.properties
```properties
VehicleConfig.ABSOLUTE_SPEED_LIMIT=150,KILOMETERS,HOURS
VehicleConfig.RESPECT_TRAFFIC_LIGHTS=FALSE
VehicleConfig.CarSimulation.NUMBER_OF_WHEELS=3
...
```

We might choose to extract a common knightrider.properties file
as well as a common chase.properties file:

knightrider.properties
```properties
VehicleConfig.ABSOLUTE_SPEED_LIMIT=60,KILOMETERS,HOURS
VehicleConfig.CarSimulation.NUMBER_OF_WHEELS=4
...
```

chase.properties
```properties
VehicleConfig.ABSOLUTE_SPEED_LIMIT=150,KILOMETERS,HOURS
VehicleConfig.RESPECT_TRAFFIC_LIGHTS=FALSE
```

And then we could re-write knightrider_chase.properties as

```properties
EXTENDS knightrider, chase

VehicleConfig.ABSOLUTE_SPEED_LIMIT=150,KILOMETERS,HOURS
```

Note that where both of the parent files define a config value,
it is necessary for the child file to specify an override. There
is no defined system of which parent file takes precedence over
the other. No conflict will be detected if the value is equal in
both parent files.