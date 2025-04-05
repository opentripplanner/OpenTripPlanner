# Comparing OTP2 and OTP1

## Summary

OpenTripPlanner has been under development since 2009, leading up to a 1.0 release in 2016. Research
and development on higher performance routing has been ongoing since 2013-2014, and work on the
second major release referred to as OTP2 officially began in 2018. As of Q3 2023, OTP2 is the only
version to receive regular updates and OTP1 is considered legacy and unsupported. This page explains 
key differences between the two versions (referred to as OTP1 and OTP2). 

OTP1 has existed for over a decade and is in widespread use. It aims to do many things for many
people: it provides passenger-facing itinerary services over APIs, but also serves as a network
analysis toolkit for urban planning and research. Though OTP1 is widely used and gets the job done,
its transit routing approach is obsolete. We have long recognized that more resource-efficient
approaches were possible. Reasonable response times and scaling to larger data sets have been
achieved through a series of complex incremental interventions that became difficult to maintain.
OTP1 has also accumulated large amounts of experimental code and specialized tools, which can be
useful in a research or consulting setting but complicate long-term maintenance.

OTP2 offers much better performance in larger transportation networks and geographic areas, and
a wider variety of alternative itineraries. OTP2's public transit routing component has been
completely rewritten, and is now distinct from bike, walk, and motor vehicle routing. Non-transit
routing remains very similar to OTP1, benefiting from years of adaptations to nuances of OpenStreetMap
data and end-user walking and biking preferences. Unlike OTP1, OTP2 is completely focused on
passenger-facing itinerary services. The innovations in OTP2 have already been applied to planning,
research, and analysis work for several years through Conveyal's R5 project, which informed and
inspired the OTP2 transit routing system.

## OTP2 Use Cases

The benefits of OTP2 will be most evident in large or dense networks spanning multiple cities:
entire countries (Netherlands, Switzerland, Norway), US states, metropolitan regions and
cross-border conurbations (e.g. NYC metro area). Although the scale of trip planners is sometimes
limited by the geographic extent of administrative structures (national rail or bus operators or
ticketing agencies), OTP2 should be capable of handling even larger networks, and we do for example
regularly test on a unified Nordic trip planner in hopes that such systems will materialize over
time as more territories adopt OTP.

OTP2 development has been driven by adoption of open source routing software in Northern Europe.
Importantly for deployments in Europe, OTP2 introduces support for EU-standard Netex and SIRI data
sources in addition to GTFS. The Nordic profile of Netex understood by OTP2 uses the same schema as
the EU profile, and generalization to the EU profile should be feasible once it is standardized.

## High-level feature comparison

| Feature                                           | OTP1                                                                                                        | OTP2                                                                                            |
|---------------------------------------------------|-------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------|
| OSM street data                                   | yes                                                                                                         | yes                                                                                             |
| GTFS transit data                                 | yes                                                                                                         | yes                                                                                             |
| Netex transit data                                | no                                                                                                          | yes<br>(Nordic profile)                                                                         |
| GTFS-Realtime                                     | yes<br>(streaming, polling, incremental)                                                                    | yes<br>(streaming, polling, incremental)                                                        |
| SIRI Realtime                                     | no                                                                                                          | yes                                                                                             |
| Elevation data                                    | TIFF and NED                                                                                                | TIFF and NED                                                                                    |
| One-to-many routing,<br> isochrones and scripting | yes                                                                                                         | no                                                                                              |
| Isochrones                                        | yes                                                                                                         | yes                                                                                             |
| Java version                                      | 8+                                                                                                          | 21+                                                                                             |
| Multiple regions per server                       | yes                                                                                                         | no                                                                                              |
| Hot reloading of graphs                           | yes                                                                                                         | no                                                                                              |
| Street (OSM) routing algorithm                    | Generalized cost A*                                                                                         | Generalized cost A*                                                                             |
| Transit routing algorithm                         | Generalized cost A*                                                                                         | Multi-criteria range-RAPTOR                                                                     |
| Search segmentation                               | Single search through access, transit, egress                                                               | Access/egress separate from transit search                                                      |
| Goal direction                                    | Upper bound search backward from destination,<br> over streets and transit, interleaved with forward search | Upper bound search backward from destination <br> on transit only, before forward search begins |
| Alternative itineraries                           | "Trip banning",<br> N lowest generalized costs                                                              | True Pareto-optimal results                                                                     |
| Departure/arrival time                            | Single departure or arrival time only                                                                       | Every minute in a window up to several days long                                                |
| API Paging                                        | no                                                                                                          | yes                                                                                             |
| Timetable View                                    | no                                                                                                          | yes                                                                                             |
| Plugin Sandbox Extensions                         | no                                                                                                          | yes ([See extensions](SandboxExtension.md))                                                     |
| Data storage                                      | local, S3 (elevation only)                                                                                  | extensible with local, ZIP,<br>and Google Cloud plugins, S3 available                           |
| Transfer Priority                                 | yes                                                                                                         | yes                                                                                             |

