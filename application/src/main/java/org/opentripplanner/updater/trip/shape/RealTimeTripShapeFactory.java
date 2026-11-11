package org.opentripplanner.updater.trip.shape;

import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.core.model.deduplicator.DeduplicatorService;
import org.opentripplanner.service.shape.model.CompactLineStringSequence;
import org.opentripplanner.service.shape.model.TripGeometryFactory;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;

/**
 * Creates the geometry of a trip pattern created by a real-time update. The factory has no access
 * to any repository; the geometry of the original pattern is given as input.
 * <p>
 * The geometry is derived from the pattern it replaces, if there is one: For each hop, the geometry
 * is copied if the hop is between the same stops. If the stops are different but belong to the
 * same stations, the old geometry is used with the first and last point moved to the new stops.
 * Otherwise, a straight line is used. A pattern without an original pattern (an added trip), or
 * without a geometry for the original, gets a straight line between each pair of stops, measured
 * with the haversine formula.
 */
public class RealTimeTripShapeFactory {

  private final DeduplicatorService deduplicator;

  public RealTimeTripShapeFactory(DeduplicatorService deduplicator) {
    this.deduplicator = deduplicator;
  }

  /**
   * Create the geometry of the given pattern.
   *
   * @param originalGeometry the geometry of {@link TripPattern#getOriginalTripPattern()}, or
   *                         {@code null} if the pattern has no original or its geometry is not
   *                         available.
   * @throws IllegalStateException if the geometry does not have one hop per pair of stops
   */
  public CompactLineStringSequence create(
    TripPattern pattern,
    @Nullable CompactLineStringSequence originalGeometry
  ) {
    var original = pattern.getOriginalTripPattern();
    CompactLineStringSequence geometry;
    if (original == null || originalGeometry == null) {
      geometry = TripGeometryFactory.straightLines(pattern.getStopPattern(), deduplicator);
    } else {
      geometry = TripGeometryFactory.fromLineStrings(
        generateHopGeometriesFromOriginalTripPatternGeometry(
          pattern.getStopPattern(),
          original.getStopPattern(),
          originalGeometry
        ),
        deduplicator
      );
    }
    int expectedHops = Math.max(pattern.numberOfStops() - 1, 0);
    if (geometry.size() != expectedHops) {
      throw new IllegalStateException(
        "Geometry of trip pattern %s has %d hops, expected %d".formatted(
          pattern.getId(),
          geometry.size(),
          expectedHops
        )
      );
    }
    return geometry;
  }

  /**
   * The new pattern and the one it replaces may have a different number of stops. The geometry is
   * then preserved up until the first mismatching stop, and a straight line is used for all
   * segments after that.
   */
  private static List<LineString> generateHopGeometriesFromOriginalTripPatternGeometry(
    StopPattern stopPattern,
    StopPattern originalStopPattern,
    CompactLineStringSequence originalTripPatternGeometry
  ) {
    List<LineString> hopGeometries = new ArrayList<>();

    for (int i = 0; i < stopPattern.getSize() - 1; i++) {
      LineString hopGeometry =
        i < originalStopPattern.getSize() - 1 ? originalTripPatternGeometry.get(i) : null;

      if (hopGeometry != null && stopPattern.sameStops(originalStopPattern, i)) {
        // Copy hop geometry from previous pattern
        hopGeometries.add(hopGeometry);
      } else if (hopGeometry != null && stopPattern.sameStations(originalStopPattern, i)) {
        // Use old geometry but patch first and last point with new stops
        var newStart = stopPattern.getStop(i).getCoordinate().asJtsCoordinate();
        var newEnd = stopPattern
          .getStop(i + 1)
          .getCoordinate()
          .asJtsCoordinate();

        Coordinate[] coordinates = hopGeometry.getCoordinates();
        coordinates[0].setCoordinate(newStart);
        coordinates[coordinates.length - 1].setCoordinate(newEnd);

        hopGeometries.add(GeometryUtils.getGeometryFactory().createLineString(coordinates));
      } else {
        // Create new straight-line geometry for hop
        hopGeometries.add(
          GeometryUtils.getGeometryFactory().createLineString(new Coordinate[] {
            stopPattern.getStop(i).getCoordinate().asJtsCoordinate(),
            stopPattern
              .getStop(i + 1)
              .getCoordinate()
              .asJtsCoordinate(),
          })
        );
      }
    }
    return hopGeometries;
  }
}
