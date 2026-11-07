package org.opentripplanner.service.shape.internal;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.service.shape.ScheduledShapeData;
import org.opentripplanner.service.shape.ShapeTestFactory;
import org.opentripplanner.service.shape.model.CompactLineStringSequence;

class DefaultShapeServiceTest {

  private static final FeedScopedId SCHEDULED_PATTERN = new FeedScopedId("F", "scheduled");
  private static final FeedScopedId RT_PATTERN = new FeedScopedId("F", "rt");
  private static final FeedScopedId UNKNOWN_PATTERN = new FeedScopedId("F", "unknown");

  private static final CompactLineStringSequence SCHEDULED_TRIP_PATTERN_GEOMETRY =
    ShapeTestFactory.straightLine(60.0);
  private static final CompactLineStringSequence RT_TRIP_PATTERN_GEOMETRY =
    ShapeTestFactory.straightLine(61.0);
  private static final CompactLineStringSequence OVERRIDING_TRIP_PATTERN_GEOMETRY =
    ShapeTestFactory.straightLine(62.0);

  private final DefaultShapeRepository repository = new DefaultShapeRepository(
    ScheduledShapeData.of(Map.of(SCHEDULED_PATTERN, SCHEDULED_TRIP_PATTERN_GEOMETRY))
  );

  @Test
  void scheduledGeometryIsServed() {
    var service = new DefaultShapeService(repository.createSnapshot());

    assertThat(service.getTripPatternGeometry(SCHEDULED_PATTERN)).isSameInstanceAs(
      SCHEDULED_TRIP_PATTERN_GEOMETRY
    );
  }

  @Test
  void realTimeGeometryIsServed() {
    repository.setTripPatternGeometry(RT_PATTERN, RT_TRIP_PATTERN_GEOMETRY);
    var service = new DefaultShapeService(repository.createSnapshot());

    assertThat(service.getTripPatternGeometry(RT_PATTERN)).isSameInstanceAs(
      RT_TRIP_PATTERN_GEOMETRY
    );
  }

  @Test
  void realTimeGeometryTakesPrecedenceOverScheduled() {
    repository.setTripPatternGeometry(SCHEDULED_PATTERN, OVERRIDING_TRIP_PATTERN_GEOMETRY);
    var service = new DefaultShapeService(repository.createSnapshot());

    assertThat(service.getTripPatternGeometry(SCHEDULED_PATTERN)).isSameInstanceAs(
      OVERRIDING_TRIP_PATTERN_GEOMETRY
    );
  }

  @Test
  void serviceOverTheMutableRepositorySeesUncommittedWrites() {
    var service = new DefaultShapeService(repository);
    repository.setTripPatternGeometry(RT_PATTERN, RT_TRIP_PATTERN_GEOMETRY);

    assertThat(service.getTripPatternGeometry(RT_PATTERN)).isSameInstanceAs(
      RT_TRIP_PATTERN_GEOMETRY
    );
  }

  @Test
  void serviceOverASnapshotDoesNotSeeLaterWrites() {
    var service = new DefaultShapeService(repository.createSnapshot());

    repository.setTripPatternGeometry(RT_PATTERN, RT_TRIP_PATTERN_GEOMETRY);

    assertThrows(IllegalStateException.class, () -> service.getTripPatternGeometry(RT_PATTERN));
  }

  @Test
  void serviceFailsForUnknownPattern() {
    var service = new DefaultShapeService(repository.createSnapshot());

    assertThrows(IllegalStateException.class, () ->
      service.getTripPatternGeometry(UNKNOWN_PATTERN)
    );
  }
}
