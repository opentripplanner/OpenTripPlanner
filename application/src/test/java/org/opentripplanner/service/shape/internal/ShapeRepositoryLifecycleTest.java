package org.opentripplanner.service.shape.internal;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.service.shape.ScheduledShapeData;
import org.opentripplanner.service.shape.ShapeTestFactory;
import org.opentripplanner.service.shape.model.CompactLineStringSequence;

class ShapeRepositoryLifecycleTest {

  private static final FeedScopedId PATTERN_1 = new FeedScopedId("F", "P1");
  private static final FeedScopedId PATTERN_2 = new FeedScopedId("F", "P2");

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
    buffer.setTripPatternGeometry(PATTERN_1, TRIP_PATTERN_GEOMETRY_1);
    var snapshot = lifecycle.freeze(buffer);

    lifecycle.copyOnWrite(snapshot).setTripPatternGeometry(PATTERN_2, TRIP_PATTERN_GEOMETRY_2);

    assertThat(snapshot.getTripPatternGeometry(PATTERN_2)).isNull();
    var next = lifecycle.freeze(buffer);
    assertThat(next.getTripPatternGeometry(PATTERN_1)).isEqualTo(TRIP_PATTERN_GEOMETRY_1);
    assertThat(next.getTripPatternGeometry(PATTERN_2)).isEqualTo(TRIP_PATTERN_GEOMETRY_2);
  }
}
