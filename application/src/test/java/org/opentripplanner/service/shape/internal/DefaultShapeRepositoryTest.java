package org.opentripplanner.service.shape.internal;

import static com.google.common.truth.Truth.assertThat;
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
  private static final Route ROUTE_G = TransitRepositoryForTest.route(
    new FeedScopedId("G", "route")
  ).build();
  private static final Route ROUTE_H = TransitRepositoryForTest.route(
    new FeedScopedId("H", "route")
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
  private static final TripPattern OTHER_FEED_REAL_TIME = TripPattern.of(
    new FeedScopedId("G", "rt")
  )
    .withRoute(ROUTE_G)
    .withStopPattern(STOP_PATTERN)
    .build();
  private static final TripPattern THIRD_FEED_REAL_TIME = TripPattern.of(
    new FeedScopedId("H", "rt")
  )
    .withRoute(ROUTE_H)
    .withStopPattern(STOP_PATTERN)
    .build();

  private static final CompactLineStringSequence SCHEDULED_TRIP_PATTERN_GEOMETRY =
    ShapeTestFactory.straightLine(60.0);
  private static final CompactLineStringSequence REAL_TIME_TRIP_PATTERN_GEOMETRY =
    ShapeTestFactory.straightLine(61.0);
  private static final CompactLineStringSequence OVERRIDING_TRIP_PATTERN_GEOMETRY =
    ShapeTestFactory.straightLine(62.0);

  private final DefaultShapeRepository repository = new DefaultShapeRepository(
    ScheduledShapeData.of(Map.of(SCHEDULED.getId(), SCHEDULED_TRIP_PATTERN_GEOMETRY))
  );

  @Test
  void initialSnapshotContainsTheScheduledData() {
    var snapshot = repository.createSnapshot();

    assertThat(snapshot.getTripPatternShape(SCHEDULED.getId())).isEqualTo(
      SCHEDULED_TRIP_PATTERN_GEOMETRY
    );
    assertThat(snapshot.getTripPatternShape(REAL_TIME.getId())).isNull();
  }

  @Test
  void snapshotContainsRealTimeGeometry() {
    repository.setTripPatternShape(REAL_TIME, REAL_TIME_TRIP_PATTERN_GEOMETRY);

    var snapshot = repository.createSnapshot();

    assertThat(snapshot.getTripPatternShape(REAL_TIME.getId())).isEqualTo(
      REAL_TIME_TRIP_PATTERN_GEOMETRY
    );
    assertThat(snapshot.getTripPatternShape(SCHEDULED.getId())).isEqualTo(
      SCHEDULED_TRIP_PATTERN_GEOMETRY
    );
  }

  @Test
  void realTimeGeometryOverridesScheduled() {
    repository.setTripPatternShape(SCHEDULED, OVERRIDING_TRIP_PATTERN_GEOMETRY);

    assertThat(repository.getTripPatternShape(SCHEDULED.getId())).isEqualTo(
      OVERRIDING_TRIP_PATTERN_GEOMETRY
    );
    assertThat(repository.createSnapshot().getTripPatternShape(SCHEDULED.getId())).isEqualTo(
      OVERRIDING_TRIP_PATTERN_GEOMETRY
    );
  }

  @Test
  void clearRemovesRealTimeGeometriesOfTheFeedOnly() {
    repository.setTripPatternShape(REAL_TIME, REAL_TIME_TRIP_PATTERN_GEOMETRY);
    repository.setTripPatternShape(OTHER_FEED_REAL_TIME, REAL_TIME_TRIP_PATTERN_GEOMETRY);
    var snapshotBeforeClear = repository.createSnapshot();

    repository.clear("F");

    assertThat(repository.getTripPatternShape(REAL_TIME.getId())).isNull();
    assertThat(repository.getTripPatternShape(OTHER_FEED_REAL_TIME.getId())).isEqualTo(
      REAL_TIME_TRIP_PATTERN_GEOMETRY
    );
    assertThat(repository.getTripPatternShape(SCHEDULED.getId())).isEqualTo(
      SCHEDULED_TRIP_PATTERN_GEOMETRY
    );
    assertThat(snapshotBeforeClear.getTripPatternShape(REAL_TIME.getId())).isEqualTo(
      REAL_TIME_TRIP_PATTERN_GEOMETRY
    );
  }

  @Test
  void snapshotAfterClearDoesNotContainTheFeedAndGeometryCanBeAddedAgain() {
    repository.setTripPatternShape(REAL_TIME, REAL_TIME_TRIP_PATTERN_GEOMETRY);
    repository.createSnapshot();

    repository.clear("F");
    var snapshotAfterClear = repository.createSnapshot();
    repository.setTripPatternShape(REAL_TIME, OVERRIDING_TRIP_PATTERN_GEOMETRY);

    assertThat(snapshotAfterClear.getTripPatternShape(REAL_TIME.getId())).isNull();
    assertThat(snapshotAfterClear.getTripPatternShape(SCHEDULED.getId())).isEqualTo(
      SCHEDULED_TRIP_PATTERN_GEOMETRY
    );
    assertThat(repository.createSnapshot().getTripPatternShape(REAL_TIME.getId())).isEqualTo(
      OVERRIDING_TRIP_PATTERN_GEOMETRY
    );
  }

  @Test
  void snapshotContainingAFeedIsNotAffectedByLaterWritesToTheFeed() {
    repository.setTripPatternShape(REAL_TIME, REAL_TIME_TRIP_PATTERN_GEOMETRY);
    var snapshot = repository.createSnapshot();

    repository.setTripPatternShape(OTHER, OVERRIDING_TRIP_PATTERN_GEOMETRY);

    assertThat(snapshot.getTripPatternShape(OTHER.getId())).isNull();
    assertThat(snapshot.getTripPatternShape(REAL_TIME.getId())).isEqualTo(
      REAL_TIME_TRIP_PATTERN_GEOMETRY
    );
  }

  @Test
  void unchangedFeedIsStillServedWhenAnotherFeedIsWrittenOrCleared() {
    repository.setTripPatternShape(REAL_TIME, REAL_TIME_TRIP_PATTERN_GEOMETRY);
    repository.setTripPatternShape(OTHER_FEED_REAL_TIME, OVERRIDING_TRIP_PATTERN_GEOMETRY);
    repository.createSnapshot();

    repository.clear("F");
    repository.setTripPatternShape(THIRD_FEED_REAL_TIME, OVERRIDING_TRIP_PATTERN_GEOMETRY);
    var snapshot = repository.createSnapshot();

    assertThat(snapshot.getTripPatternShape(OTHER_FEED_REAL_TIME.getId())).isEqualTo(
      OVERRIDING_TRIP_PATTERN_GEOMETRY
    );
    assertThat(snapshot.getTripPatternShape(REAL_TIME.getId())).isNull();
  }

  @Test
  void clearOfAFeedWithoutGeometriesDoesNotChangeTheSnapshot() {
    repository.setTripPatternShape(REAL_TIME, REAL_TIME_TRIP_PATTERN_GEOMETRY);
    repository.createSnapshot();

    repository.clear("unknown feed");
    var snapshot = repository.createSnapshot();

    assertThat(snapshot.getTripPatternShape(REAL_TIME.getId())).isEqualTo(
      REAL_TIME_TRIP_PATTERN_GEOMETRY
    );
    assertThat(snapshot.getTripPatternShape(SCHEDULED.getId())).isEqualTo(
      SCHEDULED_TRIP_PATTERN_GEOMETRY
    );
  }

  @Test
  void snapshotIsNotAffectedByLaterWrites() {
    var snapshot = repository.createSnapshot();

    repository.setTripPatternShape(OTHER, REAL_TIME_TRIP_PATTERN_GEOMETRY);

    assertThat(snapshot.getTripPatternShape(OTHER.getId())).isNull();
    assertThat(repository.getTripPatternShape(OTHER.getId())).isEqualTo(
      REAL_TIME_TRIP_PATTERN_GEOMETRY
    );
  }

  @Test
  void rejectAMissingShape() {
    var exception = assertThrows(IllegalArgumentException.class, () ->
      repository.setTripPatternShape(OTHER, null)
    );
    assertThat(exception).hasMessageThat().contains("No shape for trip pattern");
    assertThat(repository.getTripPatternShape(OTHER.getId())).isNull();
  }

  @Test
  void rejectAShapeWithTheWrongNumberOfHops() {
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
    assertThat(repository.getTripPatternShape(OTHER.getId())).isNull();
  }
}
