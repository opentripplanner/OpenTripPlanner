package org.opentripplanner.graph_builder.issues;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.transit.model.site.RegularStop;

/**
 * A stop's coordinate lies well away from the OSM {@code boarding_location} it was linked to. Their
 * reference tags match, so they are meant to be the same place, but the gap is too large to be a
 * surveying discrepancy: one of the two is misplaced, or the OSM feature carries a reference it
 * should not.
 * <p>
 * The stop is still linked, keeping its own coordinate and connected by an edge of this length, so
 * the gap is reported rather than silently walked on every itinerary.
 *
 * @param boardingLocation what the stop was linked to, for looking the feature up in OSM
 */
public record StopFarFromBoardingLocation(
  RegularStop stop,
  String boardingLocation,
  double distanceMeters
) implements DataImportIssue {
  private static final String FMT =
    "Stop %s is %.0f m from the %s it is linked to. Check the stop coordinate and the OSM reference tags.";

  @Override
  public String getMessage() {
    return String.format(FMT, stop.getId(), distanceMeters, boardingLocation);
  }

  @Override
  public Geometry getGeometry() {
    return GeometryUtils.getGeometryFactory().createPoint(
      new Coordinate(stop.getLon(), stop.getLat())
    );
  }
}
