# JUnit5 Suite Engine

Motivation for this project is the (currently) incomplete suite support of the JUnit 5 Platform.  

If you are interested about the details of implementing your own Test Engine, check the relevant section of the [JUnit 5 Guide](https://junit.org/junit5/docs/current/user-guide/#launcher-api-engines-custom).

## What it does

If found at runtime by the JUnit 5, it will find classes annotated with `@DynamicTestSuite` and execute the method defined by the
`@SelectFromMethod`. Method should be `static` and return something we can iterate over - collection, stream of array.

Classes returned by method are scanned for JUnit 5 tests. Tests found this way are executed.

```
@DynamicTestSuite
@SelectFromMethod(name="suite")
class SomeDynamicTestSuite {

    static List<Class> suite() {
        return Arrays.asList(FirstTestClass.class, SecondTestClass.class);
    }
} 
```

**Run with Maven:**

```xml
<profiles>
    <profile>
      <id>FirstSuite</id>
      <build>
        <plugins>
          <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <configuration>
              <skipTests>false</skipTests>
              <includes>
                <include>**/SomeDynamicTestSuite.java</include>
              </includes>
            </configuration>
          </plugin>
        </plugins>
      </build>
    </profile>
  </profiles>
```

Use `mvn test -P FirstSuite` to run the above.

**Run from your IDE:**

`@DynamicTestSuite` is recognized as a Test executable by your IDE.

## Templating All Tests
The `SuiteEngine` also has some configurable functionality to wrap all tests in a template. Test methods and
parameterized test methods can be wrapped in templates. The use case this was introduced for was to configure
tests suites to run tests repeatedly with different random seeds configured. An example of this use case can be
found in `RepeatingTestSuiteEngineTest`. 
 
The templating can be done without modifying any test classes or suite classes. In order to do this, a Junit test
extension is needed. The extension should implement `TestTemplateInvocationContextProvider` and the
`TestTemplateInvocationContextModifierExtension`, the latter of which is defined by this library. The
`TestTemplateInvocationContextProvider` interface defines how to template a test method whereas the
`TestTemplateInvocationContextModifierExtension` defines how to template an already templated method. The
`RepeatingTestSuiteEngineTest` shows how you can implement these methods in order to repeat tests. You can then use
other extension callbacks such as `BeforeEachCallback` to modify each test execution

In order to run a suite with your extension you need register it. You can do with with config either via VM
property or by Junit config file
`resources/junit-platform.properties`. The config required is :
* `ocadotechnology.junit5.suite.engine.templateAllTests=true`
    *  This tells the SuiteEngine to wrap all tests in templates

You also need to register your extension with tests. In order to do this automatically across all tests you can set
the config `junit.jupiter.extensions.autodetection.enabled=true` and place your extension with package name inside 
`resources/META-INF/services/org.junit.jupiter.api.extension.Extension`

## Templating Some Tests
The `ocadotechnology.junit5.suite.engine.templateAllTests=true` config is convenient if all tests in a given profile are parametrized, but if you would like with a single run to execute multiple Suites, some of which non-parametrized like so:

```xml
<profiles>
    <profile>
      <id>AllSuites</id>
      <build>
        <plugins>
          <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <configuration>
              <skipTests>false</skipTests>
              <includes>
                <include>**/*DynamicTestSuite.java</include>
              </includes>
            </configuration>
          </plugin>
        </plugins>
      </build>
    </profile>
  </profiles>
```

You can manually mark your classes with `@ExtendWith(SomeClassExtension.class)` where `SomeClassExtension` extends `com.ocadotechnology.junit5.suite.TestSuiteTemplateExtension`.

Example - try running the `AllSuites` profile, check out the classes in both suites, some of which are extending `RepeatingTestRandomSeedScenarioExtension` which extends `TestSuiteTemplateExtension`.

## Get it

**Maven:**
```xml
<dependency>
  <groupId>com.ocadotechnology.junit5</groupId>
  <artifactId>suite-engine</artifactId>
  <version>{suite-engine.version}</version>
  <scope>test</scope>
</dependency>
```

**Gradle:**

```
testCompile "com.ocadotechnology.junit5:suite-engine:${suite-engine.version}"
```

## Contributing

This is an early version, designed to solve a very specific problem. It is not complete and maybe it will not match your specific requirements. 

Any feedback, features or code are welcome.