package org.opentripplanner.ext.flex.flexpathcalculator;

import java.util.function.Supplier;
import org.locationtech.jts.geom.LineString;

/**
 * This class contains the results from a FlexPathCalculator.
 */
public class FlexPath {

  public int distanceMeters;
  public int durationSeconds;
  private final Supplier<LineString> geometrySupplier;
  private LineString geometry;

  /**
   *
   * @param geometrySupplier Computing a linestring from a GraphPath is a surprisingly expensive
   *                         operation and since there are very many instances of these
   *                         for a flex access/egress search the actual computation is delayed until
   *                         the linestring is actually needed.
   *                         Most of them are _not_ needed so this increases performance quite
   *                         dramatically.
   */
  public FlexPath(int distanceMeters, int durationSeconds, Supplier<LineString> geometrySupplier) {
    this.distanceMeters = distanceMeters;
    this.durationSeconds = durationSeconds;
    this.geometrySupplier = geometrySupplier;
  }

  public LineString getGeometry() {
    if(geometry == null) {
      geometry = geometrySupplier.get();
    }
    return geometry;
  }
}
