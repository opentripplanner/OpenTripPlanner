package org.opentripplanner.updater.trip.patterncache;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;
import static org.opentripplanner.street.geometry.GeometryUtils.makeLineString;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.deduplicator.DeduplicatorService;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.service.shape.ScheduledShapeData;
import org.opentripplanner.service.shape.internal.DefaultShapeRepository;
import org.opentripplanner.service.shape.model.CompactLineStringSequence;
import org.opentripplanner.service.shape.model.TripGeometryFactory;
import org.opentripplanner.street.geometry.SphericalDistanceLibrary;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;

class RealTimeShapeResolverTest {

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

  private ScheduledShapeData scheduledShapes = ScheduledShapeData.empty();
  private DefaultShapeRepository repository = new DefaultShapeRepository(scheduledShapes);
  private RealTimeShapeResolver subject = new RealTimeShapeResolver(
    () -> repository,
    DeduplicatorService.NOOP
  );

  @Test
  void hopGeometryForReplacementPattern() {
    repositoryWithOriginalGeometry();
    var pattern = TripPattern.of(id("replacement"))
      .withRoute(ROUTE)
      .withStopPattern(TransitRepositoryForTest.stopPattern(STOP_A, STOP_B, STOP_X, STOP_Y))
      .withRealTimeStopPatternModified()
      .withOriginalTripPattern(ORIGINAL)
      .build();

    subject.ensureGeometry(pattern);

    var tripPatternGeometry = repository.getTripPatternGeometry(pattern.getId());
    assertThat(tripPatternGeometry.size()).isEqualTo(3);
    assertThat(tripPatternGeometry.get(0)).isEqualTo(
      makeLineString(STOP_A.getCoordinate(), STOP_X.getCoordinate(), STOP_B.getCoordinate())
    );
    assertThat(tripPatternGeometry.get(1)).isEqualTo(
      makeLineString(STOP_B.getCoordinate(), STOP_X.getCoordinate())
    );
    assertThat(tripPatternGeometry.get(2)).isEqualTo(
      makeLineString(STOP_X.getCoordinate(), STOP_Y.getCoordinate())
    );
  }

  @Test
  void copyHopsFromTheOriginalWhenStopsAreUnchanged() {
    repositoryWithOriginalGeometry();
    var pattern = TripPattern.of(id("replacement"))
      .withRoute(ROUTE)
      .withStopPattern(TransitRepositoryForTest.stopPattern(STOP_A, STOP_B, STOP_C))
      .withRealTimeStopPatternModified()
      .withOriginalTripPattern(ORIGINAL)
      .build();

    subject.ensureGeometry(pattern);

    var original = repository.getTripPatternGeometry(ORIGINAL.getId());
    var tripPatternGeometry = repository.getTripPatternGeometry(pattern.getId());
    assertThat(tripPatternGeometry.distanceBetween(0, 2)).isEqualTo(original.distanceBetween(0, 2));
    assertThat(tripPatternGeometry.get(0)).isEqualTo(original.get(0));
    assertThat(tripPatternGeometry.get(1)).isEqualTo(original.get(1));
  }

  /**
   * The original pattern has one hop with an extra coordinate between the stops. The replacement
   * uses an alternative stop in the same station as the first stop, so the old geometry is kept
   * and only the first point is moved to the new stop.
   */
  @Test
  void testSetHopGeometriesFromPattern() {
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
    givenScheduledGeometry(
      original.getId(),
      TripGeometryFactory.fromLineStrings(
        List.of(
          makeLineString(stopOrigin.getCoordinate(), middle, stopDestination.getCoordinate())
        ),
        DeduplicatorService.NOOP
      )
    );
    var pattern = TripPattern.of(id("replacement2"))
      .withRoute(ROUTE)
      .withStopPattern(TransitRepositoryForTest.stopPattern(stopNewOrigin, stopDestination))
      .withRealTimeStopPatternModified()
      .withOriginalTripPattern(original)
      .build();

    subject.ensureGeometry(pattern);

    var hop = repository.getTripPatternGeometry(pattern.getId()).get(0);
    assertThat(hop).isEqualTo(
      makeLineString(stopNewOrigin.getCoordinate(), middle, stopDestination.getCoordinate())
    );
    assertThat(repository.getTripPatternGeometry(original.getId()).get(0)).isEqualTo(
      makeLineString(stopOrigin.getCoordinate(), middle, stopDestination.getCoordinate())
    );
  }

  @Test
  void useStraightLinesMeasuredWithHaversineForPatternsWithoutOriginal() {
    var pattern = TripPattern.of(id("added"))
      .withRoute(ROUTE)
      .withStopPattern(TransitRepositoryForTest.stopPattern(STOP_A, STOP_B, STOP_C))
      .withRealTimeAddedTrip()
      .build();

    subject.ensureGeometry(pattern);

    var tripPatternGeometry = repository.getTripPatternGeometry(pattern.getId());
    assertThat(tripPatternGeometry.get(0)).isEqualTo(
      makeLineString(STOP_A.getCoordinate(), STOP_B.getCoordinate())
    );
    double expected = distance(STOP_A, STOP_B) + distance(STOP_B, STOP_C);
    assertThat(tripPatternGeometry.distanceBetween(0, 2)).isEqualTo((int) Math.round(expected));
  }

