package org.opentripplanner.service.shape.internal;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;
import org.opentripplanner.service.shape.ScheduledShapeData;
import org.opentripplanner.service.shape.ShapeTestFactory;
import org.opentripplanner.service.shape.model.CompactLineStringSequence;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;

class ShapeRepositoryLifecycleTest {

  private static final TransitRepositoryForTest TEST_MODEL = TransitRepositoryForTest.of();
  private static final Route ROUTE = TransitRepositoryForTest.route("route").build();
  private static final StopPattern STOP_PATTERN = TransitRepositoryForTest.stopPattern(
    TEST_MODEL.stop("A").build(),
    TEST_MODEL.stop("B").build()
  );

  private static final TripPattern PATTERN_1 = TransitRepositoryForTest.tripPattern("P1", ROUTE)
    .withStopPattern(STOP_PATTERN)
    .build();
  private static final TripPattern PATTERN_2 = TransitRepositoryForTest.tripPattern("P2", ROUTE)
    .withStopPattern(STOP_PATTERN)
    .build();

  private static final CompactLineStringSequence TRIP_PATTERN_GEOMETRY_1 =
    ShapeTestFactory.straightLine(60.0);
  private static final CompactLineStringSequence TRIP_PATTERN_GEOMETRY_2 =
    ShapeTestFactory.straightLine(61.0);

  private final DefaultShapeRepository buffer = new DefaultShapeRepository(
    ScheduledShapeData.empty()
  );
  private final ShapeRepositoryLifecycle lifecycle = new ShapeRepositoryLifecycle(buffer);

  @Test
  void snapshotIsNotAffectedByLaterWritesToTheBuffer() {
    buffer.setTripPatternShape(PATTERN_1, TRIP_PATTERN_GEOMETRY_1);
    var snapshot = lifecycle.freeze(buffer);

    lifecycle.copyOnWrite(snapshot).setTripPatternShape(PATTERN_2, TRIP_PATTERN_GEOMETRY_2);

    assertThat(snapshot.getTripPatternShape(PATTERN_2.getId())).isNull();
    var next = lifecycle.freeze(buffer);
    assertThat(next.getTripPatternShape(PATTERN_1.getId())).isEqualTo(TRIP_PATTERN_GEOMETRY_1);
    assertThat(next.getTripPatternShape(PATTERN_2.getId())).isEqualTo(TRIP_PATTERN_GEOMETRY_2);
  }
}
