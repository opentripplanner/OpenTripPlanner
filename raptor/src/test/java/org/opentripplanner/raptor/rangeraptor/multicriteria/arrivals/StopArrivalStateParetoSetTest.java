package org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestTransfer;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.stop.ArrivalParetoSetComparatorFactory;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.stop.McStopArrival;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.stop.StopArrivalFactoryC1;
import org.opentripplanner.raptor.rangeraptor.multicriteria.ride.PatternRideC1;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.raptor.util.paretoset.ParetoSet;

public class StopArrivalStateParetoSetTest {

  // 08:35 in seconds
  private static final int A_TIME = ((8 * 60) + 35) * 60;
  private static final int ANY = 3;
  private static final int ROUND_1 = 1;
  private static final int ROUND_2 = 2;
  private static final int ROUND_3 = 3;
  private static final RaptorTripSchedule ANY_TRIP = TestTripSchedule.schedule(
    "10:00 10:30"
  ).build();

  // In this test, each stop is used to identify the pareto vector - it is just one
  // ParetoSet "subject" with multiple "stops" in it. The stop has no effect on
  // the Pareto functionality - the stop is not a criteria in the pareto-function.
  private static final int STOP_1 = 1;
  private static final int STOP_2 = 2;
  private static final int STOP_3 = 3;
  private static final int STOP_4 = 4;
  private static final int STOP_5 = 5;
  private static final int STOP_6 = 6;

  // Make sure all "base" arrivals have the same cost
  private static final int BASE_C1 = 1;

  private static final StopArrivalFactoryC1<RaptorTripSchedule> STOP_ARRIVAL_FACTORY =
    new StopArrivalFactoryC1<>();
  private static final McStopArrival<RaptorTripSchedule> ACCESS_ARRIVAL = newAccessStopState(
    999,
    5,
    BASE_C1
  );

  private static final McStopArrival<RaptorTripSchedule> TRANSIT_L1 = newTransitStopState(
    ROUND_1,
    998,
    10,
    BASE_C1
  );

  private static final McStopArrival<RaptorTripSchedule> TRANSIT_L2 = newTransitStopState(
    ROUND_2,
    997,
    20,
    BASE_C1
  );
  private static final ArrivalParetoSetComparatorFactory<
    McStopArrival<RaptorTripSchedule>
  > COMPARATOR_FACTORY = ArrivalParetoSetComparatorFactory.ofCompareC1();

  private ParetoSet<McStopArrival<RaptorTripSchedule>> subject = ParetoSet.of(
    COMPARATOR_FACTORY.compareArrivalTimeRoundAndCost()
  );

  @Test
  public void addOneElementToSet() {
    subject.add(newAccessStopState(STOP_1, 10, ANY));
    assertStopsInSet(subject, STOP_1);
  }

  @Test
  public void testTimeDominance() {
    subject.add(newAccessStopState(STOP_1, 10, ANY));
    subject.add(newAccessStopState(STOP_2, 9, ANY));
    subject.add(newAccessStopState(STOP_3, 9, ANY));
    subject.add(newAccessStopState(STOP_4, 11, ANY));
    assertStopsInSet(subject, STOP_2);
  }

  @Test
  public void testRoundDominance() {
    subject.add(newTransferStopState(ROUND_1, STOP_1, 10, ANY));
    subject.add(newTransferStopState(ROUND_2, STOP_2, 10, ANY));
    assertStopsInSet(subject, STOP_1);
  }

  @Test
  public void testCostDominance() {
    subject.add(newTransferStopState(ROUND_1, STOP_1, ANY, 20));
    subject.add(newTransferStopState(ROUND_1, STOP_2, ANY, 10));
    assertStopsInSet(subject, STOP_2);
  }

  @Test
  public void testRoundAndTimeDominance() {
    subject.add(newTransferStopState(ROUND_1, STOP_1, 10, ANY));
    subject.add(newTransferStopState(ROUND_1, STOP_2, 8, ANY));

    assertStopsInSet(subject, STOP_2);

    subject.add(newTransferStopState(ROUND_2, STOP_3, 8, ANY));

    assertStopsInSet(subject, STOP_2);

    subject.add(newTransferStopState(ROUND_2, STOP_4, 7, ANY));

    assertStopsInSet(subject, STOP_2, STOP_4);

    subject.add(newTransferStopState(ROUND_3, STOP_5, 6, ANY));

    assertStopsInSet(subject, STOP_2, STOP_4, STOP_5);

    subject.add(newTransferStopState(ROUND_3, STOP_6, 6, ANY));

    assertStopsInSet(subject, STOP_2, STOP_4, STOP_5);
  }

  private static McStopArrival<RaptorTripSchedule> newAccessStopState(
    int stop,
    int accessDurationInSeconds,
    int cost
  ) {
    return STOP_ARRIVAL_FACTORY.createAccessStopArrival(
      A_TIME,
      TestAccessEgress.walk(stop, accessDurationInSeconds, cost)
    );
  }

  private static McStopArrival<RaptorTripSchedule> newTransitStopState(
    int round,
    int stop,
    int arrivalTime,
    int cost
  ) {
    var prev = prev(round);
    var anyRide = new PatternRideC1<>(prev, ANY, ANY, ANY, ANY, ANY, ANY, ANY_TRIP);
    return STOP_ARRIVAL_FACTORY.createTransitStopArrival(anyRide, stop, arrivalTime, cost);
  }

  private static McStopArrival<RaptorTripSchedule> newTransferStopState(
    int round,
    int stop,
    int arrivalTime,
    int cost
  ) {
    var prev = prev(round);
    return STOP_ARRIVAL_FACTORY.createTransferStopArrival(
      prev,
      TestTransfer.transfer(stop, ANY, cost - prev.c1()),
      arrivalTime
    );
  }

  private static McStopArrival<RaptorTripSchedule> prev(int round) {
    return switch (round) {
      case 1 -> ACCESS_ARRIVAL;
      case 2 -> TRANSIT_L1;
      case 3 -> TRANSIT_L2;
      default -> throw new IllegalArgumentException();
    };
  }

  private void assertStopsInSet(
    ParetoSet<McStopArrival<RaptorTripSchedule>> subject,
    int... expStopIndexes
  ) {
    int[] result = subject.stream().mapToInt(McStopArrival::stop).sorted().toArray();
    assertEquals(Arrays.toString(expStopIndexes), Arrays.toString(result), "Stop indexes");
  }
}
