# Comparing OTP2 and OTP1

## Summary

OpenTripPlanner has been under development since 2009, leading up to a 1.0 release in 2016. Research and development on higher performance routing has been ongoing since 2013-2014, and work on the second major release referred to as OTP2 officially began in 2018. As of Q3 2020, a release candidate of OTP2 is available and in limited production use. This page explains key differences between the two versions (referred to as OTP1 and OTP2) to help you decide which one to use.

OTP1 has existed for over a decade and is in widespread use. It aims to do many things for many people: it provides passenger-facing itinerary services over APIs, but also serves as a network analysis toolkit for urban planning and research. Though OTP1 is widely used and gets the job done, its transit routing approach is obsolete. We have long recognized that more resource-efficient approaches were possible. Reasonable response times and scaling to larger data sets have been achieved through a series of complex incremental interventions that became difficult to maintain. OTP1 has also accumulated large amounts of experimental code and specialized tools, which can be useful in a research or consulting setting but complicate long-term maintenance.

OTP2 is brand new and still in testing, though based on code and ideas in heavy use for over five years. It offers much better performance in larger transportation networks and geographic areas, and a wider variety of alternative itineraries. OTP2's public transit routing component has been completely rewritten, and is now distinct from bike, walk, and motor vehicle routing. Non-transit routing remains identical to OTP1, benefiting from years of adaptations to nuances of OpenStreetMap data and end-user walking and biking preferences. Unlike OTP1, OTP2 is completely focused on passenger-facing itinerary services. The innovations in OTP2 have already been applied to planning, research, and analysis work for several years through Conveyal's R5 project, which informed and inspired the OTP2 transit routing system. 
 
OTP2 will not supersede OTP1 immediately for all use cases. In some situations there are legitimate reasons to continue using OTP1, or even for new OpenTripPlanner users to adopt OTP1 instead of OTP2. As development work continues over 2021 and additional 2.x releases are made, we expect this gap to close and OTP2 (in combination with other projects) may eventually fully replace OTP1, but this process is expected to take a few years.


## OTP2 Use Cases

The benefits of OTP2 will be most evident in large or dense networks spanning multiple cities: entire countries (Netherlands, Switzerland, Norway), US states, metropolitan regions and cross-border conurbations (e.g. NYC metro area). Although the scale of trip planners is sometimes limited by the geographic extent of administrative structures (national rail or bus operators or ticketing agencies), OTP2 should be capable of handling even larger networks, and we do for example regularly test on a unified Nordic trip planner in hopes that such systems will materialize over time as more territories adopt OTP.

OTP2 development has been driven by adoption of open source routing software in Northern Europe. Importantly for deployments in Europe, OTP2 introduces support for EU-standard Netex and SIRI data sources in addition to GTFS. The Nordic profile of Netex understood by OTP2 uses the same schema as the EU profile, and generalization to the EU profile should be feasible once it is standardized. 


## Choosing between OTP1 and OTP2

Much development effort has gone into OTP2, and most OTP development effort will continue to focus on OTP2 after its release. OTP2 is much more efficient than OTP1 for certain common use cases, providing faster responses for a larger number of simultaneous users over larger geographic areas and more complex transportation networks.

However, this does not mean that all users of OpenTripPlanner should switch to OTP2, or that all new users will want to start with OTP2. As of fall 2020, OTP1 remains much more widely used than OTP2, and most importantly OTP2 has a smaller feature set than OTP1. That is to say, OTP2 can do less things than OTP1, but it does them much more efficiently and tries to cover the most common use cases for large-scale OTP deployments. 

When in doubt, new users are advised to try out OTP2 and switch to OTP1 if they need features that are not available in OTP2. If some feature you need is missing from OTP2, you can also create a new issue or comment on an existing one on GitHub, letting us know why it is important to you. New features can be added to the OTP2 if there is sufficient demand and development resources to maintain them.


## High-level feature comparison

