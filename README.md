[![Build](https://github.com/hsldevcom/opentripplanner/workflows/Process%20dev-2.x%20push%20or%20pr/badge.svg?branch=master)](https://github.com/HSLdevcom/opentripplanner/actions)

## Overview

OpenTripPlanner (OTP) is an open source multi-modal trip planner, focusing on travel by scheduled public transportation in combination with bicycling, walking, and mobility services including bike share and ride hailing. Its server component runs on any platform with a Java virtual machine (including Linux, Mac, and Windows). It exposes REST and GraphQL APIs that can be accessed by various clients including open source Javascript components and native mobile applications. It builds its representation of the transportation network from open data in open standard file formats (primarily GTFS and OpenStreetMap). It applies real-time updates and alerts with immediate visibility to clients, finding itineraries that account for disruptions and service changes.

Note that this branch contains **OpenTripPlanner 2**, the second major version of OTP, which has been under development since Q2 2018 and is approaching release. As of September 2020, OTP2 is in feature freeze and we have tagged a release candidate `v2.0-RC1` which is undergoing testing. 

If you do not want to test or explore this cutting edge version, please switch to the `master` or `dev-1.x` branches for the latest stable 1.x release and upcoming final 1.x release respectively.

## Repository layout

The main Java server code is in `src/main/`. OTP also includes a Javascript client based on the Leaflet mapping library in `src/client/`. This client is now primarily used for testing, with most major deployments building custom clients from reusable components. The Maven build produces a unified ("shaded") JAR file at `target/otp-VERSION.jar` containing all necessary code and dependencies to run OpenTripPlanner.

Additional information and instructions are available in the [main documentation](http://docs.opentripplanner.org/en/dev-2.x/), including a 
[quick introduction](http://docs.opentripplanner.org/en/dev-2.x/Basic-Tutorial/).

## Development 

OpenTripPlanner is a collaborative project incorporating code, translation, and documentation from contributors around the world. We welcome new contributions. Further [development guidelines](http://docs.opentripplanner.org/en/latest/Developers-Guide/) can be found in the documentation.

### Development history

The OpenTripPlanner project was launched by Portland, Oregon's transport agency TriMet (http://trimet.org/) in July of 2009. As of this writing in Q3 2020, it has been in development for over ten years. See the main documentation for an overview of [OTP history](http://docs.opentripplanner.org/en/dev-2.x/History/) and a list of [cities and regions using OTP](http://docs.opentripplanner.org/en/dev-2.x/Deployments/) around the world.


## Mailing Lists

The main forums through which the OpenTripPlanner community organizes development and provides mutual assistance are our two Google discussion groups. Changes and extensions to OTP are debated on the [opentripplanner-dev](https://groups.google.com/forum/#!forum/opentripplanner-dev) developers' list. More general questions and announcements of interest to non-developer OTP users should be directed to the [opentripplanner-users](https://groups.google.com/forum/#!forum/opentripplanner-users) list. Other details of [project governance](http://docs.opentripplanner.org/en/dev-2.x/Governance/) can be found in the main documentation.
