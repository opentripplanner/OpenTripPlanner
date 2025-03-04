package org.opentripplanner.raptor.rangeraptor.multicriteria.passthrough;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.raptor.api.request.RaptorViaLocation;
import org.opentripplanner.raptor.rangeraptor.internalapi.PassThroughPointsService;

class BitSetPassThroughPointsServiceTest {

  private static final int PASS_THROUGH_SEQ_NO_1 = 1;
  private static final int PASS_THROUGH_SEQ_NO_2 = 2;

  private static final int STOP_11 = 0;
  private static final int STOP_12 = 1;
  private static final int STOP_13 = 2;
  private static final int[] STOPS_1 = new int[] { STOP_11, STOP_12, STOP_13 };
  private static final int STOP_21 = 3;
  private static final int STOP_22 = 4;
  private static final int STOP_23 = 5;
  private static final int[] STOPS_2 = new int[] { STOP_21, STOP_22, STOP_23 };
  private static final int STOP_31 = 6;

  private static final PassThroughPointsService SUBJECT = BitSetPassThroughPointsService.of(
    List.of(
      RaptorViaLocation.passThrough("PT1").addPassThroughStops(STOPS_1).build(),
      RaptorViaLocation.passThrough("PT2").addPassThroughStops(STOPS_2).build()
    )
  );
  /**
   * We expect the c2 value at the destination to be the same as the number of pass-through
   * points in the request.
   */
  private static final int EXPECTED_C2_AT_DESTINATION = 2;

  static Stream<Arguments> passThroughPointTestCases() {
    return Stream.of(
      Arguments.of(PASS_THROUGH_SEQ_NO_1, true, STOP_11),
      Arguments.of(PASS_THROUGH_SEQ_NO_1, true, STOP_12),
      Arguments.of(PASS_THROUGH_SEQ_NO_1, true, STOP_13),
      Arguments.of(PASS_THROUGH_SEQ_NO_2, true, STOP_21),
      Arguments.of(PASS_THROUGH_SEQ_NO_2, true, STOP_22),
      Arguments.of(PASS_THROUGH_SEQ_NO_2, true, STOP_23)
    );
  }

  @ParameterizedTest
  @MethodSource("passThroughPointTestCases")
  void passThroughPointTest(int expectedSeqNr, boolean isPassThroughPoint, int stopIndex) {
    assertEquals(isPassThroughPoint, SUBJECT.isPassThroughPoint(stopIndex));
    AtomicBoolean c2Updated = new AtomicBoolean(false);
    SUBJECT.updateC2Value(expectedSeqNr - 1, newC2 -> {
      assertEquals(expectedSeqNr, newC2);
      c2Updated.set(true);
    });
    assertTrue(c2Updated.get(), "The c2 update is not performed");
    SUBJECT.updateC2Value(expectedSeqNr - 2, newC2 ->
      fail("A visited pass-through-point should not increase the c2. New C2: " + newC2)
    );
    SUBJECT.updateC2Value(expectedSeqNr, newC2 ->
      fail(
        "A pass-through-point where the previous point is not visited should not increase the C2. New C2: " +
        newC2
      )
    );
    assertTrue(SUBJECT.acceptC2AtDestination().test(EXPECTED_C2_AT_DESTINATION));
    assertFalse(SUBJECT.acceptC2AtDestination().test(EXPECTED_C2_AT_DESTINATION - 1));
    assertFalse(SUBJECT.acceptC2AtDestination().test(EXPECTED_C2_AT_DESTINATION + 1));
  }

  @Test
  void notAPassthroughPoint() {
    assertFalse(SUBJECT.isPassThroughPoint(STOP_31));
  }
}
