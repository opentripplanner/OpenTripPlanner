package org.opentripplanner.model.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.transit.model._data.TransitModelForTest.id;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.transit.model._data.TransitModelForTest;

class ScheduledTransitLegTest {

  static final ZonedDateTime TIME = OffsetDateTime
    .parse("2023-04-17T17:49:06+02:00")
    .toZonedDateTime();

  @Test
  void defaultFares() {
    var testModel = TransitModelForTest.of();
    var route = TransitModelForTest.route(id("2")).build();
    var pattern = TransitModelForTest
      .tripPattern("1", route)
      .withStopPattern(testModel.stopPattern(3))
      .build();
    var leg = new ScheduledTransitLegBuilder()
      .withTripTimes(null)
      .withTripPattern(pattern)
      .withBoardStopIndexInPattern(0)
      .withAlightStopIndexInPattern(2)
      .withStartTime(TIME)
      .withEndTime(TIME.plusMinutes(10))
      .withServiceDate(TIME.toLocalDate())
      .withZoneId(ZoneIds.BERLIN)
      .withGeneralizedCost(100)
      .build();

    assertEquals(List.of(), leg.fareProducts());
  }
}
