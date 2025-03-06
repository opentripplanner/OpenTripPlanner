package org.opentripplanner.routing.algorithm.transferoptimization.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opentripplanner.model.transfer.TransferPriority.ALLOWED;
import static org.opentripplanner.model.transfer.TransferPriority.NOT_ALLOWED;
import static org.opentripplanner.model.transfer.TransferPriority.PREFERRED;
import static org.opentripplanner.model.transfer.TransferPriority.RECOMMENDED;
import static org.opentripplanner.routing.algorithm.transferoptimization.services.TransferGeneratorDummy.dummyTransferGenerator;
import static org.opentripplanner.utils.time.TimeUtils.time;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.transfer.ConstrainedTransfer;
import org.opentripplanner.model.transfer.TransferPriority;
import org.opentripplanner.raptorlegacy._data.RaptorTestConstants;
import org.opentripplanner.raptorlegacy._data.api.PathUtils;
import org.opentripplanner.raptorlegacy._data.transit.TestTripSchedule;

/**
 * <pre>
 * POSSIBLE TRANSFERS
 * Transfers        B-C 1m     C-D 2m       D-E 3m     E-F 4m      F-G 5m
 * Constraint       ALLOWED  RECOMMENDED  PREFERRED  GUARANTEED  STAY_SEATED
 * Trip 1  A 10:02  B 10:10    C 10:15     D 10:20     E 10:25     F 10:30
 * Trip 2           C 10:13    D 10:18     E 10:24     G 10:30     G 10:36   H 10:40
 * </pre>
 * <p>
 * Case: There is 5 possible places to transfer in this setup. We want to test that the correct one
 * is picked according to the constraint. We can test all relevant cases by changing the egress
 * stop, since the transfers are ordered with the highest priority last. Transfer in the same stop
 * is NOT_ALLOWED.
 * <p>
 * Expect: The highest priority should be picked
 * <p>
 * Module under test: We are testing the Optimized Transfer Service, not Routing it self. So, the
 * path will always include two trips with one transfer selected even where single trip might be
 * found by the router.
 * <p>
 * Note! This test uses some of the constants and utility methods of {@link
 * OptimizePathDomainServiceTest}
 */
@SuppressWarnings("SameParameterValue")
public class OptimizePathDomainServiceConstrainedTest implements RaptorTestConstants {

  /**
   * The exact start time to walk to stop A to catch Trip_1 with 40s board slack
   */
  private final int START_TIME_T1 = time("10:00:20");

  // Given
  TestTripSchedule trip1 = TestTripSchedule.schedule()
    .pattern("T1", STOP_A, STOP_B, STOP_C, STOP_D, STOP_E, STOP_F)
    .times("10:02 10:10 10:15 10:20 10:25 10:30")
    .build();

  TestTripSchedule trip2 = TestTripSchedule.schedule()
    .pattern("T2", STOP_C, STOP_D, STOP_E, STOP_F, STOP_G, STOP_H)
    .times("10:13 10:18 10:24 10:30 10:36 10:40")
    .build();

  TransferGenerator<TestTripSchedule> transfers = dummyTransferGenerator(
    List.of(
      TestTransferBuilder.tx(trip1, STOP_B, trip2, STOP_C).priority(ALLOWED).walk(D1m).build(),
      TestTransferBuilder.tx(trip1, STOP_C, trip2, STOP_D).priority(RECOMMENDED).walk(D2m).build(),
      TestTransferBuilder.tx(trip1, STOP_D, trip2, STOP_E).priority(PREFERRED).walk(D3m).build(),
      TestTransferBuilder.tx(trip1, STOP_E, trip2, STOP_F).guaranteed().walk(D4m).build(),
      TestTransferBuilder.tx(trip1, STOP_F, trip2, STOP_G).staySeated().walk(D5m).build(),
      TestTransferBuilder.tx(trip1, STOP_C, trip2, STOP_C).priority(NOT_ALLOWED).build(),
      TestTransferBuilder.tx(trip1, STOP_D, trip2, STOP_D).priority(NOT_ALLOWED).build(),
      TestTransferBuilder.tx(trip1, STOP_E, trip2, STOP_E).priority(NOT_ALLOWED).build(),
      TestTransferBuilder.tx(trip1, STOP_F, trip2, STOP_F).priority(NOT_ALLOWED).build()
    )
  );

