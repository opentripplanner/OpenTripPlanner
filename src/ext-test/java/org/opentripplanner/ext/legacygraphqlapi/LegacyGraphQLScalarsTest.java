package org.opentripplanner.ext.legacygraphqlapi;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class LegacyGraphQLScalarsTest {

  @Test
  void duration() {
    var string = LegacyGraphQLScalars.durationScalar
      .getCoercing()
      .serialize(Duration.ofMinutes(30));
    assertEquals("PT30M", string);
  }
}
