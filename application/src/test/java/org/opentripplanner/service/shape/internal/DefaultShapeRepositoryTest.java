package org.opentripplanner.service.shape.internal;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.deduplicator.DeduplicatorService;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.service.shape.ScheduledShapeData;
import org.opentripplanner.service.shape.ShapeTestFactory;
import org.opentripplanner.service.shape.model.CompactLineStringSequence;
import org.opentripplanner.service.shape.model.TripShapeFactory;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;

class DefaultShapeRepositoryTest {

  private static final TransitRepositoryForTest TEST_MODEL = TransitRepositoryForTest.of();
  private static final StopPattern STOP_PATTERN = TransitRepositoryForTest.stopPattern(
    TEST_MODEL.stop("A").build(),
    TEST_MODEL.stop("B").build()
  );

  private static final Route ROUTE_F = TransitRepositoryForTest.route(
    new FeedScopedId("F", "route")
  ).build();

  private static final TripPattern SCHEDULED = TripPattern.of(new FeedScopedId("F", "scheduled"))
    .withRoute(ROUTE_F)
    .withStopPattern(STOP_PATTERN)
    .build();
  private static final TripPattern REAL_TIME = TripPattern.of(new FeedScopedId("F", "rt"))
    .withRoute(ROUTE_F)
    .withStopPattern(STOP_PATTERN)
    .build();
  private static final TripPattern OTHER = TripPattern.of(new FeedScopedId("F", "other"))
    .withRoute(ROUTE_F)
    .withStopPattern(STOP_PATTERN)
    .build();

  private static final CompactLineStringSequence SCHEDULED_TRIP_PATTERN_SHAPE =
    ShapeTestFactory.straightLine(60.0);
  private static final CompactLineStringSequence REAL_TIME_TRIP_PATTERN_SHAPE =
    ShapeTestFactory.straightLine(61.0);
  private static final CompactLineStringSequence OVERRIDING_TRIP_PATTERN_SHAPE =
    ShapeTestFactory.straightLine(62.0);

  private final DefaultShapeRepository repository = new DefaultShapeRepository(
    ScheduledShapeData.of(Map.of(SCHEDULED.getId(), SCHEDULED_TRIP_PATTERN_SHAPE))
  );

  @Test
  void initialSnapshotContainsTheScheduledData() {
    var snapshot = repository.createSnapshot();

    assertEquals(SCHEDULED_TRIP_PATTERN_SHAPE, snapshot.getTripPatternShape(SCHEDULED.getId()));
    assertNull(snapshot.getTripPatternShape(REAL_TIME.getId()));
  }

  @Test
  void snapshotContainsRealTimeTripPatternShape() {
    repository.setTripPatternShape(REAL_TIME, REAL_TIME_TRIP_PATTERN_SHAPE);

    var snapshot = repository.createSnapshot();

    assertEquals(REAL_TIME_TRIP_PATTERN_SHAPE, snapshot.getTripPatternShape(REAL_TIME.getId()));
    assertEquals(SCHEDULED_TRIP_PATTERN_SHAPE, snapshot.getTripPatternShape(SCHEDULED.getId()));
  }

  @Test
  void realTimeTripPatternShapeOverridesScheduled() {
    repository.setTripPatternShape(SCHEDULED, OVERRIDING_TRIP_PATTERN_SHAPE);

    assertEquals(OVERRIDING_TRIP_PATTERN_SHAPE, repository.getTripPatternShape(SCHEDULED.getId()));
    assertEquals(
      OVERRIDING_TRIP_PATTERN_SHAPE,
      repository.createSnapshot().getTripPatternShape(SCHEDULED.getId())
    );
  }

  @Test
  void clearRemovesRealTimeTripPatternShapesButKeepsTheScheduledData() {
    repository.setTripPatternShape(REAL_TIME, REAL_TIME_TRIP_PATTERN_SHAPE);
    repository.setTripPatternShape(SCHEDULED, OVERRIDING_TRIP_PATTERN_SHAPE);

    repository.clear("F");

    assertNull(repository.getTripPatternShape(REAL_TIME.getId()));
    assertEquals(SCHEDULED_TRIP_PATTERN_SHAPE, repository.getTripPatternShape(SCHEDULED.getId()));
  }

  @Test
  void snapshotAfterClearDoesNotContainTheFeedAndTripPatternShapeCanBeAddedAgain() {
    repository.setTripPatternShape(REAL_TIME, REAL_TIME_TRIP_PATTERN_SHAPE);
    repository.createSnapshot();

    repository.clear("F");
    var snapshotAfterClear = repository.createSnapshot();
    repository.setTripPatternShape(REAL_TIME, OVERRIDING_TRIP_PATTERN_SHAPE);

    assertNull(snapshotAfterClear.getTripPatternShape(REAL_TIME.getId()));
    assertEquals(
      SCHEDULED_TRIP_PATTERN_SHAPE,
      snapshotAfterClear.getTripPatternShape(SCHEDULED.getId())
    );
    assertEquals(
      OVERRIDING_TRIP_PATTERN_SHAPE,
      repository.createSnapshot().getTripPatternShape(REAL_TIME.getId())
    );
  }

  @Test
  void clearOfAFeedWithoutTripPatternShapesDoesNotChangeTheSnapshot() {
    repository.setTripPatternShape(REAL_TIME, REAL_TIME_TRIP_PATTERN_SHAPE);
    repository.createSnapshot();

    repository.clear("unknown feed");
    var snapshot = repository.createSnapshot();

    assertEquals(REAL_TIME_TRIP_PATTERN_SHAPE, snapshot.getTripPatternShape(REAL_TIME.getId()));
    assertEquals(SCHEDULED_TRIP_PATTERN_SHAPE, snapshot.getTripPatternShape(SCHEDULED.getId()));
  }

  @Test
  void snapshotIsNotAffectedByLaterWrites() {
    var snapshot = repository.createSnapshot();

    repository.setTripPatternShape(OTHER, REAL_TIME_TRIP_PATTERN_SHAPE);

    assertNull(snapshot.getTripPatternShape(OTHER.getId()));
    assertEquals(REAL_TIME_TRIP_PATTERN_SHAPE, repository.getTripPatternShape(OTHER.getId()));
  }

  @Test
  void rejectAMissingTripPatternShape() {
    var exception = assertThrows(IllegalArgumentException.class, () ->
      repository.setTripPatternShape(OTHER, null)
    );
    assertThat(exception).hasMessageThat().contains("No shape for trip pattern");
    assertNull(repository.getTripPatternShape(OTHER.getId()));
  }

  @Test
  void rejectATripPatternShapeWithTheWrongNumberOfHops() {
    var threeHops = TripShapeFactory.fromLineStrings(
      List.of(
        GeometryUtils.makeLineString(10.0, 60.0, 10.1, 60.1),
        GeometryUtils.makeLineString(10.1, 60.1, 10.2, 60.2),
        GeometryUtils.makeLineString(10.2, 60.2, 10.3, 60.3)
      ),
      DeduplicatorService.NOOP
    );

    var exception = assertThrows(IllegalArgumentException.class, () ->
      repository.setTripPatternShape(OTHER, threeHops)
    );
    assertThat(exception).hasMessageThat().contains("3 hops, expected 1");
    assertNull(repository.getTripPatternShape(OTHER.getId()));
  }
}
