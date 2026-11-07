package org.opentripplanner.service.shape;

import static com.google.common.truth.Truth.assertThat;
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

    assertThat(empty.getTripPatternGeometry(id("A"))).isNull();
  }

  @Test
  void ofCopiesTheMapAndSharesTheGeometry() {
    var geometriesByTripPattern = new HashMap<>(Map.of(id("A"), TRIP_PATTERN_GEOMETRY));

    var data = ScheduledShapeData.of(geometriesByTripPattern);
    geometriesByTripPattern.put(id("B"), TRIP_PATTERN_GEOMETRY);

    assertThat(data.getTripPatternGeometry(id("A"))).isSameInstanceAs(TRIP_PATTERN_GEOMETRY);
    assertThat(data.getTripPatternGeometry(id("B"))).isNull();
  }
}
