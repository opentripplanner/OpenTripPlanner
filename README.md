## Overview

[![Join the chat at https://gitter.im/opentripplanner/OpenTripPLanner](https://badges.gitter.im/opentripplanner/OpenTripPlanner.svg)](https://gitter.im/opentripplanner/OpenTripPlanner)
[![Matrix](https://img.shields.io/matrix/opentripplanner%3Amatrix.org?label=Matrix%20chat&?cacheSeconds=172800)](https://matrix.to/#/#opentripplanner_OpenTripPlanner:gitter.im)
[![codecov](https://codecov.io/gh/opentripplanner/OpenTripPlanner/branch/dev-2.x/graph/badge.svg?token=ak4PbIKgZ1)](https://codecov.io/gh/opentripplanner/OpenTripPlanner)
[![Commit activity](https://img.shields.io/github/commit-activity/y/opentripplanner/OpenTripPlanner)](https://github.com/opentripplanner/OpenTripPlanner/graphs/contributors)
[![Docker Pulls](https://img.shields.io/docker/pulls/opentripplanner/opentripplanner)](https://hub.docker.com/r/opentripplanner/opentripplanner)

OpenTripPlanner (OTP) is an open source multi-modal trip planner, focusing on travel by scheduled
public transportation in combination with bicycling, walking, and mobility services including bike
share and ride hailing. Its server component runs on any platform with a Java virtual machine (
including Linux, Mac, and Windows). It exposes GraphQL APIs that can be accessed by various
clients including open source Javascript components and native mobile applications. It builds its
representation of the transportation network from open data in open standard file formats (primarily
GTFS and OpenStreetMap). It applies real-time updates and alerts with immediate visibility to
clients, finding itineraries that account for disruptions and service changes.

Note that this branch contains **OpenTripPlanner 2**, the second major version of OTP, which has
been under development since 2018 and is now the dominant one and the only one being supported.

## Performance Test

[📊 Dashboard](https://otp-performance.leonard.io/) 

We run a speed test (included in the code) to measure the performance for every PR merged into OTP. 

[More information about how to set up and run it.](./test/performance/README.md)

## Repository layout

The main Java server code is in `application/src/main/`. OTP also includes a Javascript client 
based on the MapLibre mapping library in `client/src/`. This client is now used for testing, with
most major deployments building custom clients from reusable components. The Maven build produces a
unified ("shaded") JAR file at `otp-shaded/target/otp-shaded-VERSION.jar` containing all necessary
code and dependencies to run OpenTripPlanner.

Additional information and instructions are available in
the [main documentation](http://docs.opentripplanner.org/en/dev-2.x/), including a
[quick introduction](http://docs.opentripplanner.org/en/dev-2.x/Basic-Tutorial/).

## Development


OpenTripPlanner is a collaborative project incorporating code, translation, and documentation from
contributors around the world. We welcome new contributions.
Further [development guidelines](http://docs.opentripplanner.org/en/latest/Developers-Guide/) can be
found in the documentation.

### Development history

The OpenTripPlanner project was launched by Portland, Oregon's transport agency
TriMet (http://trimet.org/) in July of 2009. As of this writing in Q3 2020, it has been in
development for over ten years. See the main documentation for an overview
of [OTP history](http://docs.opentripplanner.org/en/dev-2.x/History/) and a list
of [cities and regions using OTP](http://docs.opentripplanner.org/en/dev-2.x/Deployments/) around
the world.

## Getting in touch

The fastest way to get help is to use our [Gitter chat room](https://gitter.im/opentripplanner/OpenTripPlanner) where most of the core developers
are. Bug reports may be filed via the Github [issue tracker](https://github.com/openplans/OpenTripPlanner/issues). The OpenTripPlanner [mailing list](http://groups.google.com/group/opentripplanner-users)
is used almost exclusively for project announcements. The mailing list and issue tracker are not
intended for support questions or discussions. Please use the chat for this purpose. Other details
of [project governance](http://docs.opentripplanner.org/en/dev-2.x/Governance/) can be found in the main documentation.

## OTP Ecosystem

- [awesome-transit](https://github.com/MobilityData/awesome-transit) Community list of transit APIs,
  apps, datasets, research, and software.