  @Test
  void ensureGeometryIsIdempotent() {
    var pattern = TripPattern.of(id("added"))
      .withRoute(ROUTE)
      .withStopPattern(TransitRepositoryForTest.stopPattern(STOP_A, STOP_B, STOP_C))
      .withRealTimeAddedTrip()
      .build();

    subject.ensureGeometry(pattern);
    var first = repository.getTripPatternGeometry(pattern.getId());
    subject.ensureGeometry(pattern);

    assertThat(repository.getTripPatternGeometry(pattern.getId())).isSameInstanceAs(first);
  }

  @Test
  void doNotTouchTheRepositoryForScheduledPatterns() {
    var accessCount = new AtomicInteger();
    var resolver = new RealTimeShapeResolver(() -> {
      accessCount.incrementAndGet();
      return repository;
    }, DeduplicatorService.NOOP);

    resolver.ensureGeometry(ORIGINAL);

    assertThat(accessCount.get()).isEqualTo(0);
    assertThat(repository.getTripPatternGeometry(ORIGINAL.getId())).isNull();
  }

  @Test
  void writeOnlyToTheRealTimeRepository() {
    repositoryWithOriginalGeometry();
    var pattern = TripPattern.of(id("replacement"))
      .withRoute(ROUTE)
      .withStopPattern(TransitRepositoryForTest.stopPattern(STOP_A, STOP_B, STOP_C))
      .withRealTimeStopPatternModified()
      .withOriginalTripPattern(ORIGINAL)
      .build();
    var snapshotBefore = repository.createSnapshot();

    subject.ensureGeometry(pattern);

    assertThat(repository.getTripPatternGeometry(pattern.getId())).isNotNull();
    assertThat(scheduledShapes.getTripPatternGeometry(pattern.getId())).isNull();
    assertThat(snapshotBefore.getTripPatternGeometry(pattern.getId())).isNull();
  }

  @Test
  void deriveFromAnOriginalWrittenEarlierInTheSameTransaction() {
    repositoryWithOriginalGeometry();
    var firstUpdate = TripPattern.of(id("replacement1"))
      .withRoute(ROUTE)
      .withStopPattern(TransitRepositoryForTest.stopPattern(STOP_A, STOP_B, STOP_C))
      .withRealTimeStopPatternModified()
      .withOriginalTripPattern(ORIGINAL)
      .build();
    var secondUpdate = TripPattern.of(id("replacement2nd"))
      .withRoute(ROUTE)
      .withStopPattern(TransitRepositoryForTest.stopPattern(STOP_A, STOP_B, STOP_C))
      .withRealTimeStopPatternModified()
      .withOriginalTripPattern(firstUpdate)
      .build();

    subject.ensureGeometry(firstUpdate);
    subject.ensureGeometry(secondUpdate);

    var first = repository.getTripPatternGeometry(firstUpdate.getId());
    var second = repository.getTripPatternGeometry(secondUpdate.getId());
    assertThat(second.get(0)).isEqualTo(first.get(0));
    assertThat(second.get(1)).isEqualTo(first.get(1));
  }

  @Test
  void failIfTheOriginalPatternHasNoGeometry() {
    var pattern = TripPattern.of(id("replacement"))
      .withRoute(ROUTE)
      .withStopPattern(TransitRepositoryForTest.stopPattern(STOP_A, STOP_B))
      .withRealTimeStopPatternModified()
      .withOriginalTripPattern(ORIGINAL)
      .build();

    assertThrows(IllegalStateException.class, () -> subject.ensureGeometry(pattern));
  }

  private static double distance(RegularStop from, RegularStop to) {
    return SphericalDistanceLibrary.distance(
      from.getLat(),
      from.getLon(),
      to.getLat(),
      to.getLon()
    );
  }

  private void givenScheduledGeometry(
    FeedScopedId patternId,
    CompactLineStringSequence tripPatternGeometry
  ) {
    scheduledShapes = ScheduledShapeData.of(Map.of(patternId, tripPatternGeometry));
    repository = new DefaultShapeRepository(scheduledShapes);
  }

  private void repositoryWithOriginalGeometry() {
    givenScheduledGeometry(
      ORIGINAL.getId(),
      TripGeometryFactory.fromLineStrings(
        List.of(
          makeLineString(STOP_A.getCoordinate(), STOP_X.getCoordinate(), STOP_B.getCoordinate()),
          makeLineString(STOP_B.getCoordinate(), STOP_Y.getCoordinate(), STOP_C.getCoordinate())
        ),
        DeduplicatorService.NOOP
      )
    );
  }
}
