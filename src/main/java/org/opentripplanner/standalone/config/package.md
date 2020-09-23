# OTP configuration design

This document explains the configuration design.


## Design Goals

The design goals are:
- Load and validate the configuration early, before loading the graph or creating any 
  components/modules. This ensures fast feedback and early termination of the server if the
  configuration is not valid.
- Ignore unknown parameters, just print a warning message in the log. This will make the process 
  of upgrading and downgrading OTP easier, and less prune to service failure. Try to make reasonable
  default values instead of making a parameter required.
- Encapsulate config parsing, so parsing and error handling is consistent - and stay consistent 
  over time(maintenance).
- Configuration should be injected into OTP components/modules in using inversion-of-control. Each
  module should define the needed config as an interface(or simple Java class). This ensures  
  type-safty and provide a consistent way to document needed configuration for each module. NB! The
  `BuildConfig` is not doing correct, 
 

## Implementation

For historic reasons the configuration loader uses jackson and parses the config into a JSON node 
tree. Java objects are mapped explicit by wrapping each `JsonNode` in a `NodeAdapter`. The 
`NodeAdapter` decorate the `JsonNode` to provide type-safe getters for rich basic types, like 
`Enum`, `List`(type-safe), `Map`(type-safe), `LocalDateTime`, `URI` and so on. It also helps with 
validation and providing a list of unused parameters. 


### Config Injection

Each module in OTP witch need configuration should declare what it needs and document each parameter
with using JavaDoc. It can be done using a interface or a simple plain-old-java-object(POJO):

```
/** <Sumary doc goes here> */
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

/** <Sumary doc goes here> */
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
`ModuleNnnnConfig` that either extend the interface above or instantiate the POJO. The
config class should map from JSON using the `NodeAdapter` into itself(extending the interface)
or into the POJO.

The 2 approaches are almost identical, but the interface is a bit more "pure" with respect to the 
responsibilities, while the POJO approach saves a few lines of code.


## Examples

Before you start implementing or injecting config into your new code, take a look at 
`TransitRoutingConfig` or `RouterConfig#mapRoutingRequest(...)` for good examples.    

The root configuration classes `BuildConfig`, `OtpConfig` and `RouterConfig` are **NOT** good examples,
they are not defined in the modules they are used, and they do parse their own config. This creates
cyclic dependencies between the configuration loading and the modules using it. There is an issue 
to (Merge otp-config, router-config and build-config)[https://github.com/opentripplanner/OpenTripPlanner/issues/3020].
When doing this issue, we should move the above classes to the appropriate packages, possibly 
splitting them by their usage. Than parsing and instantiating them should be done in the 
`org.opentripplanner.standalone.config` package.
