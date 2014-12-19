<img src="https://github.com/openplans/OpenTripPlanner/wiki/Home/otp_logo_wiki.png" align="right"/>

## OpenTripPlanner
_Note: this wiki is targeted primarily at the OTP development community. For general information about the project, please visit [**opentripplanner.org**](http://www.opentripplanner.org)_

**OpenTripPlanner** (OTP) is an open source multi-modal trip planner, which runs on Linux, Mac, Windows, or potentially any platform with a Java virtual machine. OTP is released under the [LGPL](http://www.gnu.org/licenses/lgpl-3.0.txt) license. As of Summer 2014, the code is under active development with a variety of [deployments](Deployments) around the world, and we are working toward a 1.0 release in the coming year.

**Latest Project Updates:**

 * In 2014 OpenTripPlanner was a focal point in the Dutch Transport Ministry's MMRI (MultiModal Travel Information) project which encouraged investment in trip planning platforms and services. A consortium of five companies worked together on extending OpenTripPlanner to improve performance in large regional transport networks and account for real-time vehicle delay. Plannerstack is now working toward hosting OpenTripPlanner and Bliksem RRRR as services, including high quality open data integration for the Netherlands.

 * OpenTripPlanner has become <a href="http://sfconservancy.org/">Software Freedom Conservancy's</a> thirty-first member project. Conservancy is a non-profit public charity that provides a range of financial and administrative services to member projects that develop Free, Libre, and Open Source Software (FLOSS). By joining Conservancy, OpenTripPlanner obtains the benefits of a formal non-profit organizational structure while keeping the project focused on software development and documentation. See [[SFC]] for more details.

 * Portland's TriMet relaunched its [official trip planner](http://ride.trimet.org), now powered by OTP, on August 6, 2012, following a successful 10-month beta run. Read [more about the project](https://github.com/openplans/OpenTripPlanner/wiki/Portland-Regional-Trip-Planner), and see also TriMet's 2009-2011 [OTP Final Report](https://github.com/openplans/OpenTripPlanner/wiki/Reports/OTP%20Final%20Report%20-%20Metro%202009-2011%20RTO%20Grant.pdf).


## Status

[[OpenTripPlanner|About]] is presently at **version 0.13.0**. See the [[changelog]] and the [[version notes|Version-Notes]] to decide which branch or tag you want to work with.

[![Build Status](http://ci.opentripplanner.org/buildStatus/icon?job=OpenTripPlanner)](http://ci.opentripplanner.org/job/OpenTripPlanner/)

The software currently:

 * Plans multi-modal walking, wheelchair, biking and transit trips
 * Takes travel time, road type/safety, and elevation data into account when planning bike trips, and provides an interface for customizing the weighting of these three factors
 * Shows graphical elevation profiles for bike trips
 * Imports data from GTFS, shapefiles, OpenStreetMap and the National Elevation Dataset
 * Plans trips in about 100ms in a moderate sized city
 * Exposes a RESTful API (XML and JSON), which other apps or front-ends can build on
 * Supports [[GTFS-Realtime]] for service changes and alerts
 * Supports [[Bike rental]] 
 * Supports one-to-many and many-to-many searches for planning, alternatives analysis, and accessibility research purposes.

See the [Milestones](https://github.com/openplans/OpenTripPlanner/issues/milestones) page for more information on what's next.  

See [[NYCPerformance]] for performance characteristics in a larger metropolitan area with extensive transit coverage.

## Basic OTP Architecture

At the core of OpenTripPlanner is a library of Java code that finds efficient paths through multi-modal transportation networks built from [OpenStreetMap](http://wiki.openstreetmap.org/wiki/Main_Page) and [GTFS](https://developers.google.com/transit/gtfs/) data. Several different services are built upon this library:

The **OTP Routing API** is a [RESTful](https://en.wikipedia.org/wiki/Representational_state_transfer) web service that responds to journey planning requests with initineraries in a JSON or XML representation. You can combine this API with OTP's standard Javascript front end to provide users with trip planning functionality in a familiar map interface, or write your own applications that talk directly to the API. This API is provided by a Java [servlet](http://docs.oracle.com/javaee/6/tutorial/doc/bnafd.html) which can be dropped into any [servlet container](http://en.wikipedia.org/wiki/Web_container). In simpler terms, the OTP REST API is provided by a plug-in for standards-compliant Java web servers.

The **OTP Transit Index API** is another [RESTful](https://en.wikipedia.org/wiki/Representational_state_transfer) web service that provides information derived from the input GTFS feed(s). Examples include routes serving a particular stop, upcoming vehicles at a particular stop, upcoming stops on a given trip, etc.

The term "OTP Analyst" refers to parts of OTP that apply the routing engine to transportation network analysis rather than end-to-end trip planning. OTP Analyst includes:

The **OTP Analyst Web Services** provide network analysis results such as travel time maps and isochrones as standard web Mercator tiles or GIS rasters via a [WMS](http://en.wikipedia.org/wiki/Web_Map_Service)-derived API. These web services are conceptually separate from the routing API, but are provided by the same servlet: once you have a working OTP trip planner you can also use it to produce travel time maps and other visualizations of transit service. See [this blog post](http://openplans.org/2012/06/visualizing-urban-accessibility-with-opentripplanner-analyst/) for discussion and examples.

The **OTP Analyst Batch Processor** is a command-line tool that handles more complex one-off network analysis tasks. It uses the same core routing library and data sources as other OTP services, but allows for very open-ended configuration and the inclusion of population or opportunity data. While configuration and use are currently somewhat opaque for non-developers, the "Batch Analyst" is becoming a powerful tool for visualizing how transportation networks affect access to urban opportunities. See [this article](http://www.theatlanticcities.com/commute/2013/01/best-maps-weve-seen-sandys-transit-outage-new-york/4488/) for an example case study on the effects of hurricane Sandy in New York.

## OTP Quick Start

 * [Very basic introduction](Minimal-Introduction)
 * [Advanced introduction](https://github.com/openplans/OpenTripPlanner/wiki/FiveMinutes) *Note that this page does not correspond to the same version of OTP used in the above minimal introduction.* 
 * [Set up a development environment and write some code](Setting-up-a-development-environment)
 * [Available web app language translations](https://github.com/openplans/OpenTripPlanner/wiki/Translation)

## Code Repository and Developer Information

To browse the source online visit https://github.com/openplans/OpenTripPlanner.

To create a local copy of the repository, use the following command:

`$ git clone git://github.com/openplans/OpenTripPlanner.git`

**NOTE** as part of a large project in the Netherlands we are currently restructuring the OpenTripPlanner project to reduce the number of Maven modules, clarify the naming scheme for those modules, and simplify installation and configuration for new users (see "stand-alone mode" below). The documentation in this wiki is therefore not in sync with the `master` branch of OTP (on which we carry out active development). All users and developers referencing this documentation will want to check out the `stable` git branch instead of the `master` branch, as it is quite recent but still follows the old naming scheme and structure.

 * [Setting up a development environment](Setting-up-a-development-environment)
 * [Developers Guide](https://github.com/openplans/OpenTripPlanner/wiki/DevelopersGuide) (code conventions, how to submit patches, etc)
 * [Javadoc documentation for the entire project](http://docs.opentripplanner.org/javadoc/)
 * [Web service API documentation](http://docs.opentripplanner.org/apidoc) (automatically generated by [Enunciate](http://enunciate.codehaus.org/))
 * [[Tutorials]] (complete list of development tutorials)
 * [FAQ](https://github.com/openplans/OpenTripPlanner/wiki/FrequentlyAskedQuestions)
 * Mailing lists:
     - [Developer discussion list](http://groups.google.com/group/opentripplanner-dev)
     - [User discussion list](http://groups.google.com/group/opentripplanner-users)
 * IRC channel:
     - `#opentripplanner` on Freenode
     - You can connect using your favorite IRC client or [chat through the web](http://webchat.freenode.net/?channels=opentripplanner) (just enter a username and click *Connect*)
     - Check out and contribute to [future ideas for the software](https://github.com/opentripplanner/OpenTripPlanner/wiki/Ideas)
 * [[RoutingBibliography]] articles that have informed development of the OTP routing engine.
 * [[AnalyticsBibliography]] articles on non-passenger-facing applications of multi-modal routing engines (including OTP) in urban planning, public policy, and the social sciences.
 * Discussion/details of other [[complex issues|ComplexIssues]] and proposals requiring more space than a single ticket.
 * [[Translation]] on making OpenTripPlanner speak your local language.
 * [[Libraries]] notes on libraries OTP depends on.
 * Here is the [OTP logo](Home/otp_logo.svg) in vector (SVG) format.
 * See schedule/notes for our [[Weekly Check-In Discussions]], held Thursdays at 12:30pm EDT (GMT-4).

## Contact Info

Send questions and comments to the [user mailing list](http://groups.google.com/group/opentripplanner-users).  

Discuss internal developement details on the [dev mailing list](http://groups.google.com/group/opentripplanner-dev).  

Chat with us via IRC on Freenode channel `#opentripplanner` or [chat through the web](http://webchat.freenode.net/?channels=opentripplanner) 

File bug reports via the Github [issue tracker](https://github.com/openplans/OpenTripPlanner/issues). Note that the issue tracker is not intended for support questions or discussions. Please post them to one of the mailing lists instead.

## Stakeholders and User Information

 * [OpenTripPlanner.com](http://opentripplanner.com/) has information for agencies, live demos, and details on commercial support for OTP.
 * [OpenTripPlanner mailing list](http://groups.google.com/group/opentripplanner-users) (for getting help with and discussing the software)
 * [Frequently asked questions (FAQ)](https://github.com/openplans/OpenTripPlanner/wiki/FrequentlyAskedQuestions) (what is a trip planner?  who's behind this?)
 * [Phase One project plan](https://github.com/openplans/OpenTripPlanner/wiki/ProjectPlan)
 * [Kick-off workshop](https://github.com/openplans/OpenTripPlanner/wiki/kick-off-workshop) (notes from the workshop that launched this project)
 * For information about the OTP Workshop held in Portland, OR on July 13-15, 2011, see [[2011 OTP Workshop]]


## Background

The project began a collaborative effort among [TriMet](http://trimet.org), [OpenPlans](http://openplans.org), and the developers of [FivePoints](http://fpdev.org/), [OneBusAway](https://github.com/OneBusAway/onebusaway/wiki) and [Graphserver](http://bmander.github.com/graphserver/), and has since grown to encompass a global community of users and developers. More information on OTP's origins is available at the [Project History](https://github.com/openplans/OpenTripPlanner/wiki/Project-History) page.

In November of 2013, OpenTripPlanner became the thirty-first member project of the <a href="http://sfconservancy.org/">Software Freedom Conservancy.</a>

## Financial Support

OpenTripPlanner is a member project of Software Freedom Conservancy, a 501(c)(3) organization incorporated in New York, and donations made to it are fully tax-deductible to the extent permitted by law. Donations can be made by credit card, wire transfer or paper check. Please contact <accounting@sfconservancy.org> for wire transfer instructions. Please be sure to note in the email what country the wire transfer will initiate from and what currency it will be in. Send paper check donations, drawn in USD and payable to Software Freedom Conservancy, Inc., to:

```
Software Freedom Conservancy, Inc.
137 Montague ST STE 380
Brooklyn, NY 11201-3548 USA
```

Please indicate "directed donation: OpenTripPlanner" in the memo field of the check.