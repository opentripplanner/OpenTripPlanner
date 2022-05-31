package org.opentripplanner.model.modes;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.network.TransitMode;

public class AllowedTransitModeFilterTest {

  @Test
  public void testMainModeMatch() {
    final var ALLOWED_TRANSIT_MODE = AllowedTransitModeFilter.of(TransitMode.BUS);
    assertTrue(ALLOWED_TRANSIT_MODE.allows(TransitMode.BUS, null));
    assertTrue(ALLOWED_TRANSIT_MODE.allows(TransitMode.BUS, "something"));
    assertFalse(ALLOWED_TRANSIT_MODE.allows(TransitMode.RAIL, null));
    assertFalse(ALLOWED_TRANSIT_MODE.allows(TransitMode.RAIL, "something"));
  }

  @Test
  public void testSubmodeMatch() {
    final var SUBMODE = "shuttleBus";
    final var subject = AllowedTransitModeFilter.of(TransitMode.BUS, SUBMODE);

    assertTrue(subject.allows(TransitMode.BUS, SUBMODE));
    assertFalse(subject.allows(TransitMode.BUS, "other"));
    assertFalse(subject.allows(TransitMode.BUS, null));
  }

  @Test
  public void testUnknownSubmodeMatch() {
    final var ALLOWED_TRANSIT_MODE = AllowedTransitModeFilter.ofUnknownSubModes(TransitMode.BUS);

    assertTrue(ALLOWED_TRANSIT_MODE.allows(TransitMode.BUS, null));
    assertFalse(ALLOWED_TRANSIT_MODE.allows(TransitMode.BUS, "shuttleBus"));
  }
}
