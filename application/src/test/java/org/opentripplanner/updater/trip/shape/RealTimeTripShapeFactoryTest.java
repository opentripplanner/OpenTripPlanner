package org.opentripplanner.updater.trip.shape;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;
import static org.opentripplanner.street.geometry.GeometryUtils.makeLineString;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.deduplicator.DeduplicatorService;
import org.opentripplanner.service.shape.model.CompactLineStringSequence;
import org.opentripplanner.service.shape.model.TripShapeFactory;
import org.opentripplanner.street.geometry.SphericalDistanceLibrary;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;

class RealTimeTripShapeFactoryTest {

  private static final TransitRepositoryForTest TEST_MODEL = TransitRepositoryForTest.of();
  private static final Route ROUTE = TransitRepositoryForTest.route("routeId").build();

  private static final RegularStop STOP_A = TEST_MODEL.stop("A").build();
  private static final RegularStop STOP_X = TEST_MODEL.stop("X").build();
  private static final RegularStop STOP_B = TEST_MODEL.stop("B").build();
  private static final RegularStop STOP_Y = TEST_MODEL.stop("Y").build();
  private static final RegularStop STOP_C = TEST_MODEL.stop("C").build();

  private static final TripPattern ORIGINAL = TripPattern.of(id("original"))
    .withRoute(ROUTE)
    .withStopPattern(TransitRepositoryForTest.stopPattern(STOP_A, STOP_B, STOP_C))
    .build();

  private static final CompactLineStringSequence ORIGINAL_TRIP_PATTERN_SHAPE =
    TripShapeFactory.fromLineStrings(
      List.of(
        makeLineString(STOP_A.getCoordinate(), STOP_X.getCoordinate(), STOP_B.getCoordinate()),
        makeLineString(STOP_B.getCoordinate(), STOP_Y.getCoordinate(), STOP_C.getCoordinate())
      ),
      DeduplicatorService.NOOP
    );

  private final RealTimeTripShapeFactory subject = new RealTimeTripShapeFactory(
    DeduplicatorService.NOOP
  );

  @Test
  void hopShapeForReplacementPattern() {
    var pattern = TripPattern.of(id("replacement"))
      .withRoute(ROUTE)
      .withStopPattern(TransitRepositoryForTest.stopPattern(STOP_A, STOP_B, STOP_X, STOP_Y))
      .withRealTimeStopPatternModified()
      .withOriginalTripPattern(ORIGINAL)
      .build();

    var shape = subject.createTripPatternShape(pattern, ORIGINAL_TRIP_PATTERN_SHAPE);

    assertEquals(3, shape.size());
    assertEquals(
      makeLineString(STOP_A.getCoordinate(), STOP_X.getCoordinate(), STOP_B.getCoordinate()),
      shape.get(0)
    );
    assertEquals(makeLineString(STOP_B.getCoordinate(), STOP_X.getCoordinate()), shape.get(1));
    assertEquals(makeLineString(STOP_X.getCoordinate(), STOP_Y.getCoordinate()), shape.get(2));
  }

  @Test
  void copyHopsFromTheOriginalWhenStopsAreUnchanged() {
    var pattern = TripPattern.of(id("replacement"))
      .withRoute(ROUTE)
      .withStopPattern(TransitRepositoryForTest.stopPattern(STOP_A, STOP_B, STOP_C))
      .withRealTimeStopPatternModified()
      .withOriginalTripPattern(ORIGINAL)
      .build();

    var shape = subject.createTripPatternShape(pattern, ORIGINAL_TRIP_PATTERN_SHAPE);

    assertEquals(ORIGINAL_TRIP_PATTERN_SHAPE.distanceBetween(0, 2), shape.distanceBetween(0, 2));
    assertEquals(ORIGINAL_TRIP_PATTERN_SHAPE.get(0), shape.get(0));
    assertEquals(ORIGINAL_TRIP_PATTERN_SHAPE.get(1), shape.get(1));
  }

