package org.opentripplanner.model.plan.paging.cursor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class DeduplicationPageCutTest {

  public static final Instant DEPARTURE_TIME = Instant.ofEpochSecond(1_000_000);
  public static final Instant ARRIVAL_TIME = Instant.ofEpochSecond(2_000_000);
  public static final int GENERALIZED_COST = 1700;
  public static final int NUM_OF_TRANSFERS = 2;
  public static final boolean ON_STREET = false;

  @Test
  void testToString() {
    assertEquals(
      "[1970-01-12T13:46:40Z, 1970-01-24T03:33:20Z, $1700, Tx2, transit]",
      new DeduplicationPageCut(
        DEPARTURE_TIME,
        ARRIVAL_TIME,
        GENERALIZED_COST,
        NUM_OF_TRANSFERS,
        ON_STREET
      ).toString()
    );
  }
}
