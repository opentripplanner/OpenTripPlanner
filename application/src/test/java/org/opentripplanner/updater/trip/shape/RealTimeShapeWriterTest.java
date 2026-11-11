package org.opentripplanner.updater.trip.shape;

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
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;

class RealTimeShapeWriterTest {

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
  private final RealTimeTripShapeFactory factory = new RealTimeTripShapeFactory(
    DeduplicatorService.NOOP
  );
  private final RealTimeShapeWriter subject = new RealTimeShapeWriter(() -> repository, factory);

  @Test
  void storeAGeometryDerivedFromTheOriginal() {
    repositoryWithOriginalGeometry();
    var pattern = TripPattern.of(id("replacement"))
      .withRoute(ROUTE)
      .withStopPattern(TransitRepositoryForTest.stopPattern(STOP_A, STOP_B, STOP_C))
      .withRealTimeStopPatternModified()
      .withOriginalTripPattern(ORIGINAL)
      .build();

    subject.ensureGeometry(pattern);

    var original = repository.getTripPatternGeometry(ORIGINAL.getId());
    var geometry = repository.getTripPatternGeometry(pattern.getId());
    assertThat(geometry.get(0)).isEqualTo(original.get(0));
    assertThat(geometry.get(1)).isEqualTo(original.get(1));
  }

  @Test
  void storeStraightLinesForPatternsWithoutOriginal() {
    var pattern = TripPattern.of(id("added"))
      .withRoute(ROUTE)
      .withStopPattern(TransitRepositoryForTest.stopPattern(STOP_A, STOP_B, STOP_C))
      .withRealTimeAddedTrip()
      .build();

    subject.ensureGeometry(pattern);

    var geometry = repository.getTripPatternGeometry(pattern.getId());
    assertThat(geometry.get(0)).isEqualTo(
      makeLineString(STOP_A.getCoordinate(), STOP_B.getCoordinate())
    );
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
    var writer = new RealTimeShapeWriter(() -> {
      accessCount.incrementAndGet();
      return repository;
    }, factory);

    writer.ensureGeometry(ORIGINAL);

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
  void useStraightLinesWhenARealTimeOriginalHasNoGeometry() {
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
    repository.clear(firstUpdate.getFeedId());

    subject.ensureGeometry(secondUpdate);

    var second = repository.getTripPatternGeometry(secondUpdate.getId());
    assertThat(second.get(0)).isEqualTo(
      makeLineString(STOP_A.getCoordinate(), STOP_B.getCoordinate())
    );
    assertThat(repository.getTripPatternGeometry(firstUpdate.getId())).isNull();
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
