package org.opentripplanner.service.shape;

import java.util.Collection;
import org.opentripplanner.core.model.deduplicator.DeduplicatorService;
import org.opentripplanner.service.shape.dataimport.ShapeDataImport;
import org.opentripplanner.service.shape.model.CompactLineStringSequence;
import org.opentripplanner.service.shape.model.TripShapeFactory;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.network.TripPattern;

/**
 * Test helper for creating shape data. Patterns that are not built from shape data get a straight
 * line between each pair of stops. Use {@code TestServerContext.createShapeService} to serve the
 * data.
 */
public class ShapeTestFactory {

  private ShapeTestFactory() {}

  /**
   * A short north-south geometry starting at the given latitude. Different latitudes give
   * different geometries, which makes them easy to tell apart in tests.
   */
  public static CompactLineStringSequence straightLine(double lat) {
    var testModel = TransitRepositoryForTest.of();
    var stopPattern = TransitRepositoryForTest.stopPattern(
      testModel.stop("A", lat, 10.0).build(),
      testModel.stop("B", lat + 0.01, 10.0).build()
    );
    return TripShapeFactory.straightLines(stopPattern, DeduplicatorService.NOOP);
  }

  /**
   * A shape data import with a straight-line geometry for each of the patterns.
   */
  public static ShapeDataImport shapeDataImport(Collection<TripPattern> patterns) {
    var shapeDataImport = new ShapeDataImport();
    for (var pattern : patterns) {
      shapeDataImport.setTripPatternShape(
        pattern.getId(),
        TripShapeFactory.straightLines(pattern.getStopPattern(), DeduplicatorService.NOOP)
      );
    }
    return shapeDataImport;
  }
}