  @Test
  public void testTransferPriorityAllowed() {
    testPriority(
      STOP_D,
      ALLOWED,
      "A ~ BUS T1 10:02 10:10 ~ B ~ Walk 1m ~ C ~ BUS T2 10:13 10:18 ~ D [10:01:20 10:18:20 17m Tₓ1 C₁1_120 Tₚ3_300]"
    );
  }

  @Test
  public void testTransferPriorityRecommended() {
    testPriority(
      STOP_E,
      RECOMMENDED,
      "A ~ BUS T1 10:02 10:15 ~ C ~ Walk 2m ~ D ~ BUS T2 10:18 10:24 ~ E [10:01:20 10:24:20 23m Tₓ1 C₁1_540 Tₚ3_200]"
    );
  }

  @Test
  public void testTransferPriorityPreferred() {
    testPriority(
      STOP_F,
      PREFERRED,
      "A ~ BUS T1 10:02 10:20 ~ D ~ Walk 3m ~ E ~ BUS T2 10:24 10:30 ~ F [10:01:20 10:30:20 29m Tₓ1 C₁1_960 Tₚ3_100]"
    );
  }

  @Test
  public void testTransferGuaranteed() {
    testGuaranteed(
      STOP_G,
      "A ~ BUS T1 10:02 10:25 ~ E ~ Walk 4m ~ F ~ BUS T2 10:30 10:36 ~ G [10:01:20 10:36:20 35m Tₓ1 C₁2_350 Tₚ2_300]"
    );
  }

  @Test
  public void testTransferStaySeated() {
    testStaySeated(
      STOP_H,
      "A ~ BUS T1 10:02 10:30 ~ F ~ Walk 5m ~ G ~ BUS T2 10:36 10:40 ~ H [10:01:20 10:40:20 39m Tₓ0 C₁2_650 Tₚ1_300]"
    );
  }

  /* private methods */

  private void testStaySeated(int egressStop, String expItinerary) {
    doTest(egressStop, true, false, ALLOWED, expItinerary);
  }

  private void testGuaranteed(int egressStop, String expItinerary) {
    doTest(egressStop, false, true, ALLOWED, expItinerary);
  }

  private void testPriority(int egressStop, TransferPriority expPriority, String expItinerary) {
    doTest(egressStop, false, false, expPriority, expItinerary);
  }

  private void doTest(
    int egressStop,
    boolean expStaySeated,
    boolean expGuaranteed,
    TransferPriority expPriority,
    String expItinerary
  ) {
    var original = OptimizePathDomainServiceTest.pathBuilder()
      .access(START_TIME_T1, STOP_A)
      .bus(trip1, STOP_B)
      .walk(D1m, STOP_C)
      .bus(trip2, egressStop)
      .egress(D0s);

    var subject = OptimizePathDomainServiceTest.subject(transfers, null);

    // Find the path with the lowest cost
    var result = subject.findBestTransitPath(original);

    assertEquals(expItinerary, PathUtils.pathsToString(result));

    // Verify the attached Transfer is exist and is valid
    var firstTransitLeg = result.iterator().next().accessLeg().nextTransitLeg();
    assertNotNull(firstTransitLeg);

    var txConstraints = (ConstrainedTransfer) firstTransitLeg.getConstrainedTransferAfterLeg();

    if (expPriority != null) {
      assertNotNull(txConstraints);
    }

    if (txConstraints != null) {
      var c = txConstraints.getTransferConstraint();
      assertEquals(expPriority, c.getPriority(), txConstraints.toString());
      assertEquals(expStaySeated, c.isStaySeated(), txConstraints.toString());
      assertEquals(expGuaranteed, c.isGuaranteed(), txConstraints.toString());
    }
  }
}
