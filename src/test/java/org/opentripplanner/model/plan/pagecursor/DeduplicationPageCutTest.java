package org.opentripplanner.model.plan.pagecursor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class DeduplicationPageCutTest {

  @Test
  void testToString() {
    assertEquals(
      "[1970-01-24T03:33:20Z, 1970-01-12T13:46:40Z, $1700, Tx2, transit]",
      new DeduplicationPageCut(
        Instant.ofEpochSecond(1_000_000),
        Instant.ofEpochSecond(2_000_000),
        1700,
        2,
        false
      )
        .toString()
    );
  }
}
