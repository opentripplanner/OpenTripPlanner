## Overview

OpenTripPlanner (OTP) is an open source multi-modal trip planner. It depends on open data in open standard file formats (GTFS and OpenStreetMap), and includes a REST API for journey planning as well as a map-based Javascript client. OpenTripPlanner can also create travel time contour visualizations and compute accessibility indicators for planning and research applications. For more information, see the project website: http://opentripplanner.org

The main Java server code is in `src/main/`. OTP also includes a Javascript client based on the Leaflet mapping library in `src/client/`. The Maven build produces a JAR file at `target/otp-VERSION.jar` containing all necessary code and dependencies to run OpenTripPlanner.

Additional information and instructions are available in the [main documentation](http://opentripplanner.readthedocs.org/en/latest/), including a 
[quick introduction](http://opentripplanner.readthedocs.org/en/latest/Basic-Usage/).

[![Build Status](https://travis-ci.org/opentripplanner/OpenTripPlanner.svg?branch=master)](https://travis-ci.org/opentripplanner/OpenTripPlanner)

## Development 

OpenTripPlanner is a collaborative project incorporating code, translation, and documentation from contributors around the world. We welcome new contributions and prefer to format our code according to GeoTools-based formatting guidelines; an Eclipse autoformatter can be found at the root of this project (https://raw.github.com/openplans/OpenTripPlanner/master/formatter.xml). Further [development guidelines](http://opentripplanner.readthedocs.org/en/latest/Developers-Guide/) can be found in the documentation.

The OpenTripPlanner project was launched by Portland, Oregon's transport agency TriMet (http://trimet.org/), and began in July of 2009 with a kick-off conference bringing together transit agencies and the authors of the major open source transit passenger information software of the day: David Emory of FivePoints, Brian Ferris of OneBusAway, and Brandon Martin-Anderson of GraphServer. From 2008 through 2012, development was coordinated by New York nonprofit OpenPlans (http://openplans.org/). By early 2013, OpenTripPlanner had become the primary trip planning software used by TriMet in the Portland regional trip planner (http://ride.trimet.org/) and was backing several popular mobile applications. Public-facing OpenTripPlanner instances were available in at least ten countries throughout the world. At this point the OpenPlans transportation software team became the independent consultancy Conveyal (http://www.conveyal.com/). The original OpenTripPlanner development team from OpenPlans still actively participates in programming, design, and community coordination via the mailing list and their roles on the OTP Project Leadership Committee.

In summer of 2013, the OpenTripPlanner project was accepted for membership in the Software Freedom Conservancy (SFC). SFC handles the legal and financial details common to many open source projects, providing a formal framework for OTP and allowing contributors to concentrate on the code. For more information, see the SFC website at http://sfconservancy.org/.


## Mailing Lists

The main forums through which the OpenTripPlanner community organizes development and provides mutual assistance are our two Google discussion groups. Changes and extensions to OTP are debated on the developers' list (opentripplanner-dev). More general questions and announcements of interest to non-developer OTP users should be directed to the opentripplanner-users list.