| Feature | OTP1 | OTP2 |
|---------|------|------|
| OSM street data | yes | yes |
| GTFS transit data | yes | yes ([_frequency.txt_ partially supported](https://github.com/opentripplanner/OpenTripPlanner/issues/3243)) |
| Netex transit data | no | yes<br>(Nordic profile) |
| GTFS-Realtime | yes<br>(streaming, polling, incremental) | yes<br>(streaming, polling, incremental) |
| SIRI Realtime | no | yes |
| Elevation data | TIFF and NED | TIFF and NED |
| One-to-many routing,<br> isochrones and scripting | yes | no |
| Java version | 8+ | 11+ |
| Multiple regions per server | yes | no |
| Hot reloading of graphs | yes | no |
| Street (OSM) routing algorithm | Generalized cost A* | Generalized cost A* |
| Transit routing algorithm | Generalized cost A* | Multi-criteria range-RAPTOR |
| Search segmentation | Single search through access, transit, egress | Access/egress separate from transit search |
| Goal direction | Upper bound search backward from destination,<br> over streets and transit, interleaved with forward search | Upper bound search backward from destination <br> on transit only, before forward search begins |
| Alternative itineraries | "Trip banning",<br> N lowest generalized costs | True Pareto-optimal results |
| Departure/arrival time | Single departure or arrival time only | Every minute in a window up to several days long |
| API Paging | no | yes |
| Timetable View | no | yes |
| Plugin Sandbox Extensions | no | yes ([See extensions](SandboxExtension.md)) |
| Data storage | local, S3 (elevation only) | extensible with local, ZIP,<br>and Google Cloud plugins, S3 available |
| Transfer Priority | yes (route, trip, stop level) | no (planned) |
/ REST API format | XML, JSON | JSON only |


## Commentary on OTP1 features removed from OTP2

OTP2 brings significant improvements in speed and scalability, but does not retain all features of OTP1. We have chosen to prioritize long-term maintainability, so only those features that are "owned" by a team of professional developers will be carried over to OTP2. 

Features that have been removed to simplify the code base and improve maintainability may be removed permanently. Other missing features are still priorities for the organization leading OTP2 development (Entur) but have not yet been adapted to the new transit routing system, and will be added in upcoming releases. Some features have been removed to reflect separation of concerns: following principles of modular design they should be handled outside OTP, or are already covered by other projects where they are more actively developed.

### Analysis

Many OpenTripPlanner contributors have been primarily interested in transportation and urban planning use cases. We consider these use cases quite important. This has been a major area of application for OpenTripPlanner and has helped popularize cumulative opportunities accessibility metrics. For example, the University of Minnesota Accessibility Observatory used OpenTripPlanner for [Access Across America](http://access.umn.edu/research/america/). Nonetheless, the analysis code in OTP1 is essentially an unmaintained and unsupported early prototype for later projects, specifically Conveyal's R5 (and the Conveyal Analysis system built upon it). OTP1 seems to have gained popularity for analysis uses due to the existence of documentation and an active user community, but has significant technical shortcomings. One of these is simply speed: OTP1 can be orders of magnitude slower (and more memory-intensive) than the approaches exemplified in R5. The other is the requirement to search at a single specific time. Travel times and especially wait times on scheduled transit vary greatly depending on when you depart. Accounting for variation over a time window requires repeated independent searches at each possible departure time, which is very inefficient. R5 is highly optimized to capture variations in travel time across time windows and account for uncertainty in waiting times on frequency-based routes. 

Due to its similarity to the R5 approach, OTP2's transit router would not have these same problems. Nonetheless, we have decided not to port the 
OTP1 analysis features over to OTP2 since it would broaden the focus away from passenger information and draw finite attention away from existing projects like R5 and Conveyal Analysis. 

Accordingly, we have made an effort to clean up and augment OTP1 analysis documentation for researchers who will continue to need it. 
It should remain possible for people to continue using OTP1 if they prefer.
If you would instead like to apply the innovations present in OTP2, we recommend looking into R5 or Conveyal Analysis.


### Routers API and Hot Reloading

Via it's Routers API, OTP1 allows loading data and serving APIs for multiple separate geographic areas. This is functionally equivalent to running more than one OTP server with separate data sets. This system also allows reloading transportation network data when it changes, or even pushing new data over a network connection.

These were all adaptations to the very different IT environment that existed earlier in OTP history. These days, containerization and on-demand cloud servers have become ubiquitous, and most users solve these problems in totally different ways - by provisioning and starting up entirely new virtual servers, then switching a load balancer over to those new servers. Because the Routers API is complex and exposes potentially damaging functionality over the network, it has been removed from OTP2 to simplify the code base and make it easier to reason about security.

### Routing request parameters

Less parameters are available on the OTP2 REST API than in OTP1. Often there is no practical loss of functionality, just a different way of expressing things due to the new routing algorithms. A summary of parameters that have been removed and their replacements can be found in the migration guide [OTP2-MigrationGuide](OTP2-MigrationGuide.md).


## OTP Trip planning and Transit index APIs

OTP1 have two APIs for trip planning, the REST API and an obsolete GraphQL API(early version of 
the Digitransit GraphQL API). OTP2 still support the REST API and it is very similar in functionality
compared with the OTP1 version. In the future we would like to create a new official OTP API using 
GraphQL replacing the REST API. We will probably support the REST API for a long time to allow 
everyone to migrate to the new GraphQL API. Today, OTP2 comes with two Sandbox extension APIs:

- [HSL Legacy GraphQL API](sandbox/LegacyGraphQLApi.md) - HSL's GraphQL API used by the Digitransit project.
- [Transmodel API](sandbox/TransmodelApi.md) - Entur´s Transmodel API

The plan is to merge the two APIs above, clean it up and make it the new official API. The HSL API
uses GTFS terminology, while the Entur API is Transmodel(NeTEx) based. Both APIs are similar in 
semantics/structure and provide the same functionality. The plan is to merge these to APIs into one
new official OTP2 API. We will then deprecate the REST API, Transmodel API and the HSL API. The new
API will be available in a GTFS and a Transmodel "translated" version.  


## Additional characteristics added in OTP2

**Sandbox Extensions** OTP2's Sandbox system allows for plugins, proprietary extensions, and experimental feature development with less overhead. It forces OTP2 to become more extensible, while reducing process overhead when developing non-core features.

**Cloud support** In OTP1 all data access (config, input data, and graph output) is by direct access to the local filesystem. The only exception is elevation data, which can be loaded from AWS S3 as well. In OTP2, all data access is through an abstraction layer. This can be configured to support individual local files, zip files, and Google Cloud Storage. The new data access treats directories and zip files as “equal”, and this functionality is used to read the contents of GTFS and NeTEx archives. Other data sources can be supported by writing plugins. Entur has written a plugin for AWS S3 which has not been merged. If requested they can provide this code for AWS S3.

**Library upgrades** We have adapted OTP2 to run on Java 11+ and moved to newer versions of some dependencies such as GraphQL and One Bus Away.

**Bugfixes** At least bug issues have been resolved in OTP2. Critical fixes have been backported to OTP1. See https://github.com/issues?q=is%3Aclosed+is%3Aissue+label%3AOTP2+label%3Abug

## Other features removed from OTP2

**AlertPatch** GTFS-RT Service Alerts will no longer affect routing (e.g. cancel trips). A GTFS-RT Trip Updates feed should be used for this purpose.
  
