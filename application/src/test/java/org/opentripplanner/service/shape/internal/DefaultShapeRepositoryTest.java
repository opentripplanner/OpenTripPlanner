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

    assertThat(snapshot.getTripPatternShape(SCHEDULED)).isEqualTo(SCHEDULED_TRIP_PATTERN_GEOMETRY);
    assertThat(snapshot.getTripPatternShape(REAL_TIME)).isNull();
  }

  @Test
  void snapshotContainsRealTimeGeometry() {
    repository.setTripPatternShape(REAL_TIME, REAL_TIME_TRIP_PATTERN_GEOMETRY);

    var snapshot = repository.createSnapshot();

    assertThat(snapshot.getTripPatternShape(REAL_TIME)).isEqualTo(REAL_TIME_TRIP_PATTERN_GEOMETRY);
    assertThat(snapshot.getTripPatternShape(SCHEDULED)).isEqualTo(SCHEDULED_TRIP_PATTERN_GEOMETRY);
  }

  @Test
  void realTimeGeometryOverridesScheduled() {
    repository.setTripPatternShape(SCHEDULED, OVERRIDING_TRIP_PATTERN_GEOMETRY);

    assertThat(repository.getTripPatternShape(SCHEDULED)).isEqualTo(
      OVERRIDING_TRIP_PATTERN_GEOMETRY
    );
    assertThat(repository.createSnapshot().getTripPatternShape(SCHEDULED)).isEqualTo(
      OVERRIDING_TRIP_PATTERN_GEOMETRY
    );
  }

  @Test
  void clearRemovesRealTimeGeometriesOfTheFeedOnly() {
    var otherFeedPattern = new FeedScopedId("G", "rt");
    repository.setTripPatternShape(REAL_TIME, REAL_TIME_TRIP_PATTERN_GEOMETRY);
    repository.setTripPatternShape(otherFeedPattern, REAL_TIME_TRIP_PATTERN_GEOMETRY);
    var snapshotBeforeClear = repository.createSnapshot();

    repository.clear("F");

    assertThat(repository.getTripPatternShape(REAL_TIME)).isNull();
    assertThat(repository.getTripPatternShape(otherFeedPattern)).isEqualTo(
      REAL_TIME_TRIP_PATTERN_GEOMETRY
    );
    assertThat(repository.getTripPatternShape(SCHEDULED)).isEqualTo(
      SCHEDULED_TRIP_PATTERN_GEOMETRY
    );
    assertThat(snapshotBeforeClear.getTripPatternShape(REAL_TIME)).isEqualTo(
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

    assertThat(snapshotAfterClear.getTripPatternShape(REAL_TIME)).isNull();
    assertThat(snapshotAfterClear.getTripPatternShape(SCHEDULED)).isEqualTo(
      SCHEDULED_TRIP_PATTERN_GEOMETRY
    );
    assertThat(repository.createSnapshot().getTripPatternShape(REAL_TIME)).isEqualTo(
      OVERRIDING_TRIP_PATTERN_GEOMETRY
    );
  }

  @Test
  void snapshotContainingAFeedIsNotAffectedByLaterWritesToTheFeed() {
    repository.setTripPatternShape(REAL_TIME, REAL_TIME_TRIP_PATTERN_GEOMETRY);
    var snapshot = repository.createSnapshot();

    repository.setTripPatternShape(OTHER, OVERRIDING_TRIP_PATTERN_GEOMETRY);

    assertThat(snapshot.getTripPatternShape(OTHER)).isNull();
    assertThat(snapshot.getTripPatternShape(REAL_TIME)).isEqualTo(REAL_TIME_TRIP_PATTERN_GEOMETRY);
  }

  @Test
  void unchangedFeedIsStillServedWhenAnotherFeedIsWrittenOrCleared() {
    var otherFeedPattern = new FeedScopedId("G", "rt");
    repository.setTripPatternShape(REAL_TIME, REAL_TIME_TRIP_PATTERN_GEOMETRY);
    repository.setTripPatternShape(otherFeedPattern, OVERRIDING_TRIP_PATTERN_GEOMETRY);
    repository.createSnapshot();

    repository.clear("F");
    repository.setTripPatternShape(new FeedScopedId("H", "rt"), OVERRIDING_TRIP_PATTERN_GEOMETRY);
    var snapshot = repository.createSnapshot();

    assertThat(snapshot.getTripPatternShape(otherFeedPattern)).isEqualTo(
      OVERRIDING_TRIP_PATTERN_GEOMETRY
    );
    assertThat(snapshot.getTripPatternShape(REAL_TIME)).isNull();
  }

  @Test
  void clearOfAFeedWithoutGeometriesDoesNotChangeTheSnapshot() {
    repository.setTripPatternShape(REAL_TIME, REAL_TIME_TRIP_PATTERN_GEOMETRY);
    repository.createSnapshot();

    repository.clear("unknown feed");
    var snapshot = repository.createSnapshot();

    assertThat(snapshot.getTripPatternShape(REAL_TIME)).isEqualTo(REAL_TIME_TRIP_PATTERN_GEOMETRY);
    assertThat(snapshot.getTripPatternShape(SCHEDULED)).isEqualTo(SCHEDULED_TRIP_PATTERN_GEOMETRY);
  }

  @Test
  void snapshotIsNotAffectedByLaterWrites() {
    var snapshot = repository.createSnapshot();

    repository.setTripPatternShape(OTHER, REAL_TIME_TRIP_PATTERN_GEOMETRY);

    assertThat(snapshot.getTripPatternShape(OTHER)).isNull();
    assertThat(repository.getTripPatternShape(OTHER)).isEqualTo(REAL_TIME_TRIP_PATTERN_GEOMETRY);
  }
}
