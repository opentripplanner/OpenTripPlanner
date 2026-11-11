package org.opentripplanner.service.shape.internal;

import static com.google.common.truth.Truth.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.service.shape.ScheduledShapeData;
import org.opentripplanner.service.shape.ShapeTestFactory;
import org.opentripplanner.service.shape.model.CompactLineStringSequence;

class DefaultShapeRepositoryTest {

  private static final FeedScopedId SCHEDULED = new FeedScopedId("F", "scheduled");
  private static final FeedScopedId REAL_TIME = new FeedScopedId("F", "rt");
  private static final FeedScopedId OTHER = new FeedScopedId("F", "other");

  private static final CompactLineStringSequence SCHEDULED_TRIP_PATTERN_GEOMETRY =
    ShapeTestFactory.straightLine(60.0);
  private static final CompactLineStringSequence REAL_TIME_TRIP_PATTERN_GEOMETRY =
    ShapeTestFactory.straightLine(61.0);
  private static final CompactLineStringSequence OVERRIDING_TRIP_PATTERN_GEOMETRY =
    ShapeTestFactory.straightLine(62.0);

  private final DefaultShapeRepository repository = new DefaultShapeRepository(
    ScheduledShapeData.of(Map.of(SCHEDULED, SCHEDULED_TRIP_PATTERN_GEOMETRY))
  );

  @Test
  void initialSnapshotContainsTheScheduledData() {
    var snapshot = repository.createSnapshot();

    assertThat(snapshot.getTripPatternGeometry(SCHEDULED)).isEqualTo(
      SCHEDULED_TRIP_PATTERN_GEOMETRY
    );
    assertThat(snapshot.getTripPatternGeometry(REAL_TIME)).isNull();
  }

  @Test
  void snapshotContainsRealTimeGeometry() {
    repository.setTripPatternGeometry(REAL_TIME, REAL_TIME_TRIP_PATTERN_GEOMETRY);

    var snapshot = repository.createSnapshot();

    assertThat(snapshot.getTripPatternGeometry(REAL_TIME)).isEqualTo(
      REAL_TIME_TRIP_PATTERN_GEOMETRY
    );
    assertThat(snapshot.getTripPatternGeometry(SCHEDULED)).isEqualTo(
      SCHEDULED_TRIP_PATTERN_GEOMETRY
    );
  }

  @Test
  void realTimeGeometryOverridesScheduled() {
    repository.setTripPatternGeometry(SCHEDULED, OVERRIDING_TRIP_PATTERN_GEOMETRY);

    assertThat(repository.getTripPatternGeometry(SCHEDULED)).isEqualTo(
      OVERRIDING_TRIP_PATTERN_GEOMETRY
    );
    assertThat(repository.createSnapshot().getTripPatternGeometry(SCHEDULED)).isEqualTo(
      OVERRIDING_TRIP_PATTERN_GEOMETRY
    );
  }

  @Test
  void clearRemovesRealTimeGeometriesOfTheFeedOnly() {
    var otherFeedPattern = new FeedScopedId("G", "rt");
    repository.setTripPatternGeometry(REAL_TIME, REAL_TIME_TRIP_PATTERN_GEOMETRY);
    repository.setTripPatternGeometry(otherFeedPattern, REAL_TIME_TRIP_PATTERN_GEOMETRY);
    var snapshotBeforeClear = repository.createSnapshot();

    repository.clear("F");

    assertThat(repository.getTripPatternGeometry(REAL_TIME)).isNull();
    assertThat(repository.getTripPatternGeometry(otherFeedPattern)).isEqualTo(
      REAL_TIME_TRIP_PATTERN_GEOMETRY
    );
    assertThat(repository.getTripPatternGeometry(SCHEDULED)).isEqualTo(
      SCHEDULED_TRIP_PATTERN_GEOMETRY
    );
    assertThat(snapshotBeforeClear.getTripPatternGeometry(REAL_TIME)).isEqualTo(
      REAL_TIME_TRIP_PATTERN_GEOMETRY
    );
  }

