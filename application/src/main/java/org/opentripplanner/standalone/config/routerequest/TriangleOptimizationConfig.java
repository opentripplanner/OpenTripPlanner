package org.opentripplanner.standalone.config.routerequest;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_0;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_5;

import org.opentripplanner.routing.api.request.preference.TimeSlopeSafetyTriangle;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class TriangleOptimizationConfig {

  static void mapOptimizationTriangle(NodeAdapter c, TimeSlopeSafetyTriangle.Builder preferences) {
    var optimizationTriangle = c
      .of("triangle")
      .since(V2_5)
      .summary("Triangle optimization criteria.")
      .description("Optimization type doesn't need to be defined if these values are defined.")
      .asObject();
    mapTriangleParameters(optimizationTriangle, preferences);
  }

  private static void mapTriangleParameters(
    NodeAdapter c,
    TimeSlopeSafetyTriangle.Builder builder
  ) {
    builder
      .withTime(
        c
          .of("time")
          .since(V2_0)
          .summary("Relative importance of duration of travel (range 0.0, 1.0).")
          .asDouble(builder.time())
      )
      .withSlope(
        c
          .of("flatness")
          .since(V2_0)
          .summary("Relative importance of flat terrain (range 0.0, 1.0).")
          .asDouble(builder.slope())
      )
      .withSafety(
        c
          .of("safety")
          .since(V2_0)
          .summary("Relative importance of safety (range 0.0, 1.0).")
          .description(
            """
            If this factor is set to 1.0, the safety value is applied in whole as a multiplier on
            top of the reluctance.

            The safety value is set for tags in OpenStreetMap by the [OSM Tag Mapper](osm/OsmTag.md)
            which is mainly determined by the amount of traffic, but can also include other
            concerns such as convenience and general cyclist preferences by taking into account road
            surface etc.
            """
          )
          .asDouble(builder.safety())
      );
  }
}
