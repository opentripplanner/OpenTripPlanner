package org.opentripplanner.raptor.rangeraptor.multicriteria;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestTransfer;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.AbstractStopArrival;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.AccessStopArrival;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.TransferStopArrival;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.TransitStopArrival;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

public class StopArrivalStateParetoSetTest {

  // 08:35 in seconds
  private static final int A_TIME = ((8 * 60) + 35) * 60;
  private static final int ANY = 3;
  private static final int ROUND_1 = 1;
  private static final int ROUND_2 = 2;
  private static final int ROUND_3 = 3;
  private static final RaptorTripSchedule ANY_TRIP = TestTripSchedule
    .schedule("10:00 10:30")
    .build();

  // In this test each stop is used to identify the pareto vector - it is just one
  // ParetoSet "subject" with multiple "stops" in it. The stop have no effect on
  // the Pareto functionality.
  private static final int STOP_1 = 1;
  private static final int STOP_2 = 2;
  private static final int STOP_3 = 3;
  private static final int STOP_4 = 4;
  private static final int STOP_5 = 5;
  private static final int STOP_6 = 6;

  // Make sure all "base" arrivals have the same cost
  private static final int BASE_COST = 1;
  private static final AbstractStopArrival<RaptorTripSchedule> ACCESS_ARRIVAL = newAccessStopState(
    999,
    5,
    BASE_COST
  );
  private final StopArrivalParetoSet<RaptorTripSchedule> subject = new StopArrivalParetoSet<>(null);
  private static final AbstractStopArrival<RaptorTripSchedule> TRANSIT_L1 = newTransitStopState(
    ROUND_1,
    998,
    10,
    BASE_COST
  );

  @Test
  public void addOneElementToSet() {
    subject.add(newAccessStopState(STOP_1, 10, ANY));
    assertStopsInSet(STOP_1);
  }

  private static final AbstractStopArrival<RaptorTripSchedule> TRANSIT_L2 = newTransitStopState(
    ROUND_2,
    997,
    20,
    BASE_COST
  );

  @Test
  public void testTimeDominance() {
    subject.add(newAccessStopState(STOP_1, 10, ANY));
    subject.add(newAccessStopState(STOP_2, 9, ANY));
    subject.add(newAccessStopState(STOP_3, 9, ANY));
    subject.add(newAccessStopState(STOP_4, 11, ANY));
    assertStopsInSet(STOP_2);
  }

  @Test
  public void testRoundDominance() {
    subject.add(newTransferStopState(ROUND_1, STOP_1, 10, ANY));
    subject.add(newTransferStopState(ROUND_2, STOP_2, 10, ANY));
    assertStopsInSet(STOP_1);
  }

  @Test
  public void testCostDominance() {
    subject.add(newTransferStopState(ROUND_1, STOP_1, ANY, 20));
    subject.add(newTransferStopState(ROUND_1, STOP_2, ANY, 10));
    assertStopsInSet(STOP_2);
  }

  @Test
  public void testRoundAndTimeDominance() {
    subject.add(newTransferStopState(ROUND_1, STOP_1, 10, ANY));
    subject.add(newTransferStopState(ROUND_1, STOP_2, 8, ANY));

    assertStopsInSet(STOP_2);

    subject.add(newTransferStopState(ROUND_2, STOP_3, 8, ANY));

    assertStopsInSet(STOP_2);

    subject.add(newTransferStopState(ROUND_2, STOP_4, 7, ANY));

    assertStopsInSet(STOP_2, STOP_4);

    subject.add(newTransferStopState(ROUND_3, STOP_5, 6, ANY));

    assertStopsInSet(STOP_2, STOP_4, STOP_5);

    subject.add(newTransferStopState(ROUND_3, STOP_6, 6, ANY));

    assertStopsInSet(STOP_2, STOP_4, STOP_5);
  }

  /**
   * During the same round transfers should not dominate transits, but this is handled by the worker
   * state (2-phase transfer calculation), not by the pareto-set. Using the pareto-set for this
   * would cause unnecessary exploration in the following round.
   */
  @Test
  public void testTransitAndTransferDoesNotAffectDominance() {
    subject.add(newAccessStopState(STOP_1, 20, ANY));
    subject.add(newTransitStopState(ROUND_1, STOP_2, 10, ANY));
    subject.add(newTransferStopState(ROUND_1, STOP_4, 8, ANY));
    assertStopsInSet(STOP_1, STOP_4);
  }

  private static AccessStopArrival<RaptorTripSchedule> newAccessStopState(
    int stop,
    int accessDurationInSeconds,
    int cost
  ) {
    return new AccessStopArrival<>(
      A_TIME,
      TestAccessEgress.walk(stop, accessDurationInSeconds, cost)
    );
  }

  private static TransitStopArrival<RaptorTripSchedule> newTransitStopState(
    int round,
    int stop,
    int arrivalTime,
    int cost
  ) {
    var prev = prev(round);
    return new TransitStopArrival<>(prev, stop, arrivalTime, cost, ANY_TRIP);
  }

  private static TransferStopArrival<RaptorTripSchedule> newTransferStopState(
    int round,
    int stop,
    int arrivalTime,
    int cost
  ) {
    var prev = prev(round);
    return new TransferStopArrival<>(
      prev,
      TestTransfer.transfer(stop, ANY, cost - prev.cost()),
      arrivalTime
    );
  }

  private static AbstractStopArrival<RaptorTripSchedule> prev(int round) {
    switch (round) {
      case 1:
        return ACCESS_ARRIVAL;
      case 2:
        return TRANSIT_L1;
      case 3:
        return TRANSIT_L2;
      default:
        throw new IllegalArgumentException();
    }
  }

  private void assertStopsInSet(int... expStopIndexes) {
    int[] result = subject.stream().mapToInt(AbstractStopArrival::stop).sorted().toArray();
    assertEquals(Arrays.toString(expStopIndexes), Arrays.toString(result), "Stop indexes");
  }
}
