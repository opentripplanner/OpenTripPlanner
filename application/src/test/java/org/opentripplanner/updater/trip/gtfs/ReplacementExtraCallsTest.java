package org.opentripplanner.updater.trip.gtfs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.BitSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.site.RegularStop;

class ReplacementExtraCallsTest {

  private static final TransitRepositoryForTest TEST_MODEL = TransitRepositoryForTest.of();
  private static final RegularStop A = TEST_MODEL.stop("A").build();
  private static final RegularStop B = TEST_MODEL.stop("B").build();
  private static final RegularStop C = TEST_MODEL.stop("C").build();
  private static final RegularStop D = TEST_MODEL.stop("D").build();

  @Test
  void identicalStopsHaveNoExtraCalls() {
    assertEquals(bits(), ReplacementExtraCalls.find(List.of(A, B), List.of(A, B)));
  }

  @Test
  void stopsAppendedAfterTheScheduledTerminusAreExtraCalls() {
    assertEquals(bits(2, 3), ReplacementExtraCalls.find(List.of(A, B), List.of(A, B, C, D)));
  }

  @Test
  void stopsInsertedInTheMiddleAreExtraCalls() {
    assertEquals(bits(1), ReplacementExtraCalls.find(List.of(A, C), List.of(A, B, C)));
  }

  @Test
  void stopsPrependedBeforeTheScheduledOriginAreExtraCalls() {
    assertEquals(bits(0), ReplacementExtraCalls.find(List.of(B, C), List.of(A, B, C)));
  }

  @Test
  void removedStopMeansNothingIsFlagged() {
    assertEquals(bits(), ReplacementExtraCalls.find(List.of(A, B, C), List.of(A, C, D)));
  }

  @Test
  void reorderedStopsMeanNothingIsFlagged() {
    assertEquals(bits(), ReplacementExtraCalls.find(List.of(A, B), List.of(B, A, C)));
  }

  private static BitSet bits(int... positions) {
    var set = new BitSet();
    for (int p : positions) {
      set.set(p);
    }
    return set;
  }
}
