package org.opentripplanner.raptor.rangeraptor.destination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor.rangeraptor.destination.DestinationArrivalParetoSetComparators.paretoComparator;
import static org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetCost.NONE;
import static org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetTime.USE_ARRIVAL_TIME;
import static org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetTime.USE_TIMETABLE;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.api.TestRaptorPath;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor.api.model.RelaxFunction;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetTime;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.stop.StopArrivalFactoryC1;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.raptor.spi.SearchDirection;
import org.opentripplanner.raptor.util.paretoset.ParetoSet;

class EagerPathBagTest {

  private static final int ITERATION_TIME = 1000;
  private static final int STOP = 5;
  private static final StopArrivalFactoryC1<RaptorTripSchedule> FACTORY =
    new StopArrivalFactoryC1<>();

  private int mapperCalls = 0;

  private final EagerPathBag<RaptorTripSchedule> subject = new EagerPathBag<>(
    this::mapToPath,
    RaptorTestConstants::stopIndexToName
  );

  @Test
  void anAcceptedArrivalGetsAPath() {
    var set = createSet(USE_ARRIVAL_TIME);

    assertEquals(true, set.add(arrival(ITERATION_TIME, 1100)));

    assertEquals(List.of(1100), arrivalTimes());
    assertEquals(1, mapperCalls);
  }

  @Test
  void aRejectedArrivalIsNeitherMappedNorStored() {
    var set = createSet(USE_ARRIVAL_TIME);
    set.add(arrival(ITERATION_TIME, 1100));

    assertEquals(false, set.add(arrival(ITERATION_TIME, 1200)));

    assertEquals(List.of(1100), arrivalTimes());
    assertEquals(1, mapperCalls);
  }

  @Test
  void theBagIsInSyncWithTheSetWhenAnArrivalIsDropped() {
    var set = createSet(USE_ARRIVAL_TIME);
    set.add(arrival(ITERATION_TIME, 1100));

    set.add(arrival(ITERATION_TIME, 1000));

    assertEquals(1, set.size());
    assertEquals(List.of(1000), arrivalTimes(), "The path of a dropped arrival must be removed");
    assertEquals(2, mapperCalls);
  }

  @Test
  void thePathsAreSortedWithTheEarliestArrivalFirst() {
    // The two arrivals do not dominate each other: one departs later, the other arrives earlier
    var set = createSet(USE_TIMETABLE);
    set.add(arrival(ITERATION_TIME + 60, 1200));
    set.add(arrival(ITERATION_TIME, 1100));
    set.add(arrival(ITERATION_TIME + 30, 1150));

    assertEquals(3, set.size());
    assertEquals(List.of(1100, 1150, 1200), arrivalTimes());
  }

  @Test
  void anEmptyBagGivesAnEmptyList() {
    assertEquals(List.of(), subject.toList());
  }

  private ParetoSet<DestinationArrival<RaptorTripSchedule>> createSet(ParetoSetTime time) {
    return ParetoSet.of(
      paretoComparator(time, SearchDirection.FORWARD, NONE, RelaxFunction.NORMAL, null),
      subject
    );
  }

  private List<Integer> arrivalTimes() {
    return subject.toList().stream().map(RaptorPath::endTimeInclusivePenalty).toList();
  }

  private RaptorPath<RaptorTripSchedule> mapToPath(DestinationArrival<RaptorTripSchedule> a) {
    ++mapperCalls;
    return new TestRaptorPath(
      a.iterationDepartureTime(),
      a.iterationDepartureTime(),
      a.arrivalTime(),
      a.numberOfTransfers(),
      a.c1(),
      a.c2()
    );
  }

  private static DestinationArrival<RaptorTripSchedule> arrival(
    int iterationDepartureTime,
    int arrivalTime
  ) {
    var access = FACTORY.createAccessStopArrival(
      iterationDepartureTime,
      TestAccessEgress.walk(STOP, 60)
    );
    return new DestinationArrival<>(
      TestAccessEgress.walk(STOP, 60),
      access,
      iterationDepartureTime,
      iterationDepartureTime,
      arrivalTime,
      0,
      0
    );
  }
}
