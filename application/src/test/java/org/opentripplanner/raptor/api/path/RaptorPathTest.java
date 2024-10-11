package org.opentripplanner.raptor.api.path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.api.TestRaptorPath;
import org.opentripplanner.raptor.api.model.GeneralizedCostRelaxFunction;

class RaptorPathTest {

  private static final int VALUE = 150;
  private static final int SMALL = 100;

  private final TestRaptorPath subject = new TestRaptorPath(
    VALUE,
    VALUE,
    VALUE,
    VALUE,
    VALUE,
    VALUE,
    VALUE
  );
  private final TestRaptorPath same = new TestRaptorPath(
    VALUE,
    VALUE,
    VALUE,
    VALUE,
    VALUE,
    VALUE,
    VALUE
  );
  private final TestRaptorPath smallIterationDepartureTime = new TestRaptorPath(
    SMALL,
    VALUE,
    VALUE,
    VALUE,
    VALUE,
    VALUE,
    VALUE
  );
  private final TestRaptorPath smallDepartureTime = new TestRaptorPath(
    VALUE,
    SMALL,
    VALUE,
    VALUE,
    VALUE,
    VALUE,
    VALUE
  );
  private final TestRaptorPath smallArrivalTime = new TestRaptorPath(
    VALUE,
    VALUE,
    SMALL,
    VALUE,
    VALUE,
    VALUE,
    VALUE
  );
  private final TestRaptorPath smallDuration = new TestRaptorPath(
    VALUE,
    VALUE,
    VALUE,
    SMALL,
    VALUE,
    VALUE,
    VALUE
  );
  private final TestRaptorPath smallNumberOfTransfers = new TestRaptorPath(
    VALUE,
    VALUE,
    VALUE,
    VALUE,
    SMALL,
    VALUE,
    VALUE
  );
  private final TestRaptorPath smallC1 = new TestRaptorPath(
    VALUE,
    VALUE,
    VALUE,
    VALUE,
    VALUE,
    SMALL,
    VALUE
  );

  @Test
  void compareIterationDepartureTime() {
    assertFalse(RaptorPath.compareIterationDepartureTime(subject, subject));
    assertFalse(RaptorPath.compareIterationDepartureTime(subject, same));
    assertTrue(RaptorPath.compareIterationDepartureTime(subject, smallIterationDepartureTime));
    assertFalse(RaptorPath.compareIterationDepartureTime(smallIterationDepartureTime, subject));
  }

  @Test
  void compareDepartureTime() {
    assertFalse(RaptorPath.compareDepartureTime(subject, subject));
    assertFalse(RaptorPath.compareDepartureTime(subject, same));
    assertTrue(RaptorPath.compareDepartureTime(subject, smallDepartureTime));
    assertFalse(RaptorPath.compareDepartureTime(smallDepartureTime, subject));
  }

  @Test
  void compareArrivalTime() {
    assertFalse(RaptorPath.compareArrivalTime(subject, subject));
    assertFalse(RaptorPath.compareArrivalTime(subject, same));
    assertFalse(RaptorPath.compareArrivalTime(subject, smallArrivalTime));
    assertTrue(RaptorPath.compareArrivalTime(smallArrivalTime, subject));
  }

  @Test
  void compareDuration() {
    assertFalse(RaptorPath.compareDurationInclusivePenalty(subject, subject));
    assertFalse(RaptorPath.compareDurationInclusivePenalty(subject, same));
    assertFalse(RaptorPath.compareDurationInclusivePenalty(subject, smallDuration));
    assertTrue(RaptorPath.compareDurationInclusivePenalty(smallDuration, subject));
  }

  @Test
  void compareNumberOfTransfers() {
    assertFalse(RaptorPath.compareNumberOfTransfers(subject, subject));
    assertFalse(RaptorPath.compareNumberOfTransfers(subject, same));
    assertFalse(RaptorPath.compareNumberOfTransfers(subject, smallNumberOfTransfers));
    assertTrue(RaptorPath.compareNumberOfTransfers(smallNumberOfTransfers, subject));
  }

  @Test
  void compareC1() {
    assertFalse(RaptorPath.compareC1(subject, subject));
    assertFalse(RaptorPath.compareC1(subject, same));
    assertFalse(RaptorPath.compareC1(subject, smallC1));
    assertTrue(RaptorPath.compareC1(smallC1, subject));
    assertTrue(
      RaptorPath.compareC1(GeneralizedCostRelaxFunction.of(1.25, 26), subject, smallArrivalTime)
    );
  }
}