  @Test
  void snapshotAfterClearDoesNotContainTheFeedAndGeometryCanBeAddedAgain() {
    repository.setTripPatternGeometry(REAL_TIME, REAL_TIME_TRIP_PATTERN_GEOMETRY);
    repository.createSnapshot();

    repository.clear("F");
    var snapshotAfterClear = repository.createSnapshot();
    repository.setTripPatternGeometry(REAL_TIME, OVERRIDING_TRIP_PATTERN_GEOMETRY);

    assertThat(snapshotAfterClear.getTripPatternGeometry(REAL_TIME)).isNull();
    assertThat(snapshotAfterClear.getTripPatternGeometry(SCHEDULED)).isEqualTo(
      SCHEDULED_TRIP_PATTERN_GEOMETRY
    );
    assertThat(repository.createSnapshot().getTripPatternGeometry(REAL_TIME)).isEqualTo(
      OVERRIDING_TRIP_PATTERN_GEOMETRY
    );
  }

  @Test
  void snapshotContainingAFeedIsNotAffectedByLaterWritesToTheFeed() {
    repository.setTripPatternGeometry(REAL_TIME, REAL_TIME_TRIP_PATTERN_GEOMETRY);
    var snapshot = repository.createSnapshot();

    repository.setTripPatternGeometry(OTHER, OVERRIDING_TRIP_PATTERN_GEOMETRY);

    assertThat(snapshot.getTripPatternGeometry(OTHER)).isNull();
    assertThat(snapshot.getTripPatternGeometry(REAL_TIME)).isEqualTo(
      REAL_TIME_TRIP_PATTERN_GEOMETRY
    );
  }

  @Test
  void unchangedFeedIsStillServedWhenAnotherFeedIsWrittenOrCleared() {
    var otherFeedPattern = new FeedScopedId("G", "rt");
    repository.setTripPatternGeometry(REAL_TIME, REAL_TIME_TRIP_PATTERN_GEOMETRY);
    repository.setTripPatternGeometry(otherFeedPattern, OVERRIDING_TRIP_PATTERN_GEOMETRY);
    repository.createSnapshot();

    repository.clear("F");
    repository.setTripPatternGeometry(
      new FeedScopedId("H", "rt"),
      OVERRIDING_TRIP_PATTERN_GEOMETRY
    );
    var snapshot = repository.createSnapshot();

    assertThat(snapshot.getTripPatternGeometry(otherFeedPattern)).isEqualTo(
      OVERRIDING_TRIP_PATTERN_GEOMETRY
    );
    assertThat(snapshot.getTripPatternGeometry(REAL_TIME)).isNull();
  }

  @Test
  void clearOfAFeedWithoutGeometriesDoesNotChangeTheSnapshot() {
    repository.setTripPatternGeometry(REAL_TIME, REAL_TIME_TRIP_PATTERN_GEOMETRY);
    repository.createSnapshot();

    repository.clear("unknown feed");
    var snapshot = repository.createSnapshot();

    assertThat(snapshot.getTripPatternGeometry(REAL_TIME)).isEqualTo(
      REAL_TIME_TRIP_PATTERN_GEOMETRY
    );
    assertThat(snapshot.getTripPatternGeometry(SCHEDULED)).isEqualTo(
      SCHEDULED_TRIP_PATTERN_GEOMETRY
    );
  }

  @Test
  void snapshotIsNotAffectedByLaterWrites() {
    var snapshot = repository.createSnapshot();

    repository.setTripPatternGeometry(OTHER, REAL_TIME_TRIP_PATTERN_GEOMETRY);

    assertThat(snapshot.getTripPatternGeometry(OTHER)).isNull();
    assertThat(repository.getTripPatternGeometry(OTHER)).isEqualTo(REAL_TIME_TRIP_PATTERN_GEOMETRY);
  }
}
