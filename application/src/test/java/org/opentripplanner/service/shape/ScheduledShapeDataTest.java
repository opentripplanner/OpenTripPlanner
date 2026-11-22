package org.opentripplanner.service.shape;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.service.shape.model.CompactLineStringSequence;

class ScheduledShapeDataTest {

  private static final CompactLineStringSequence TRIP_PATTERN_GEOMETRY =
    ShapeTestFactory.straightLine(60.0);

  @Test
  void emptyHasNoGeometry() {
    var empty = ScheduledShapeData.empty();

    assertNull(empty.getTripPatternShape(id("A")));
  }

  @Test
  void ofCopiesTheMapAndSharesTheGeometry() {
    var shapesByTripPattern = new HashMap<>(Map.of(id("A"), TRIP_PATTERN_GEOMETRY));

    var data = ScheduledShapeData.of(shapesByTripPattern);
    shapesByTripPattern.put(id("B"), TRIP_PATTERN_GEOMETRY);

    assertSame(TRIP_PATTERN_GEOMETRY, data.getTripPatternShape(id("A")));
    assertNull(data.getTripPatternShape(id("B")));
  }
}
