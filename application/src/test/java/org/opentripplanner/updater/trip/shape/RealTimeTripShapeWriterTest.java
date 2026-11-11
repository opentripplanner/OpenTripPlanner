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
import org.opentripplanner.service.shape.model.TripShapeFactory;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;

class RealTimeTripShapeWriterTest {

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
  private final RealTimeTripShapeWriter subject = new RealTimeTripShapeWriter(
    () -> repository,
    factory
  );

  @Test
  void storeAShapeDerivedFromTheOriginal() {
    repositoryWithOriginalShape();
    var pattern = TripPattern.of(id("replacement"))
      .withRoute(ROUTE)
      .withStopPattern(TransitRepositoryForTest.stopPattern(STOP_A, STOP_B, STOP_C))
      .withRealTimeStopPatternModified()
      .withOriginalTripPattern(ORIGINAL)
      .build();

    subject.ensureTripPatternShape(pattern);

    var original = repository.getTripPatternShape(ORIGINAL.getId());
    var geometry = repository.getTripPatternShape(pattern.getId());
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

    subject.ensureTripPatternShape(pattern);

    var geometry = repository.getTripPatternShape(pattern.getId());
    assertThat(geometry.get(0)).isEqualTo(
      makeLineString(STOP_A.getCoordinate(), STOP_B.getCoordinate())
    );
  }

  @Test
  void ensureTripPatternShapeIsIdempotent() {
    var pattern = TripPattern.of(id("added"))
      .withRoute(ROUTE)
      .withStopPattern(TransitRepositoryForTest.stopPattern(STOP_A, STOP_B, STOP_C))
      .withRealTimeAddedTrip()
      .build();

    subject.ensureTripPatternShape(pattern);
    var first = repository.getTripPatternShape(pattern.getId());
    subject.ensureTripPatternShape(pattern);

    assertThat(repository.getTripPatternShape(pattern.getId())).isSameInstanceAs(first);
  }

  @Test
  void doNotTouchTheRepositoryForScheduledPatterns() {
    var accessCount = new AtomicInteger();
    var writer = new RealTimeTripShapeWriter(() -> {
      accessCount.incrementAndGet();
      return repository;
    }, factory);

    writer.ensureTripPatternShape(ORIGINAL);

    assertThat(accessCount.get()).isEqualTo(0);
    assertThat(repository.getTripPatternShape(ORIGINAL.getId())).isNull();
  }

  @Test
  void writeOnlyToTheRealTimeRepository() {
    repositoryWithOriginalShape();
    var pattern = TripPattern.of(id("replacement"))
      .withRoute(ROUTE)
      .withStopPattern(TransitRepositoryForTest.stopPattern(STOP_A, STOP_B, STOP_C))
      .withRealTimeStopPatternModified()
      .withOriginalTripPattern(ORIGINAL)
      .build();
    var snapshotBefore = repository.createSnapshot();

    subject.ensureTripPatternShape(pattern);

    assertThat(repository.getTripPatternShape(pattern.getId())).isNotNull();
    assertThat(scheduledShapes.getTripPatternShape(pattern.getId())).isNull();
    assertThat(snapshotBefore.getTripPatternShape(pattern.getId())).isNull();
  }

  @Test
  void deriveFromAnOriginalWrittenEarlierInTheSameTransaction() {
    repositoryWithOriginalShape();
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

    subject.ensureTripPatternShape(firstUpdate);
    subject.ensureTripPatternShape(secondUpdate);

    var first = repository.getTripPatternShape(firstUpdate.getId());
    var second = repository.getTripPatternShape(secondUpdate.getId());
    assertThat(second.get(0)).isEqualTo(first.get(0));
    assertThat(second.get(1)).isEqualTo(first.get(1));
  }

  @Test
  void useStraightLinesWhenARealTimeOriginalHasNoShape() {
    repositoryWithOriginalShape();
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
    subject.ensureTripPatternShape(firstUpdate);
    repository.clear(firstUpdate.getFeedId());

    subject.ensureTripPatternShape(secondUpdate);

    var second = repository.getTripPatternShape(secondUpdate.getId());
    assertThat(second.get(0)).isEqualTo(
      makeLineString(STOP_A.getCoordinate(), STOP_B.getCoordinate())
    );
    assertThat(repository.getTripPatternShape(firstUpdate.getId())).isNull();
  }

  @Test
  void failIfTheOriginalPatternHasNoShape() {
    var pattern = TripPattern.of(id("replacement"))
      .withRoute(ROUTE)
      .withStopPattern(TransitRepositoryForTest.stopPattern(STOP_A, STOP_B))
      .withRealTimeStopPatternModified()
      .withOriginalTripPattern(ORIGINAL)
      .build();

    assertThrows(IllegalStateException.class, () -> subject.ensureTripPatternShape(pattern));
  }

  private void givenScheduledShape(
    FeedScopedId patternId,
    CompactLineStringSequence tripPatternShape
  ) {
    scheduledShapes = ScheduledShapeData.of(Map.of(patternId, tripPatternShape));
    repository = new DefaultShapeRepository(scheduledShapes);
  }

  private void repositoryWithOriginalShape() {
    givenScheduledShape(
      ORIGINAL.getId(),
      TripShapeFactory.fromLineStrings(
        List.of(
          makeLineString(STOP_A.getCoordinate(), STOP_X.getCoordinate(), STOP_B.getCoordinate()),
          makeLineString(STOP_B.getCoordinate(), STOP_Y.getCoordinate(), STOP_C.getCoordinate())
        ),
        DeduplicatorService.NOOP
      )
    );
  }
}
