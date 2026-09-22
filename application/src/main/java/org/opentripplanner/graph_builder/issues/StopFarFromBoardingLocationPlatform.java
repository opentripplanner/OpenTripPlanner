package org.opentripplanner.graph_builder.issues;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.transit.model.site.RegularStop;

/**
 * A stop's coordinate lies well outside the OSM {@code boarding_location} platform it was linked to.
 * Their reference tags match, so they are meant to be the same place, but the gap is too large to be
 * a surveying discrepancy: one of the two is misplaced, or the platform carries a reference it
 * should not.
 * <p>
 * The stop is still linked, keeping its own coordinate and connected to the platform by an edge of
 * this length, so the gap is reported rather than silently walked on every itinerary.
 */
public record StopFarFromBoardingLocationPlatform(
  RegularStop stop,
  double distanceMeters
) implements DataImportIssue {
  private static final String FMT =
    "Stop %s is %.0f m outside the OSM platform it is linked to; it is connected to the platform " +
    "by a walk of that length. Check the stop coordinate and the platform's reference tags.";

  @Override
  public String getMessage() {
    return String.format(FMT, stop.getId(), distanceMeters);
  }

  @Override
  public Geometry getGeometry() {
    return GeometryUtils.getGeometryFactory().createPoint(
      new Coordinate(stop.getLon(), stop.getLat())
    );
  }
}
