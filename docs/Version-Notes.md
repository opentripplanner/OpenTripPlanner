## The *master* branch (Version 0.x.0-SNAPSHOT)
This is the most up-to-date branch where all new development occurs and is the basis for an upcoming x.y release of OTP.
It does not support WAR deployment as it has a built-in HTTP layer, the same one used in the Glassfish application server.
If you run `target/otp-x.y.z-shaded.jar` with the `--server` option (use `--help` for a list of available options),
resources will be available at the following URLs:
- The JavaScript map-based trip planner is at [[http://localhost:8080/]]
- The OTP API base path is [[http://localhost:8080/otp/]]
- The trip planning endpoint is at [[http://localhost:8080/otp/routers/default/plan?...]]
- The GTFS index (stops, trips, routes, etc.) is at [[http://localhost:8080/otp/routers/default/index/{routes,trips,stops}]]

For detailed paths of specific resources under these base paths, see the API docs at [[http://dev.opentripplanner.org/apidoc/master/]]

## Versions 0.15, 0.16, 0.17...
These are point releases as we work toward 1.0. Generally you will want to use the highest-numbered version and
consult the corresponding [API docs](http://dev.opentripplanner.org/apidoc). Configuration and usage should be close
to that described on our [main documentation](http://opentripplanner.readthedocs.org).

## Version 0.11 (branch 0.11.x)
This is the maintenance branch for release opentripplanner-0.11.0 from 24 March 2014. We do not recommend that
you use it unless you have a legacy deployment to maintain.
It is a multi-module Maven project and has several submodules which are described in the README.md in the root.
It has already shifted toward the 1.0 API in that router IDs are systematically included in the API URLs
(the router ID of "default" is still accepted) and the GTFS index is in the midst of an overhaul to give it a more
coherent hierarchical structure.

#### Standalone deployment 
Running `otp-core/target/otp-x.y.z-shaded.jar` (which contains all dependencies) you will find:
- API base path [[http://localhost:8080/otp/]]
- Trip planner endpoint [[http://localhost:8080/otp/routers/default/plan?...]]
- The GTFS index (agencies, stops, etc.) [[http://localhost:8080/otp/routers/default/transit/agencyIds]]

## Version 0.10 (branch 0.10.x)
This is the maintenance branch for release opentripplanner-0.10.0, which contains very few updates since early 2013. It uses a version of the API that is fast disappearing, and exists mainly for those who wish to avoid changing interfaces in long-standing deployments (e.g. router IDs are provided as query parameters). It only allows deployment to a servlet container and does not contain its own HTTP layer. We recommend against using this branch if you are new to OTP.

- API base path [[http://localhost:8080/opentripplanner-api-webapp/ws/]]
- GTFS index base path [[http://localhost:8080/opentripplanner-api-webapp/ws/transit/]]

The API docs are at [[http://docs.opentripplanner.org/apidoc/0.10.0/]]
