# OTP Architecture

OTP is developed over more than 10 years, and most of the design documentation is in the code as
comments and JavaDoc. Over the years the complexity have increased, and the natural developer
turnover creates a demand for more architecture and design documentation. The new OTP2 documentation
is put together with the source; hopefully making it easier to maintain. Instead of documenting
modules in old style _package-info.java_ files we use _package.md_ files. This document should serve
as an index to all existing top-level documented components.

This document is far from complete - hopefully it can evolve over time and become a good
introduction to OTP architecture. The OTP project GitHub issues are a good place to look for
detailed discussions on many design decisions.

Be sure to also read the [developer documentation](docs/Developers-Guide.md).

## Modules/Components

The diagram shows a simplified/generic version on how we want to model the OTP components with 2 
examples. The Transit model is more complex than the VehiclePosition model.   

![MainModelOverview](docs/images/ServiceModelOverview.png)

 - `Use Case Service` A service which combine the functionality in many `Domain Services` to fulfill
   a use-case or set of features. It may have an api with request/response classes. These are 
   usually stateless; Hence the `Use Case Service` does normally not have a model. The implementing
   class has the same name as the interface with prefix `Default`.
 - `Domain Model` A model which encapsulate a business area. In the drawing two examples are shown,
   the `transit` and `vhicleposition` domain model. The transit model is more complex so the 
   implementation have a separate `Service` and `Repository`. Almost all http endpoints are , 
   read-only so the `Service` can focus on serving the http API endpoints, while the repository
   is used to maintain the model by the updaters. 

> **Note!** The above is the goal, the current package structure needs cleanup.


Below is a list of documented components in OTP. Not every component is documented at a high level,
but this is a start and we would like to expand this list in the future.

### [OTP Configuration design](src/main/java/org/opentripplanner/standalone/config/package.md)

The Configuration module is responsible for loading and parsing OTP configuration files and map them
into Plan Old Java Objects (POJOs). These POJOs are injected into the other components.

### [REST API](src/main/java/org/opentripplanner/api/package.md)

Short introduction to the REST API.

### [GTFS import module](src/main/java/org/opentripplanner/gtfs/package.md)

Used to import GTFS transit data files.

### [NeTEx import module](src/main/java/org/opentripplanner/netex/package.md)

Used to import NeTEx transit data files.

### Transit Routing

#### [Raptor transit routing](src/main/java/org/opentripplanner/raptor/package.md)

This is the OTP2 new transit routing engine implemented using the Raptor algorithm. It explains how
Raptor works, the important concepts and the design. It might be worth reading even if you are not a
developer - just to understand how the transit routing works.

The Raptor functionality is quite complex, so we want to isolate it from the remaining code.
Therefore, the raptor component is designed to have as few dependencies as possible. In fact there
are _no_
dependencies from Raptor to other parts of OTP code, only to utility classes not found in the JDK.
Also, the code follows a stricter object-oriented design, than most other parts of OTP. The Raptor
implementation is highly critical code, hence we set the bar higher with respect to code quality.

OTP provides transit data to Raptor by implementing the _raptor/api/transit_ model. The
[RoutingService](src/main/java/org/opentripplanner/routing/RoutingService.java)
is responsible for mapping from the OTP context to a
[RaptorRequest](src/main/java/org/opentripplanner/raptor/api/request/RaptorRequest.java)
and then map the
result, [Raptor Path](src/main/java/org/opentripplanner/raptor/api/path/Path.java), back to
the OTP internal domain. This might seem like a lot of unnecessary mapping, but mapping is simple -
routing is not.

The performance of Raptor is important, and we care about every millisecond. All changes to the
existing Raptor coded should be tested with the
[SpeedTest](src/test/java/org/opentripplanner/transit/raptor/speed_test/package.md) and compared
with an earlier version of the code to make sure the performance is NOT degraded.

#### [Transfer path optimization](src/main/java/org/opentripplanner/routing/algorithm/transferoptimization/package.md)

Describes the transfer functionality, the design and the implementation. The logic for finding the
best transfer is distributed to the Raptor and
the [OptimizeTransferService](src/main/java/org/opentripplanner/routing/algorithm/transferoptimization/OptimizeTransferService.java)
.

#### [Itinerary list filter chain](src/main/java/org/opentripplanner/routing/algorithm/filterchain/package.md)

Describes the itinerary list filter chain, used to post-process the itineraries returned from the
routers in [RoutingWorker](src/main/java/org/opentripplanner/routing/algorithm/RoutingWorker.java),
in order to sort and reduce the number of returned itineraries. It can also be used to decorate the
returned itineraries, especially if it requires more complex calculations, which would be unfeasible
to do during the routing process.

### [Service](src/main/java/org/opentripplanner/service/package.md)
The service package contains small services usually specific to one or a few use-cases. In contrast 
to a domain model they may use one or many domain models and other services. 