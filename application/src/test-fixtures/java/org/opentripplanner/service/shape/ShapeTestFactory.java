package org.opentripplanner.service.shape;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.core.model.deduplicator.DeduplicatorService;
import org.opentripplanner.service.shape.dataimport.ShapeDataImport;
import org.opentripplanner.service.shape.model.CompactLineStringSequence;
import org.opentripplanner.service.shape.model.TripGeometryFactory;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.transit.model.network.TripPattern;

/**
 * Test helper for creating shape data. Patterns that are not built from shape data get a straight
 * line between each pair of stops. Use {@code TestServerContext.createShapeService} to serve the
 * data.
 */
public class ShapeTestFactory {

  private ShapeTestFactory() {}

  /**
   * The geometry of the pattern as straight lines between its stops.
   */
  public static CompactLineStringSequence straightLines(TripPattern pattern) {
    var stopPattern = pattern.getStopPattern();
    var stops = new ArrayList<WgsCoordinate>(stopPattern.getSize());
    for (int i = 0; i < stopPattern.getSize(); i++) {
      stops.add(stopPattern.getStop(i).getCoordinate());
    }
    return TripGeometryFactory.straightLines(stops, DeduplicatorService.NOOP);
  }

  /**
   * A short north-south geometry starting at the given latitude. Different latitudes give
   * different geometries, which makes them easy to tell apart in tests.
   */
  public static CompactLineStringSequence straightLine(double lat) {
    return TripGeometryFactory.straightLines(
      List.of(new WgsCoordinate(lat, 10.0), new WgsCoordinate(lat + 0.01, 10.0)),
      DeduplicatorService.NOOP
    );
  }

  /**
   * A shape data import with a straight-line geometry for each of the patterns.
   */
  public static ShapeDataImport shapeDataImport(Collection<TripPattern> patterns) {
    var shapeDataImport = new ShapeDataImport();
    for (var pattern : patterns) {
      shapeDataImport.setTripPatternGeometry(pattern.getId(), straightLines(pattern));
    }
    return shapeDataImport;
  }
}
