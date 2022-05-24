package org.opentripplanner.model.modes;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.model.TransitSubMode;
import org.opentripplanner.transit.model.network.TransitMode;

public class AllowedTransitModeTest {

  @Test
  public void testMainModeMatch() {
    final var ALLOWED_TRANSIT_MODE = AllowedTransitMode.fromMainModeEnum(TransitMode.BUS);
    assertTrue(ALLOWED_TRANSIT_MODE.allows(TransitMode.BUS, null));
    assertTrue(ALLOWED_TRANSIT_MODE.allows(TransitMode.BUS, TransitSubMode.LOCALBUS));
    assertFalse(ALLOWED_TRANSIT_MODE.allows(TransitMode.RAIL, null));
    assertFalse(ALLOWED_TRANSIT_MODE.allows(TransitMode.RAIL, TransitSubMode.RAILSHUTTLE));
  }

  @Test
  public void testSubmodeMatch() {
    final var SUBMODE = TransitSubMode.SHUTTLEBUS;
    final var ALLOWED_TRANSIT_MODE = new AllowedTransitMode(TransitMode.BUS, SUBMODE);

    assertTrue(ALLOWED_TRANSIT_MODE.allows(TransitMode.BUS, SUBMODE));
    assertFalse(ALLOWED_TRANSIT_MODE.allows(TransitMode.BUS, TransitSubMode.REGIONALBUS));
    assertFalse(ALLOWED_TRANSIT_MODE.allows(TransitMode.BUS, null));
  }
}
