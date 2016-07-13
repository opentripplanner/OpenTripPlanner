<img src="https://github.com/opentripplanner/OpenTripPlanner/wiki/Home/otp_logo_wiki.png" align="right"/>

## OpenTripPlanner
_Note: this documentation is targeted primarily at the OTP development community and more technical users. For high-level information about the project, please visit [**opentripplanner.org**](http://www.opentripplanner.org)_

**OpenTripPlanner** (OTP) is an open source multi-modal trip planner, which runs on Linux, Mac, Windows, or potentially any platform with a Java virtual machine. OTP is released under the [LGPL](http://www.gnu.org/licenses/lgpl-3.0.txt) license. The code is under active development with a variety of [deployments](Deployments) around the world.

If you want to get started right away running your own OTP instance, the best place to start is the [Basic Usage](Basic-Usage) page.

**Latest Project Updates**

 * After seven years of hard work, a **1.0 release is planned for August of 2016**! OTP is in feature freeze, with all work concentrating on bug fixes and API cleanup.
 
 * The Helsinki Regional Transport Authority (HSL) [trip planner](https://digitransit.fi/en/) based on OpenTripPlanner is in public beta as of spring 2016. Source code for their new UI is [available on Github](https://github.com/HSLdevcom/digitransit-ui).

 * As of 2015, OTP powers the New York State department of transportation's [transit trip planner](http://511ny.org/tripplanner/default.aspx).
   It provides itineraries for public transit systems throughout the state in a single unified OTP instance.

 * In November of 2014, Arlington, Virginia launched a new [commute planning site](http://mobilitylab.org/2014/11/07/the-who-what-when-where-whys-of-carfreeatoz/) for the Washington, DC metropolitan area.
 It depends on OpenTripPlanner to weigh the costs and benefits of various travel options, making use of [profile routing](http://conveyal.com/blog/2015/02/24/what-is-profile-routing).

 * OpenTripPlanner was a focal point in the Dutch Transport Ministry's MMRI (MultiModal Travel Information) project which encouraged investment in trip planning platforms and services. A consortium of five companies worked together to improve OpenTripPlanner performance in large regional transport networks and account for real-time service modifications and delays. The resulting [Plannerstack Foundation](http://www.plannerstack.org/)
 is now providing OpenTripPlanner and [Bliksem RRRR](https://github.com/bliksemlabs/rrrr) as hosted services including high quality open data integration for the Netherlands.

 * OpenTripPlanner has become <a href="http://sfconservancy.org/">Software Freedom Conservancy's</a> thirty-first member project. Conservancy is a non-profit public charity that provides a range of financial and administrative services to member projects that develop Free, Libre, and Open Source Software. By joining Conservancy, OpenTripPlanner obtains the benefits of a formal non-profit organizational structure while keeping the project focused on software development and documentation.

 * TriMet relaunched Portland Oregon's [official trip planner](http://ride.trimet.org) which is now powered by OTP. Read [more about the project](https://github.com/openplans/OpenTripPlanner/wiki/Portland-Regional-Trip-Planner), and see also TriMet's 2009-2011 [OTP Final Report](https://github.com/opentripplanner/OpenTripPlanner/wiki/Reports/OTP%20Final%20Report%20-%20Metro%202009-2011%20RTO%20Grant.pdf).


## Status

[![Build Status](https://travis-ci.org/opentripplanner/OpenTripPlanner.svg?branch=master)](https://travis-ci.org/opentripplanner/OpenTripPlanner)

After seven years of hard work, a **1.0 release of OTP is planned for August of 2016**! OTP is in feature freeze, with all work concentrating on bug fixes and API cleanup. See the [changelog](Changelog) and the [version notes](Version-Notes) to decide which branch or tag you want to work with. The software currently:

 * Plans multi-modal walking, wheelchair, bicycle and transit trips
 * Takes travel time, road type, safety, and elevation into account, and allows users to customize the weighting of these three factors
 * Shows graphical elevation profiles for bike trips
 * Imports data from GTFS, OpenStreetMap, and digital elevation models
 * Typically provides multiple itineraries in a fraction of a second even in large metropolitan networks
 * Exposes a web services API which other apps or front-ends can build on
 * Supports [GTFS-Realtime](https://developers.google.com/transit/gtfs-realtime/) for service changes and alerts in both polling (pull) and streaming (push) modes
 * Supports bike rental with dynamic availability information, as well as park-and-ride and kiss-and-ride
 * Supports one-to-many and many-to-many searches for planning, alternatives analysis, and accessibility research purposes

See the [Milestones](https://github.com/opentripplanner/OpenTripPlanner/milestones) page to explore upcoming developments.


## Basic OTP Architecture

At the core of OpenTripPlanner is a library of Java code that finds efficient paths through multi-modal transportation networks built from [OpenStreetMap](http://wiki.openstreetmap.org/wiki/Main_Page) and [GTFS](https://developers.google.com/transit/gtfs/) data. Several different services are built upon this library:

The **OTP Routing API** is a [RESTful](https://en.wikipedia.org/wiki/Representational_state_transfer) web service that responds to journey planning requests with itineraries in a JSON or XML representation. You can combine this API with OTP's standard Javascript front end to provide users with trip planning functionality in a familiar map interface, or write your own applications that talk directly to the API.

The **OTP Transit Index API** is another [RESTful](https://en.wikipedia.org/wiki/Representational_state_transfer) web service that provides information derived from the input GTFS feed(s). Examples include routes serving a particular stop, upcoming vehicles at a particular stop, upcoming stops on a given trip, etc.

The term "OTP Analyst" refers to parts of OTP that apply the routing engine to transportation network analysis rather than end-to-end trip planning. OTP Analyst includes:

The **OTP Analyst Web Services** provide network analysis results such as travel time maps and isochrones as standard web Mercator tiles or GIS rasters via a [WMS](http://en.wikipedia.org/wiki/Web_Map_Service)-derived API. These web services are conceptually separate from the routing API, but are provided by the same servlet: once you have a working OTP trip planner you can also use it to produce travel time maps and other visualizations of transit service. See [this blog post](http://conveyal.com/blog/2012/07/02/analyst) for discussion and examples.

The **OTP Analyst Batch Processor** is a command-line tool that handles more complex one-off network analysis tasks. It uses the same core routing library and data sources as other OTP services, but allows for very open-ended configuration and the inclusion of population or opportunity data. While configuration and use are currently somewhat opaque for non-developers, the "Batch Analyst" is becoming a powerful tool for visualizing how transportation networks affect access to urban opportunities. See [this article](http://www.theatlanticcities.com/commute/2013/01/best-maps-weve-seen-sandys-transit-outage-new-york/4488/) for an example case study on the effects of hurricane Sandy in New York.

The **OTP Scripting API** allow the execution of routing requests from within scripts (such as _Python_). It is composed of a stable internal API, and an embedded Jython interpreter. It can be used in different contexts, such as batch analysis or automated regression testing. [More information here](Scripting).

## Additional Documentation

 * [Basic Usage (get started quickly)](Basic-Usage)
 * [Internationalization and Translations](https://github.com/opentripplanner/OpenTripPlanner/blob/master/README_LOCALIZATION.md)
 * [Developers Guide](Developers-Guide) (code conventions, how to submit patches, etc)

Some other types of documentation are generated for OTP from its source code:

 * [Javadoc documentation for the entire project](http://dev.opentripplanner.org/javadoc/)
 * [Web service API documentation](http://dev.opentripplanner.org/apidoc/) (automatically generated by [Enunciate](http://enunciate.codehaus.org/))


## Contact Info

Send questions and comments to the [user mailing list](http://groups.google.com/group/opentripplanner-users).
Discuss internal development details on the [dev mailing list](http://groups.google.com/group/opentripplanner-dev).
File bug reports via the Github [issue tracker](https://github.com/openplans/OpenTripPlanner/issues). Note that the issue tracker is not intended for support questions or discussions. Please post them to one of the mailing lists instead.


## Background

The project began a collaborative effort among [TriMet](http://trimet.org), [OpenPlans](http://openplans.org), and the developers of FivePoints, [OneBusAway](https://github.com/OneBusAway/onebusaway/wiki) and [Graphserver](http://bmander.github.com/graphserver/), and has since grown to encompass a global community of users and developers. More information on OTP's origins is available at the [Project History](History) page.
In November of 2013, OpenTripPlanner became the thirty-first member project of the <a href="http://sfconservancy.org/">Software Freedom Conservancy.</a>

## Financial Support

OpenTripPlanner is a member project of Software Freedom Conservancy, a 501(c)(3) organization incorporated in New York, and donations made to it are fully tax-deductible to the extent permitted by law. Donations can be made by credit card, wire transfer or paper check. Please contact <accounting@sfconservancy.org> for instructions. Be sure to note in the email what country the wire transfer will initiate from and what currency it will be in.