  /**
   * The original pattern has one hop with an extra coordinate between the stops. The replacement
   * uses an alternative stop in the same station as the first stop, so the old geometry is kept
   * and only the first point is moved to the new stop.
   */
  @Test
  void patchTheEndsOfTheOriginalHopWhenStopsAreInTheSameStation() {
    var stationOrigin = TEST_MODEL.station("S1").withCoordinate(0.0, 0.0).build();
    var stationDestination = TEST_MODEL.station("S2").withCoordinate(1.0, 1.0).build();
    var stopOrigin = TEST_MODEL.stop("A1")
      .withCoordinate(0.1, 0.1)
      .withParentStation(stationOrigin)
      .build();
    var stopNewOrigin = TEST_MODEL.stop("A2")
      .withCoordinate(0.2, 0.2)
      .withParentStation(stationOrigin)
      .build();
    var stopDestination = TEST_MODEL.stop("D")
      .withCoordinate(0.9, 0.9)
      .withParentStation(stationDestination)
      .build();
    var original = TripPattern.of(id("original2"))
      .withRoute(ROUTE)
      .withStopPattern(TransitRepositoryForTest.stopPattern(stopOrigin, stopDestination))
      .build();
    var middle = new WgsCoordinate(0.5, 0.5);
    var originalShape = TripShapeFactory.fromLineStrings(
      List.of(makeLineString(stopOrigin.getCoordinate(), middle, stopDestination.getCoordinate())),
      DeduplicatorService.NOOP
    );
    var pattern = TripPattern.of(id("replacement2"))
      .withRoute(ROUTE)
      .withStopPattern(TransitRepositoryForTest.stopPattern(stopNewOrigin, stopDestination))
      .withRealTimeStopPatternModified()
      .withOriginalTripPattern(original)
      .build();

    var shape = subject.createTripPatternShape(pattern, originalShape);

    assertEquals(
      makeLineString(stopNewOrigin.getCoordinate(), middle, stopDestination.getCoordinate()),
      shape.get(0)
    );
    assertEquals(
      makeLineString(stopOrigin.getCoordinate(), middle, stopDestination.getCoordinate()),
      originalShape.get(0)
    );
  }

  @Test
  void useStraightLinesMeasuredWithHaversineForPatternsWithoutOriginal() {
    var pattern = TripPattern.of(id("added"))
      .withRoute(ROUTE)
      .withStopPattern(TransitRepositoryForTest.stopPattern(STOP_A, STOP_B, STOP_C))
      .withRealTimeAddedTrip()
      .build();

    var shape = subject.createTripPatternShape(pattern, null);

    assertEquals(makeLineString(STOP_A.getCoordinate(), STOP_B.getCoordinate()), shape.get(0));
    double expected = distance(STOP_A, STOP_B) + distance(STOP_B, STOP_C);
    assertEquals((int) Math.round(expected), shape.distanceBetween(0, 2));
  }

  @Test
  void useStraightLinesWhenTheOriginalTripPatternShapeIsNotAvailable() {
    var pattern = TripPattern.of(id("replacement"))
      .withRoute(ROUTE)
      .withStopPattern(TransitRepositoryForTest.stopPattern(STOP_A, STOP_B, STOP_C))
      .withRealTimeStopPatternModified()
      .withOriginalTripPattern(ORIGINAL)
      .build();

    var shape = subject.createTripPatternShape(pattern, null);

    assertEquals(2, shape.size());
    assertEquals(makeLineString(STOP_A.getCoordinate(), STOP_B.getCoordinate()), shape.get(0));
    assertEquals(makeLineString(STOP_B.getCoordinate(), STOP_C.getCoordinate()), shape.get(1));
  }

  private static double distance(RegularStop from, RegularStop to) {
    return SphericalDistanceLibrary.distance(
      from.getLat(),
      from.getLon(),
      to.getLat(),
      to.getLon()
    );
  }
}
