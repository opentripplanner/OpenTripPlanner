package org.opentripplanner.routing.algorithm.transferoptimization.services;

import static org.opentripplanner.routing.algorithm.transferoptimization.model.TripStopTime.arrival;
import static org.opentripplanner.routing.algorithm.transferoptimization.model.TripStopTime.departure;
import static org.opentripplanner.transit.raptor._data.transit.TestTransfer.walk;

import java.util.Arrays;
import java.util.List;
import org.opentripplanner.routing.algorithm.transferoptimization.model.TripToTripTransfer;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.path.TransitPathLeg;

/**
 * Mock the TransferGenerator
 */
class TransferGeneratorDummy {
  private static final int D0s = 0;

  @SafeVarargs
  static TransferGenerator<TestTripSchedule> dummyTransferGenerator(
      final List<TripToTripTransfer<TestTripSchedule>> ... transfers
  ) {
    return new TransferGenerator<>(null, null, null) {
      @Override
      public List<List<TripToTripTransfer<TestTripSchedule>>> findAllPossibleTransfers(
              List<TransitPathLeg<TestTripSchedule>> transitLegs
      ) {
        return Arrays.asList(transfers);
      }
    };
  }

  /** Transfer from trip & stop, walk, to stop & trip */
  static TripToTripTransfer<TestTripSchedule> tx(
          TestTripSchedule fromTrip, int fromStop,
          int walkDuration,
          int toStop, TestTripSchedule toTrip
  ) {
    return new TripToTripTransfer<>(
        arrival(fromTrip, fromTrip.pattern().findStopPositionAfter(0, fromStop)),
        departure(toTrip, toTrip.pattern().findStopPositionAfter(0, toStop)),
        fromStop == toStop ? null : walk(toStop, walkDuration)
    );
  }

  /** Transfer from trip via stop to trip */
  static TripToTripTransfer<TestTripSchedule> tx(
      TestTripSchedule fromTrip,
      int sameStop,
      TestTripSchedule toTrip
  ) {
    return tx(fromTrip, sameStop, D0s, sameStop, toTrip);
  }
}
