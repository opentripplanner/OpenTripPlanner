package org.opentripplanner.service.shape.internal;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.service.shape.ScheduledShapeData;
import org.opentripplanner.service.shape.ShapeTestFactory;
import org.opentripplanner.service.shape.model.CompactLineStringSequence;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;

class DefaultShapeServiceTest {

  private static final TransitRepositoryForTest TEST_MODEL = TransitRepositoryForTest.of();
  private static final Route ROUTE = TransitRepositoryForTest.route("route").build();
  private static final StopPattern STOP_PATTERN = TransitRepositoryForTest.stopPattern(
    TEST_MODEL.stop("A").build(),
    TEST_MODEL.stop("B").build()
  );

  private static final TripPattern SCHEDULED_PATTERN = TransitRepositoryForTest.tripPattern(
    "scheduled",
    ROUTE
  )
    .withStopPattern(STOP_PATTERN)
    .build();
  private static final TripPattern RT_PATTERN = TransitRepositoryForTest.tripPattern("rt", ROUTE)
    .withStopPattern(STOP_PATTERN)
    .build();
  private static final FeedScopedId UNKNOWN_PATTERN = new FeedScopedId("F", "unknown");

  private static final CompactLineStringSequence SCHEDULED_TRIP_PATTERN_GEOMETRY =
    ShapeTestFactory.straightLine(60.0);
  private static final CompactLineStringSequence RT_TRIP_PATTERN_GEOMETRY =
    ShapeTestFactory.straightLine(61.0);
  private static final CompactLineStringSequence OVERRIDING_TRIP_PATTERN_GEOMETRY =
    ShapeTestFactory.straightLine(62.0);

  private final DefaultShapeRepository repository = new DefaultShapeRepository(
    ScheduledShapeData.of(Map.of(SCHEDULED_PATTERN.getId(), SCHEDULED_TRIP_PATTERN_GEOMETRY))
  );

  @Test
  void scheduledGeometryIsServed() {
    var service = new DefaultShapeService(repository.createSnapshot());

    assertThat(service.getTripPatternShape(SCHEDULED_PATTERN.getId())).isSameInstanceAs(
      SCHEDULED_TRIP_PATTERN_GEOMETRY
    );
  }

  @Test
  void realTimeGeometryIsServed() {
    repository.setTripPatternShape(RT_PATTERN, RT_TRIP_PATTERN_GEOMETRY);
    var service = new DefaultShapeService(repository.createSnapshot());

    assertThat(service.getTripPatternShape(RT_PATTERN.getId())).isSameInstanceAs(
      RT_TRIP_PATTERN_GEOMETRY
    );
  }

  @Test
  void realTimeGeometryTakesPrecedenceOverScheduled() {
    repository.setTripPatternShape(SCHEDULED_PATTERN, OVERRIDING_TRIP_PATTERN_GEOMETRY);
    var service = new DefaultShapeService(repository.createSnapshot());

    assertThat(service.getTripPatternShape(SCHEDULED_PATTERN.getId())).isSameInstanceAs(
      OVERRIDING_TRIP_PATTERN_GEOMETRY
    );
  }

  @Test
  void serviceOverTheMutableRepositorySeesUncommittedWrites() {
    var service = new DefaultShapeService(repository);
    repository.setTripPatternShape(RT_PATTERN, RT_TRIP_PATTERN_GEOMETRY);

    assertThat(service.getTripPatternShape(RT_PATTERN.getId())).isSameInstanceAs(
      RT_TRIP_PATTERN_GEOMETRY
    );
  }

  @Test
  void serviceOverASnapshotDoesNotSeeLaterWrites() {
    var service = new DefaultShapeService(repository.createSnapshot());

    repository.setTripPatternShape(RT_PATTERN, RT_TRIP_PATTERN_GEOMETRY);

    var exception = assertThrows(IllegalArgumentException.class, () ->
      service.getTripPatternShape(RT_PATTERN.getId())
    );
    assertThat(exception).hasMessageThat().contains("No shape found");
  }

  @Test
  void serviceFailsForUnknownPattern() {
    var service = new DefaultShapeService(repository.createSnapshot());

    var exception = assertThrows(IllegalArgumentException.class, () ->
      service.getTripPatternShape(UNKNOWN_PATTERN)
    );
    assertThat(exception).hasMessageThat().contains("No shape found");
  }
}
