## Overview

OpenTripPlanner (OTP) is an open source multi-modal trip planner, focusing on travel by scheduled public transportation in combination with bicycling, walking, and mobility services including bike share and ride hailing. Its server component runs on any platform with a Java virtual machine (including Linux, Mac, and Windows). It exposes REST and GraphQL APIs that can be accessed by various clients including open source Javascript components and native mobile applications. It builds its representation of the transportation network from open data in open standard file formats (primarily GTFS and OpenStreetMap). It applies real-time updates and alerts with immediate visibility to clients, finding itineraries that account for disruptions and service changes.

Note that this branch contains **OpenTripPlanner 2**, the second major version of OTP, which has been under development since Q2 2018. The latest version of OTP is v2.1.0, released in March 2022.

If you do not want to test or explore this version, please switch to the final 1.x release tag `v1.5.0` or the `dev-1.x` branch for any patches and bugfixes applied to the v1.5.0 release.


## Performance Test

[📊 Speed Benchmark](https://otp-performance.leonard.io/) We run the SpeedTest (included in the code) to measure the performance for every PR merged into OTP. The test uses a fixed Norwegian Netex data set and Grafana is used to display the results. Each run is listed under the [GitHub Actions](https://github.com/opentripplanner/OpenTripPlanner/actions/workflows/performance-test.yml). If you need to run the test locally you can [download the NeTex file](https://leonard.io/otp/rb_norway-aggregated-netex-2021-12-11.zip) from the Continuous Integration Pipeline. There is a [text file](test/ci-performance-test/travelSearch-expected-results.csv) with the expected results witch might need to be updated when OTP is changed and the test fails. The OSM data used is the [norway-210101.osm.pbf](https://download.geofabrik.de/europe/norway-210101.osm.pbf) file from Geofabrik OSM extract from January 1st 2021.


## Repository layout

The main Java server code is in `src/main/`. OTP also includes a Javascript client based on the Leaflet mapping library in `src/client/`. This client is now primarily used for testing, with most major deployments building custom clients from reusable components. The Maven build produces a unified ("shaded") JAR file at `target/otp-VERSION.jar` containing all necessary code and dependencies to run OpenTripPlanner.

Additional information and instructions are available in the [main documentation](http://docs.opentripplanner.org/en/dev-2.x/), including a 
[quick introduction](http://docs.opentripplanner.org/en/dev-2.x/Basic-Tutorial/).

## Development 

[![codecov](https://codecov.io/gh/opentripplanner/OpenTripPlanner/branch/dev-2.x/graph/badge.svg?token=ak4PbIKgZ1)](https://codecov.io/gh/opentripplanner/OpenTripPlanner)

OpenTripPlanner is a collaborative project incorporating code, translation, and documentation from contributors around the world. We welcome new contributions. Further [development guidelines](http://docs.opentripplanner.org/en/latest/Developers-Guide/) can be found in the documentation.

### Development history

The OpenTripPlanner project was launched by Portland, Oregon's transport agency TriMet (http://trimet.org/) in July of 2009. As of this writing in Q3 2020, it has been in development for over ten years. See the main documentation for an overview of [OTP history](http://docs.opentripplanner.org/en/dev-2.x/History/) and a list of [cities and regions using OTP](http://docs.opentripplanner.org/en/dev-2.x/Deployments/) around the world.


## Mailing Lists

The main forums through which the OpenTripPlanner community organizes development and provides mutual assistance are our two Google discussion groups. Changes and extensions to OTP are debated on the [opentripplanner-dev](https://groups.google.com/forum/#!forum/opentripplanner-dev) developers' list. More general questions and announcements of interest to non-developer OTP users should be directed to the [opentripplanner-users](https://groups.google.com/forum/#!forum/opentripplanner-users) list. Other details of [project governance](http://docs.opentripplanner.org/en/dev-2.x/Governance/) can be found in the main documentation.


## OTP Ecosystem

 - [awesome-transit](https://github.com/CUTR-at-USF/awesome-transit) Community list of transit APIs, apps, datasets, research, and software.
