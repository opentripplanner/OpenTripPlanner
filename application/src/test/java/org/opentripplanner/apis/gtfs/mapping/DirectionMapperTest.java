package org.opentripplanner.apis.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.AbsoluteDirection;
import org.opentripplanner.model.plan.RelativeDirection;

class DirectionMapperTest {

  @Test
  void absoluteDirection() {
    Arrays
      .stream(AbsoluteDirection.values())
      .forEach(d -> {
        var mapped = DirectionMapper.map(d);
        assertEquals(d.toString(), mapped.toString());
      });
  }

  @Test
  void relativeDirection() {
    Arrays
      .stream(RelativeDirection.values())
      .filter(v -> v != RelativeDirection.ENTER_OR_EXIT_STATION)
      .forEach(d -> {
        var mapped = DirectionMapper.map(d);
        assertEquals(d.toString(), mapped.toString());
      });
  }
}
