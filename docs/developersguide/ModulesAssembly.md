# WIP This is not done or thought trough


# OTP Application Module Assembly
OTP originally was a Spring application, but we found the Spring Application framework to be overkill for this project. Hence we need to use some guidelines instead to keep the application maintainable.

## Components

The OTP _application_ consists of several *modules* (Graph builder modules, Services, Updaters so on). TODO more...



## Dependency Injection (DI)

The core principle is to separate behaviour from dependency resolution. OTP do not have many Services so we do DI "by hand". We borrow some ideas/naming from Spring and Guice and try to follow the [best practices](BestPractices.md).

TODO: To assembly a module ...
TODO: To inject config ...

#### How do we allow for session/request scoped injection?
We do not keep web-client information in OTP, so there should not be any need for session scoped objects. Request scoped objects are needed, but the need for dependency injection should be limited. 

TODO *Application scoped services* are injected into request scoped objects by ...

TODO *Request scoped objects* are made available to other *request scoped objects* by ...


```
TODO - Clean up this...
The `OTPModules` only exist during start-up; Hence can not be used to inject dependencies at request time. If you need to inject Modules/Services into request scoped objects you have some possibilities:

1. Use a factroy method, and hold all needed dependencies in the module that create the reuest object.
2. Create a `SubModelConfig` that do the creation and dependency injection of components in that module. You should clearly document the scope of the `SubModuleConfig`: Start-up, application(singelton) or request.
3. Create a Factory - if the request object needs to be created in many places. The factory must then hold all application scoped dependencies (router, graphIndex) and the factory should be injected into the clients at construction time. 
```



## Top level building blocks
This are the top level building blocks. Read the JavaDoc on each class for detailed description.

- [OTPMain](../../src/main/java/org/opentripplanner/standalone/OTPMain.java)

  - Parse Command Line Parameters(CLP) 
  - Run/start appropriate modules based on the CLP and the file configuration.
  - Avoid passing the CLPs around (TODO TGR: This needs cleanup, but is easy to do after the multiple router support is removed)

- [OTPModules](../../src/main/java/org/opentripplanner/standalone/OTPModules.java)
The top level module assembly class.
  - Responsible for dependency injection and creating services, build-modules and other shared components.
  - Scope: Startup time only, only OTP Main/tests should have a dependency on this class.

- [OTPConfiguration](../../src/main/java/org/opentripplanner/standalone/config/OTPConfiguration.java)
  - Responsible for reading and parsing configuration files into JSONNodes. TODO describe config injection.

- [OTPServer](../../src/main/java/org/opentripplanner/standalone/OTPServer.java) 
TODO rename/implement OTPAppContext?
  - 

- [OTPWebApplication](../../src/main/java/org/opentripplanner/standalone/OTPWebApplication.java) (JAX-RS Application)
Injected into the WEB Application server, currently this is the embedded GrizzlyServer.

- Router

- GraphIndex




# FAQ

