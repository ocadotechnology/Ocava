# Config System

The Ocava Config system is a powerful set of tools for defining, managing
and loading configuration into a program. It provides type-safe config parsing
(including allowing for custom parsers), comprehensive namespace definition and
various other features which have proved very useful within Ocado.

## Defining Config Keys

All Config keys used by this system are defined within nested enum classes (by
convention named `*Config.java`), allowing for clear namespace definition. For a
trivial real-world example see 
[LocalFileConfig](../fileaccess/local/LocalFileConfig.java),
or for a more complex example case,
[TrafficConfig](../../../../../../../TrafficLightSimulation/src/main/java/com/ocadotechnology/trafficlights/TrafficConfig.java).

All classes defined within the outer Config classes must themselves be enum classes,
and they can define further classes within themselves.

As an example to be used in the rest of this document, you might have:

```java
public enum VehicleConfig {
  ABSOLUTE_SPEED_LIMIT,
  RESPECT_TRAFFIC_LIGHTS;
  
  public enum CarSimulation {
    NUMBER_OF_WHEELS,
    RECOGNISED_MODELS;   

    public enum MovementSpecification {
      MAX_SPEED,
      TURNING_CIRCLE,
      MAX_ACCELERATION,
      MAX_BRAKING_DECELERATION;
    }
  }
  
  public enum MotorcycleSimulation {
    NUMBER_OF_WHEELS;
  }
}
```

### SecretConfig

SecretConfig is an annotation which can be added to any Config key to exclude it
from several batch-exposure methods eg the Config class’s `toString` method will
not mention any keys annotated with `@SecretConfig`

## Constructing Config Objects

For trivial cases the `SimpleConfigBuilder` can be quite useful. It provides a
stripped-down API which can be quite useful when writing unit tests of classes
which take a Config object as a parameter.

For most use cases, the `ConfigManager.Builder` is more appropriate, as it provides
a far more complete API. Below follows a description of a “standard” approach to
using this class.

### Command-Line Argument Parsing

The constructor of the `ConfigManager.Builder` can take an array of command line
arguments. These are processed for configuration data and will always take
precedence over any other specified values.

Individual values can be specified with a `-O` prefix. Eg
`-OVehicleConfig.CarSimulation.NUMBER_OF_WHEELS=3`

Whole properties files can be specified with the prefix `-a`. Eg
`-apath/to/my/file.properties` where the file contains key-value pairs in the form

```properties
VehicleConfig.ABSOLUTE_SPEED_LIMIT=60,KILOMETERS,HOURS
VehicleConfig.MotorcycleSimulation.NUMBER_OF_WHEELS=1
```

Multiple files can be specified by using a `;` separated list of file locations.

Within the list of arguments, if there are duplicate values specified, values in
later files override those from earlier files, single values override values from
files and later single values override earlier single values.

### Length and Time Units

It is possible, though not required, to specify the time and length units your
system works in. When you later come to extract values, such as the speed limit
above, the Config object will convert the input into the units you specified.

Input values can be specified by providing the required units in the format:

```properties
VehicleConfig.ABSOLUTE_SPEED_LIMIT=60,KILOMETERS,HOURS
```

and will default to meters and seconds if not specified. System
units have no default, and calling methods which require them will cause a runtime
exception if they are not specified.

Permitted time units are defined in the java
built-in [TimeUnit](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/TimeUnit.html)
class. Permitted length units are defined in the Ocava
[LengthUnit](../physics/units/LengthUnit.java) class 

### Loading Config From Files

Properties files can be loaded via the `loadConfigFromFiles`
`loadConfigFromLocalResources` and `loadConfigFromResourceOrFile` methods,
depending on whether the file is specified with a full file path or as a packaged
resource.

As with the initial arguments, later files, and later invitations of these methods,
override earlier ones.

These methods also specify the Config enum classes which should have data in these
files.

### Building

The `build` method then returns a `ConfigManager` object which holds a `Config`
object for each top-level enumeration class specified to the builder.

By default, the `build` method will throw a `ConfigKeysNotRecognisedException` if
any of the inputs did not align with recognised enum keys. This especially protects
users from mis-typed configuration. Eg if a user typed "VehilceConfig", the value
would not match, and a runtime exception will inform the user as early as possible.

Putting all of this together, you could have something like:

```java
public static void main(String[] args) throws IOException, ConfigKeysNotRecognisedException {
  ConfigManager configManager = new ConfigManager.Builder(args)
      .loadConfigFromResourceOrFile(ImmutableList.of("defaultConfig.properties", "knightrider.properties"), ImmutableSet.of(VehicleConfig.class))
      .build();
}
```

## Extracting Values from Config Objects

After the Config objects have been created, it is then possible to extract the
loaded values in a type-safe way.

There are three methods to start this process:

- `getValue` throws a `ConfigKeyNotFoundException` if there is no key/value mapping 
for the requested key, or returns a value parser using the married string.
- `getIfValueDefined` throws a `ConfigKeyNotFoundException` if there is no
key/value mapping for the requested key, and sets its value parser to return
`Optional.EMPTY` if the mapped value is an empty string.
- `getIfKeyAndValueDefined` sets it's value parser to return `Optional.EMPTY` if
there is no key/value mapping for the requested key or the mapped value is an 
empty string.

These three methods allow the calling code to make assertions about which config
values must be defined, and allow the various override methods to set a value as
"undefined" if desired.

The value parser classes which are returned by these methods then allow the values
to be converted into different types (eg `asDouble`, `asBoolean`), to be
interpreted using the time and length units provided on conduction (`asSpeed`,
`asLength`) or even to be interpreted into connections (`asList`, `asMultimap`).
It is also possible to invoke custom parsers.

Even some simpler built-in parsers have additional functionality included,
such as "MAX" mapping to `Integer.MAX_VALUE`, all of which is detailed in the
javadoc for each method.

### Advanced features

Over time, we have discovered additional use-cases which have not been covered by
the functionality above, and so we have added optional features which can be used
to augment the features above.

[Modular Config Files](ModularConfig.md) - organising large data sets

Prefixed Config - adding multiple variations of the same data (docs under construction)
