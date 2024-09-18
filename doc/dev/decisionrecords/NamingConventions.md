# Naming Conventions

In general, we use American English. We use the GTFS terminology inside OTP as the transit domain
specific language. In cases where GTFS does not provide an alternative we use NeTEx. The naming
should follow the Java standard naming conventions. For example a "real-time updater" class
is named `RealTimeUpdater`. If in doubt check the Oxford Dictionary(American).


## Packages

Try to arrange code by domain functionality, not technology. The main structure of a package should
be `org.opentripplanner.<domain>.<component>.<sub-component>`.

| Package                         | Description                                                                                                                                                                                              |
| ------------------------------- |----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `o.o.<domain>`                  | At the top level we should divide OTP into "domain"s like `apis`, `framework`, `transit`, `street`, `astar`, `raptor`, `feeds`, `updaters`, and `application`.                                           |
| `component` and `sub-component` | A group of packages/classes which naturally belong together, think aggregate as in Domain Driven Design.                                                                                                 |
| `component.api`                 | Used for components to define the programing interface for the component. If present, (see Raptor) all outside dependencies to the component should be through the `api`.                                |
| `component.model`               | Used to create a model of a Entites, ValueObjects, ++. If exposed outside the component you should include an entry point like `xyz.model.XyzModel` and/or a Service (in api or component root package). |
| `component.service`             | Implementation of the service like `DefaultTransitService`, may also contain use-case specific code. Note, the Service interface goes into the component root or `api`, not in the service package.      |
| `component.configure`           | Component creation/orchestration. Put Dependency Injection code here, like the Dagger Module.                                                                                                            |
| `support`                       | Sometimes domain logic get complicated, then extracting/isolating it helps. `support` is used internally in a component, not outside.                                                                    |
| `framework`                     | (Abstract) building blocks internal to a domain/parent package. In some cases accessed outside the component, e.g. `OptAppException`, `TransitEntity`.                                                   |
| `mapping`                       | Map between two domains/components.                                                                                                                                                                      |
| `util`                          | General "util" functionality, often characterized by `static` methods. Dependencies to other OTP packages is NOT allowed, only 3rd party utils libs.                                                     |
| `o.o.apis`                      | OTP external endpoints. Note! Many apis are in the Sandbox where they are in the `o.o.ext` package.                                                                                                      |

> **Note!** The above is the goal, the current package structure needs cleanup.

> **Note!** Util methods depending on an OTP type/component should go into that type/component, not in the
utils class. E.g. static factory methods. Warning the "pure" utilities right now are placed into
sub-packages of `o.o.util`, the root package needs cleanup.


## Methods

Here are a list of common prefixes used, and what to expect.

| Good method prefixes                                  | Description                                                                 |
|-------------------------------------------------------|-----------------------------------------------------------------------------|
| `stop() : Stop`                                       | Field accessor, equivalent to `getStop` as in the Java Bean standard        |
| `getStop(ID id) : Stop`                               | Get Stop by ID, throws exception if not found                               |
| `getStops(Collection<ID> id) : List/Collection<Stop>` | Get ALL Stops by set of IDs, throws exception if not found                  |
| `findStop(Criteria criteria) : Optional<Stop>`        | Find one or zero stops, return `Optional`                                   |
| `findStops(Criteria criteria) : List/Stream<Stop>`    | Find 0, 1 or many stops, return a collection or stream(List is preferred)   |
| `listStops() : List/Stream<Stop>`                     | List ALL stops in context, return a collection or stream(List is preferred) |
| `withStop(Stop stop) : Builder`                       | Set stop in builder, replacing existing value and return `this` builder     |
| `initStop(Stop stop) : void`                          | Set property ONCE, a second call throws an exception                        |
| `addStop(Stop stop) : void/Builder`                   | Add a stop to a collection of stops.                                        |
| `addStops(Collection<Stop> stops) : void/Builder`     | Add set of stops to existing set.                                           |
| `withBike(Consumer<BikePref.Builder> body) : Builder` | For nested builders use lambdas.                                            |

These prefixes are also "allowed", but not preferred - they have some kind of negative "force" to them.

| Ok method prefixes, but ...                 | Description                                                                                           |
| ------------------------------------------- | ----------------------------------------------------------------------------------------------------- |
| `withStops(Collection<Stop> stops) : this`) | Replace all stops in builder with new set, consider using `addStops(...)` instead                     |
| `setStop(Stop stop)`                        | Set a mutable stop reference. Avoid if not part of natural lifecycle. Use `initStop(...)` if possible |
| `getStop() : Stop`                          | Old style accessor, use the shorter form `stop() : Stop`                                              |

## Service, Model and Repository

![MainModelOverview](../images/ServiceModelOverview.png)



Naming convention for builders with and without a context.

#### Graph Build and tests run without a context

```Java
// Create a new Stop
trip = Trip.of(id).withName("The Express").build();

// Modify and existing stop
stop = stop.copyOf().withPrivateCode("TEX").build();
```
