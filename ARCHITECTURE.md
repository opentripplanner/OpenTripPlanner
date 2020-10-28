# OTP Architecture
OTP is developed over more than 10 years, and most of the design documentation is in the code 
as comments and JavaDoc. Over the years the complexity have increased, and the natural developer 
turnover naturally creat a demand for more architecture and design documentation. The new OTP2 
documentation is put together with the source; hopefully making it easier to maintain. Instead of
documenting modules in old style _package-info.java_ files we use _package.md_ files. This document
should serve as an index to all existing top-level documented components.

This document is fare from complete - hopefully it can evolve over time and become a good 
introduction to OTP architecture. The OTP project GitHub issues are a good place look for detailed 
discussions on many design decisions.   

Be sure also the read the [developer documentation](docs/Developers-Guide.md). 
 
## Modules/Components
Below is a list of documented components in OTP. Not every component is documented at a high level,
but this is a start, and we would like to expand this list in the future.

### [OTP Configuration design](src/main/java/org/opentripplanner/standalone/config/package.md)

The Configuration module is responsible for loading and parsing OTP configuration files and map 
them into POJOs. These POJOs are injected into the other components.


### [REST API](src/main/java/org/opentripplanner/api/package.md)

Short introduction to the REST API.


### [GTFS import module](src/main/java/org/opentripplanner/gtfs/package.md)

Used to import GTFS transit data files.


### [NeTEx import module](src/main/java/org/opentripplanner/netex/package.md)

Used to import NeTEx transit data files.


### [Raptor transit routing](src/main/java/org/opentripplanner/transit/raptor/package.md)

This is the OTP2 new transit routing engine implemented using the Raptor algorithm. It explains how
Raptor works, the important concepts, and the design. It might be worth reading even if you are not
a developer - just to understand how the transit routing works. 

The Raptor functionality is quite complex, so we want to isolate the problem. Therefore, is the 
raptor component designed to have as few dependencies as possible. In fact there is _no_ 
dependencies from Raptor to other parts of OTP code, only to utility classes not found in the JDK.
Also, the code follows a stricter object-oriented design, than most other parts of OTP. The Raptor 
implementation is highly critical code, hence we set the bar higher with respect to code quality.
 
OTP provide transit data to Raptor by implementing the _raptor/api/transit_ model. The 
[RoutingService](src/main/java/org/opentripplanner/routing/RoutingService.java)
is responsible for mapping from the OTP context to a 
[RaptorRequest](src/main/java/org/opentripplanner/transit/raptor/api/request/RaptorRequest.java)
and then map the result, [Raptor Path](src/main/java/org/opentripplanner/transit/raptor/api/path/Path.java),
back to the OTP internal domain. This might seem like a lot of unnecessary mapping, but 
mapping is simple - routing is not. 

The performance of Raptor is important, and we care about every millisecond. All changes to the
existing Raptor coded should be tested with the 
[SpeedTest](src/test/java/org/opentripplanner/transit/raptor/speed_test/package.md) and 
compared with an earlier version of the code to make sure the performance is NOT degraded. 
