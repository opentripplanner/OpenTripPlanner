package org.opentripplanner.raptor.rangeraptor.standard.stoparrivals.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestTransfer;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.view.PathLegType;
import org.opentripplanner.raptor.rangeraptor.lifecycle.LifeCycleEventPublisher;
import org.opentripplanner.raptor.rangeraptor.lifecycle.LifeCycleSubscriptions;
import org.opentripplanner.raptor.rangeraptor.standard.stoparrivals.StdStopArrivals;
import org.opentripplanner.raptor.rangeraptor.standard.stoparrivals.StdStopArrivalsState;
import org.opentripplanner.raptor.rangeraptor.transit.ForwardTransitCalculator;

/**
 * The stop-arrival state is mutable and reused by the following Range Raptor iterations. An
 * arrival chain taken from the cursor must keep its values, so it can be stored as part of a
 * destination arrival and used after the iteration is complete.
 */
class StopsCursorSnapshotTest implements RaptorTestConstants {

  private static final int N_ROUNDS = 4;
  private static final int N_STOPS = 10;
  private static final int FIVE_MIN = 300;

  private static final TestTripSchedule TRIP_1 = TestTripSchedule.schedule()
    .pattern("L1", STOP_B, STOP_C)
    .times("0:10 0:20")
    .build();
  private static final TestTripSchedule TRIP_2 = TestTripSchedule.schedule()
    .pattern("L1", STOP_B, STOP_C)
    .times("0:30 0:40")
    .build();

  private static final TestTripSchedule TRIP_3 = TestTripSchedule.schedule()
    .pattern("L2", STOP_D, STOP_E)
    .times("0:30 0:40")
    .build();

  private final LifeCycleSubscriptions subscriptions = new LifeCycleSubscriptions();
  private final StdStopArrivals<TestTripSchedule> arrivals = new StdStopArrivals<>(
    N_ROUNDS,
    N_STOPS,
    subscriptions
  );
  private final StdStopArrivalsState<TestTripSchedule> state = new StdStopArrivalsState<>(
    arrivals,
    null
  );
  private final StopsCursor<TestTripSchedule> cursor = new StopsCursor<>(
    arrivals,
    new ForwardTransitCalculator<>(),
    _ -> 0
  );
  private LifeCycleEventPublisher lifeCycle;

  @BeforeEach
  void setUp() {
    lifeCycle = new LifeCycleEventPublisher(subscriptions);
    lifeCycle.prepareForNextRound(0);
  }

  @Test
  void transitArrivalKeepsItsValuesWhenTheStateIsOverwritten() {
    state.setAccessTime(FIVE_MIN, TestAccessEgress.walk(STOP_B, 60), true);
    lifeCycle.prepareForNextRound(1);
    state.setNewBestTransitTime(STOP_C, TRIP_1.arrival(1), 0, TRIP_1, true);

    var arrival = cursor.stop(1, STOP_C, true);

    // A later iteration finds another trip, and overwrites the state
    state.setNewBestTransitTime(STOP_C, TRIP_2.arrival(1), 0, TRIP_2, true);

    assertEquals(PathLegType.TRANSIT, arrival.arrivedBy());
    assertSame(TRIP_1, arrival.transitPath().trip());
    assertEquals(0, arrival.transitPath().boardStopPosition());
    assertEquals(TRIP_1.arrival(1), arrival.arrivalTime());
    assertEquals(PathLegType.ACCESS, arrival.previous().arrivedBy());
  }

  @Test
  void transferArrivalKeepsItsValuesWhenTheStateIsOverwritten() {
    int transferDuration = 120;
    int transferArrivalTime = TRIP_1.arrival(1) + transferDuration;
    state.setAccessTime(FIVE_MIN, TestAccessEgress.walk(STOP_B, 60), true);
    lifeCycle.prepareForNextRound(1);
    state.setNewBestTransitTime(STOP_C, TRIP_1.arrival(1), 0, TRIP_1, true);
    state.setNewBestTransferTime(
      STOP_C,
      transferArrivalTime,
      TestTransfer.transfer(STOP_D, transferDuration)
    );
    lifeCycle.prepareForNextRound(2);
    state.setNewBestTransitTime(STOP_E, TRIP_3.arrival(1), 0, TRIP_3, true);

    var transit2 = cursor.stop(2, STOP_E, true);
    var transfer = transit2.previous();
    var transit1 = transfer.previous();

    // A later iteration finds a later trip and a slower transfer, and overwrites the state
    lifeCycle.prepareForNextRound(1);
    state.setNewBestTransitTime(STOP_C, TRIP_2.arrival(1), 0, TRIP_2, true);
    state.setNewBestTransferTime(STOP_C, T00_40, TestTransfer.transfer(STOP_D, 300));

    assertSame(TRIP_3, transit2.transitPath().trip());
    assertEquals(PathLegType.TRANSFER, transfer.arrivedBy());
    assertEquals(transferArrivalTime, transfer.arrivalTime());
    assertEquals(transferDuration, transfer.transfer().durationInSeconds());
    assertEquals(PathLegType.TRANSIT, transit1.arrivedBy());
    assertSame(TRIP_1, transit1.transitPath().trip());
    assertEquals(TRIP_1.arrival(1), transit1.arrivalTime());
    assertEquals(PathLegType.ACCESS, transit1.previous().arrivedBy());
  }
}
