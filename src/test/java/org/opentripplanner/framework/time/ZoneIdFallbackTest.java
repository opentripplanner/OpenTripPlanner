package org.opentripplanner.framework.time;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;

class ZoneIdFallbackTest {

  @Test
  void fallback() {
    assertEquals(ZoneIds.UTC, ZoneIdFallback.zoneId(null));
  }

  @Test
  void keepOriginal() {
    assertEquals(ZoneIds.BERLIN, ZoneIdFallback.zoneId(ZoneIds.BERLIN));
  }
}