## Commentary on OTP1 features removed from OTP2

OTP2 brings significant improvements in speed and scalability, but does not retain all features of
OTP1. We have chosen to prioritize long-term maintainability, so only those features that are 
"owned" by a team of professional developers will be carried over to OTP2.

Some features have been removed to simplify the code base and improve maintainability.
Others have been removed to reflect separation of concerns:
following principles of modular design they should be handled outside OTP, or are already covered by
other projects where they are more actively developed.

### Analysis

From the beginning of the project, many OTP contributors and users have used OTP in research,
analysis, and planning applications. They have prototyped many ideas within the OTP codebase, 
including one-to-many searches producing travel time grids, isochrones, and access-to-opportunities
metrics. While some of these features may still be available as optional "sandbox" features in OTP2, they are unsupported and may be removed in the near future.

Most work of this kind moved over separate projects focused on urban planning and analytics. As of 
version 2, OTP has chosen to focus entirely on passenger information rather than analytics
applications. See the [Analysis](Analysis.md) page for more details.

### Routers API and Hot Reloading

Via its Routers API, OTP1 allows loading data and serving APIs for multiple separate geographic
areas. This is functionally equivalent to running more than one OTP server with separate data sets.
This system also allows reloading transportation network data when it changes, or even pushing new
data over a network connection.

These were all adaptations to the very different IT environment that existed earlier in OTP history.
These days, containerization and on-demand cloud servers have become ubiquitous, and most users
solve these problems in totally different ways - by provisioning and starting up entirely new
virtual servers, then switching a load balancer over to those new servers. Because the Routers API
is complex and exposes potentially damaging functionality over the network, it has been removed from
OTP2 to simplify the code base and make it easier to reason about security.

## OTP Trip planning and Transit index APIs

OTP1 had two APIs for trip planning, the REST API and an GraphQL API (early version of the
GTFS GraphQL API). This API has been formalised and is now, together with the Transmodel 
GraphQL API, the only supported way of sending requests to the OTP routing engine.

Details of those two APIs are available at the following pages:

- [GTFS GraphQL API](apis/GTFS-GraphQL-API.md) - HSL's GraphQL API used by the Digitransit
  project.
- [Transmodel API](apis/TransmodelApi.md) - Entur's Transmodel API

The plan is to merge the two APIs above, clean it up and make it the new official API. The HSL API
uses GTFS terminology, while the Entur API is Transmodel (NeTEx) based. Both APIs are similar in
semantics and structure, and provide the same functionality. 

The REST API of OTP1 has been permanently removed in 2025.

## Additional characteristics added in OTP2

**Sandbox Extensions** OTP2's Sandbox system allows for plugins, proprietary extensions, and
experimental feature development with less overhead. It forces OTP2 to become more extensible, while
reducing process overhead when developing non-core features.

**Cloud support** In OTP1 all data access (config, input data, and graph output) is by direct access
to the local filesystem. The only exception is elevation data, which can be loaded from AWS S3 as
well. In OTP2, all data access is through an abstraction layer. This can be configured to support
individual local files, zip files, and Google Cloud Storage. The new data access treats directories
and zip files as “equal”, and this functionality is used to read the contents of GTFS and NeTEx
archives. Other data sources can be supported by writing plugins. Entur has written a plugin for AWS
S3 which has not been merged. If requested they can provide this code for AWS S3.

**Library upgrades** We have adapted OTP2 to run on Java 11+ and moved to newer versions of some
dependencies such as GraphQL and One Bus Away.

**Bugfixes** At least bug issues have been resolved in OTP2. Critical fixes have been backported to
OTP1. See https://github.com/issues?q=is%3Aclosed+is%3Aissue+label%3AOTP2+label%3Abug

## Other features removed from OTP2

**AlertPatch** GTFS-RT Service Alerts will no longer affect routing (e.g. cancel trips). A GTFS-RT
Trip Updates feed should be used for this purpose.
  
## Migration guide

The development community has maintained a migration guide from version 1.5.0 up to 2.2.0 when
it was no longer feasible to document every change as version 2 was considered the only supported one.
The document can be found in the [documentation of version 2.2.0](https://docs.opentripplanner.org/en/v2.2.0/OTP2-MigrationGuide/).
