package org.opentripplanner.model.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.basic.TransitMode;

class ScheduledTransitLegBuilderTest {

  private static final TransitAlert ALERT = TransitAlert
    .of(id("alert"))
    .withDescriptionText(I18NString.of("alert"))
    .build();

  @Test
  void transferZoneId() {
    var pattern = TimetableRepositoryForTest.of().pattern(TransitMode.BUS).build();
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

  @Test
  void alerts() {
    var pattern = TimetableRepositoryForTest.of().pattern(TransitMode.BUS).build();
    var leg = new ScheduledTransitLegBuilder<>()
      .withZoneId(ZoneIds.BERLIN)
      .withServiceDate(LocalDate.of(2023, 11, 15))
      .withTripPattern(pattern)
      .withBoardStopIndexInPattern(0)
      .withAlightStopIndexInPattern(1)
      .build();

    leg.addAlert(ALERT);

    var newLeg = new ScheduledTransitLegBuilder<>(leg);

    var withScore = newLeg.withAccessibilityScore(4f).build();

    assertEquals(Set.of(ALERT), withScore.getTransitAlerts());
  }
}
