package org.opentripplanner.model.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.basic.TransitMode;

class ScheduledTransitLegBuilderTest {

  @Test
  void transferZoneId() {
    var pattern = TransitModelForTest.of().pattern(TransitMode.BUS).build();
    var leg = new ScheduledTransitLegBuilder<>()
      .withZoneId(ZoneIds.BERLIN)
      .withServiceDate(LocalDate.of(2023, 11, 15))
      .withTripPattern(pattern)
      .withBoardStopIndexInPattern(0)
      .withAlightStopIndexInPattern(1)
      .build();

    var newLeg = new ScheduledTransitLegBuilder<>(leg);
    assertEquals(ZoneIds.BERLIN, newLeg.zoneId());

    var withScore = newLeg.withAccessibilityScore(4f).build();

    assertEquals(ZoneIds.BERLIN, withScore.getZoneId());
  }
}
