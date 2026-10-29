package org.opentripplanner.updater.trip.patterncache;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.core.model.deduplicator.DeduplicatorService;
import org.opentripplanner.service.shape.ShapeRepository;
import org.opentripplanner.service.shape.model.CompactLineStringSequence;
import org.opentripplanner.service.shape.model.TripGeometryFactory;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;

/**
 * Makes sure that a trip pattern created by a real-time update has a geometry in the
 * {@link ShapeRepository}.
 * <p>
 * The geometry is derived from the pattern it replaces, if there is one: For each hop, the geometry
 * is copied if the hop is between the same stops. If the stops are different but belong to the
 * same stations, the old geometry is used with the first and last point moved to the new stops.
 * Otherwise, a straight line is used. A pattern without an original pattern (an added trip) gets a
 * straight line between each pair of stops, measured with the haversine formula.
 * <p>
 * The resolver is idempotent: the {@link TripPatternCache} outlives the update task that created
 * the pattern, so a pattern may be seen again by a later task even though the shape written by the
 * first task was discarded with its transaction.
 */
public class RealTimeShapeResolver {

  /**
   * Resolved lazily, so that tasks that do not touch any real-time pattern do not cause a new shape
   * snapshot to be published at commit.
   */
  private final Supplier<ShapeRepository> shapes;
  private final DeduplicatorService deduplicator;

  public RealTimeShapeResolver(Supplier<ShapeRepository> shapes, DeduplicatorService deduplicator) {
    this.shapes = shapes;
    this.deduplicator = deduplicator;
  }

  /**
   * Write the geometry of the given pattern, unless the pattern is not created in real-time or the
   * geometry is already present.
   */
  public void ensureGeometry(TripPattern pattern) {
    if (!pattern.isRealTimeTripPattern()) {
      return;
    }
    var repository = shapes.get();
    if (repository.getTripPatternGeometry(pattern.getId()) != null) {
      return;
    }
    var geometry = createTripPatternGeometry(pattern, repository);
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
    repository.setTripPatternGeometry(pattern.getId(), geometry);
  }

  private CompactLineStringSequence createTripPatternGeometry(
    TripPattern pattern,
    ShapeRepository repository
  ) {
    var original = pattern.getOriginalTripPattern();
    if (original == null) {
      return TripGeometryFactory.straightLines(stopCoordinates(pattern), deduplicator);
    }
    // The original is either scheduled or a real-time pattern added earlier, possibly in this
    // same transaction; the repository sees both.
    var originalTripPatternGeometry = repository.getTripPatternGeometry(original.getId());
    if (originalTripPatternGeometry == null) {
      throw new IllegalStateException(
        "No geometry found for the original trip pattern " + original.getId()
      );
    }
    return TripGeometryFactory.fromLineStrings(
      generateHopGeometriesFromOriginalTripPatternGeometry(
        pattern.getStopPattern(),
        original.getStopPattern(),
        originalTripPatternGeometry
      ),
      deduplicator
    );
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

  private static List<WgsCoordinate> stopCoordinates(TripPattern pattern) {
    var stopPattern = pattern.getStopPattern();
    var coordinates = new ArrayList<WgsCoordinate>(stopPattern.getSize());
    for (int i = 0; i < stopPattern.getSize(); i++) {
      coordinates.add(stopPattern.getStop(i).getCoordinate());
    }
    return coordinates;
  }
}
