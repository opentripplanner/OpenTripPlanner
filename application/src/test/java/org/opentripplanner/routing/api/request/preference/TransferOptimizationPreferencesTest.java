package org.opentripplanner.routing.api.request.preference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.routing.api.request.preference.ImmutablePreferencesAsserts.assertEqualsAndHashCode;
import static org.opentripplanner.routing.api.request.preference.TransferOptimizationPreferences.DEFAULT;

import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.application.OTPFeature;

class TransferOptimizationPreferencesTest {

  private static final double MIN_SAFE_WAIT_TIME_FACTOR = 7.0;
  private static final double BACK_TRAVEL_WAIT_TIME_FACTOR = 1.2;
  private static final double EXTRA_STOP_BOARD_ALIGHT_COSTS_FACTOR = 100.0;

  private final TransferOptimizationPreferences subject = TransferOptimizationPreferences.of()
    .withOptimizeTransferWaitTime(false)
    .withMinSafeWaitTimeFactor(MIN_SAFE_WAIT_TIME_FACTOR)
    .withBackTravelWaitTimeFactor(BACK_TRAVEL_WAIT_TIME_FACTOR)
    .withExtraStopBoardAlightCostsFactor(EXTRA_STOP_BOARD_ALIGHT_COSTS_FACTOR)
    .build();

  @Test
  void optimizeTransferPriority() {
    OTPFeature.TransferConstraints.testOn(() -> {
      assertTrue(DEFAULT.optimizeTransferPriority());
      assertTrue(subject.optimizeTransferPriority());
    });
    OTPFeature.TransferConstraints.testOff(() -> {
      assertFalse(DEFAULT.optimizeTransferPriority());
      assertFalse(subject.optimizeTransferPriority());
    });
  }

  @Test
  void optimizeTransferWaitTime() {
    assertTrue(DEFAULT.optimizeTransferWaitTime());
    assertFalse(subject.optimizeTransferWaitTime());
  }

  @Test
  void minSafeWaitTimeFactor() {
    assertEquals(5.0, DEFAULT.minSafeWaitTimeFactor());
    assertEquals(MIN_SAFE_WAIT_TIME_FACTOR, subject.minSafeWaitTimeFactor());
  }

  @Test
  void backTravelWaitTimeFactor() {
    assertEquals(1.0, DEFAULT.backTravelWaitTimeFactor());
    assertEquals(BACK_TRAVEL_WAIT_TIME_FACTOR, subject.backTravelWaitTimeFactor());
  }

  @Test
  void extraStopBoardAlightCostsFactor() {
    assertEquals(0.0, DEFAULT.extraStopBoardAlightCostsFactor());
    assertEquals(EXTRA_STOP_BOARD_ALIGHT_COSTS_FACTOR, subject.extraStopBoardAlightCostsFactor());
  }

  @Test
  void testToString() {
    assertEquals("TransferOptimizationPreferences{}", DEFAULT.toString());
    assertEquals(
      "TransferOptimizationPreferences{skipOptimizeWaitTime, minSafeWaitTimeFactor: 7.0, backTravelWaitTimeFactor: 1.2, extraStopBoardAlightCostsFactor: 100.0}",
      subject.toString()
    );
  }

  @Test
  void testCopyOfEqualsAndHashCode() {
    // Return same object if no value is set
    assertSame(
      TransferOptimizationPreferences.DEFAULT,
      TransferOptimizationPreferences.of().build()
    );
    assertSame(subject, subject.copyOf().build());

    // Create a copy, make a change and set it back again to force creating a new object
    var other = subject.copyOf().withMinSafeWaitTimeFactor(0.0).build();
    var copy = other.copyOf().withMinSafeWaitTimeFactor(MIN_SAFE_WAIT_TIME_FACTOR).build();
    assertEqualsAndHashCode(subject, other, copy);
  }
}
