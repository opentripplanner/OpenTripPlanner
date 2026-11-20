package org.opentripplanner.updater.trip.shape;

import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.core.model.deduplicator.DeduplicatorService;
import org.opentripplanner.service.shape.model.CompactLineStringSequence;
import org.opentripplanner.service.shape.model.TripShapeFactory;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;

/**
 * Creates the shape of a trip pattern created by a real-time update. The factory has no access to
 * any repository; the shape of the original pattern is given as input.
 * <p>
 * The shape is derived from the pattern it replaces, if there is one: For each hop, the geometry is
 * copied if the hop is between the same stops. If the stops are different but belong to the same
 * stations, the old geometry is used with the first and last point moved to the new stops.
 * Otherwise, a straight line is used. A pattern without an original pattern (an added trip), or
 * without a shape for the original, gets a straight line between each pair of stops, measured with
 * the haversine formula.
 */
public class RealTimeTripShapeFactory {

  private final DeduplicatorService deduplicator;

  public RealTimeTripShapeFactory(DeduplicatorService deduplicator) {
    this.deduplicator = deduplicator;
  }

  /**
   * Create the shape of the given pattern.
   *
   * @param originalTripPatternShape the shape of {@link TripPattern#getOriginalTripPattern()}, or
   *                                 {@code null} if the pattern has no original or its shape is
   *                                 not available.
   */
  public CompactLineStringSequence createTripPatternShape(
    TripPattern pattern,
    @Nullable CompactLineStringSequence originalTripPatternShape
  ) {
    var original = pattern.getOriginalTripPattern();
    if (original == null || originalTripPatternShape == null) {
      return TripShapeFactory.straightLines(pattern.getStopPattern(), deduplicator);
    }
    return TripShapeFactory.fromLineStrings(
      generateHopGeometriesFromOriginalTripPatternShape(
        pattern.getStopPattern(),
        original.getStopPattern(),
        originalTripPatternShape
      ),
      deduplicator
    );
  }

  /**
   * The new pattern and the one it replaces may have a different number of stops. The geometry is
   * then preserved up until the first mismatching stop, and a straight line is used for all
   * segments after that.
   */
  private static List<LineString> generateHopGeometriesFromOriginalTripPatternShape(
    StopPattern stopPattern,
    StopPattern originalStopPattern,
    CompactLineStringSequence originalTripPatternShape
  ) {
    List<LineString> hopGeometries = new ArrayList<>();

    for (int i = 0; i < stopPattern.getSize() - 1; i++) {
      LineString hopGeometry =
        i < originalStopPattern.getSize() - 1 ? originalTripPatternShape.get(i) : null;

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
