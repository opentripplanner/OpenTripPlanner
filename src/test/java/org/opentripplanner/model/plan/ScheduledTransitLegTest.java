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
    var route = TransitModelForTest.route(id("2")).build();
    var pattern = TransitModelForTest
      .tripPattern("1", route)
      .withStopPattern(TransitModelForTest.stopPattern(3))
      .build();
    var leg = new ScheduledTransitLeg(
      null,
      pattern,
      0,
      2,
      TIME,
      TIME.plusMinutes(10),
      TIME.toLocalDate(),
      ZoneIds.BERLIN,
      null,
      null,
      100,
      null
    );

    assertEquals(List.of(), leg.fareProducts());
  }
}
