# OTP configuration design

The OTP configuration model is responsible for loading the config from the file system. Other OTP
modules should not depend on the configuration, but instead define interfaces for the needed config.
The config model can then implement these interfaces and Dagger can inject them.


## Design Goals

The design goals are:

- Load and validate the configuration early, before loading the graph or creating any
  components/modules. This ensures fast feedback and early termination of the server if the
  configuration is not valid.
- Ignore unknown parameters, just print a warning message in the log. This will make the process of
  upgrading and downgrading OTP easier and less prune to service failure. Try to make reasonable
  default values instead of making a parameter required.
- Encapsulate config parsing, so parsing and error handling is consistent - and stay consistent over
  time(maintenance).
- Configuration should be injected into OTP components/modules using inversion-of-control. Each
  module should define the needed config as an interface(or simple Java class). This ensures
  type-safety and provides a consistent way to document needed configuration for each module.
- For Sandbox modules the configuration loading should be put in the
  `org.opentripplanner.standalone.config.sandbox` package. This keeps all the configuration loading
  in one place, avoiding fragmentation and makes it easier to get an overview. The interface with 
  the Sandbox parameters should be declared in the Sandbox module.


## Implementation

The configuration loader uses jackson and parses the config into a JSON node tree. Java objects are
mapped explicit by wrapping each `JsonNode` in a `NodeAdapter`. The `NodeAdapter` decorates the 
`JsonNode` to provide type-safe getters for rich basic types like `Enum`, `List`(type-safe), 
`Map`(type-safe), `LocalDateTime`, `URI` and so on. It also helps with validation and providing a 
list of unused parameters.


### Config Injection

Each OTP module in need for configuration should declare needs and document each parameter using
JavaDoc. It can be done using an interface or a simple plain-old-java-object(POJO):

```
/** <Summary doc goes here> */
interface <ModuleNnnnParameters> {

   /** 
    * <Parameter doc goes here>  
    *
    * This is an optional parameter, if not set the default is to ....
    */
   @Nullable
   LocalDateTime  firstDayOfService() = null; 
}

or 

/** <Summary doc goes here> */
public class <ModuleNnnnParameters> {
    <fields>

   /** 
    * <Parameter doc goes here>  
    *
    * This is an optional parameter, if not set the default is to ....
    */
   @Nullable
   LocalDateTime  firstDayOfService() { ... } 
}
```

In the `org.opentripplanner.standalone.config` package there will be a class called
`ModuleNnnnConfig` that either extends the interface above or instantiates the POJO. The config
class should map from JSON using the `NodeAdapter` into itself(extending the interface)
or into the POJO.

The 2 approaches are almost identical, but the interface is a bit more "pure" with respect to the
responsibilities, while the POJO approach saves a few lines of code.

## Default values - in code, config and APIs

We prefer optional parameters using default values over required parameters in APIs and in the 
configuration. Most API request parameters have default values defined in config. So, lets 
illustrate the prefered way of doing this.

We define a parameter for a feature inside the domain module. This is also where we define the 
static "global" default value. This makes the default value available, not only to the domain,
configuration and APIs - but in tests as well.

```Java
package o.o.mydomain.mysubdomain;

class MyComponentParameters(foo : String = "BAR")
```

Then, in the configuration we used the default for documentation and to set the configuration 
"default".

```Java
package o.o.standalone.config.mydomain;

class MyComponentConfig {
  final String foo;

  MyComponentConfig(NodeAdapter c) {
    var dft = new MyComponentParameters();
    this.foo = c.of("fo").asString(dft.foo());
    :
```

Last, we use the configured value in the API - both for documentation and as the default value 
for the api input parameter:

```Java
var dft = [injected configuration object];
:
GraphQLArgument.newArgument().name("foo").defaultValue(dft.foo())
```

This show how the API gets the default value from the configured value, and how we fall back to a
static value set in code, if no configuration is available. There are some cases with are more 
complicated than this, but this is the general pattern for doing it. This also makes the default 
value available in the configuration and API documentation. An important note is that in the 
API we show the configured value as the default.


## Examples

Before you start implementing or injecting config into your new code, take a look at
`TransitRoutingConfig` or `RouterConfig#mapRoutingRequest(...)` for good examples.

The root configuration classes `BuildConfig`, `OtpConfig` and `RouterConfig` are **NOT** good
examples, they are not defined in the modules they are used, and they do parse their own config.
This creates cyclic dependencies between the configuration loading and the modules using it. There
is an issue to (Merge otp-config, router-config and
build-config)[https://github.com/opentripplanner/OpenTripPlanner/issues/3020]. When doing this
issue, we should move the above classes to the appropriate packages, possibly splitting them by
their usage. Then parsing and instantiating them should be done in the
`org.opentripplanner.standalone.config` package.
