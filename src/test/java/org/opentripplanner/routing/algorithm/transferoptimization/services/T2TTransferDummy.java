package org.opentripplanner.routing.algorithm.transferoptimization.services;

import static org.opentripplanner.routing.algorithm.transferoptimization.model.TripStopTime.arrival;
import static org.opentripplanner.routing.algorithm.transferoptimization.model.TripStopTime.departure;
import static org.opentripplanner.transit.raptor._data.transit.TestTransfer.walk;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.opentripplanner.routing.algorithm.transferoptimization.model.StopTime;
import org.opentripplanner.routing.algorithm.transferoptimization.model.TripToTripTransfer;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;

/**
 * Mock the TripToTripTransfersService
 */
class T2TTransferDummy {
  private static final int D0s = 0;

  @SafeVarargs
  static StandardTransferGenerator<TestTripSchedule> dummyT2TTransferService(
      final TripToTripTransfer<TestTripSchedule> ... transfers
  ) {
    return new StandardTransferGenerator<>(null, null) {
      @Override
      public List<TripToTripTransfer<TestTripSchedule>> findTransfers(
          TestTripSchedule fromTrip, StopTime fromTripDeparture, TestTripSchedule toTrip
      ) {
        return Arrays.stream(transfers)
            .filter(tx -> tx.from().trip().equals(fromTrip) && tx.to().trip().equals(toTrip))
            .collect(Collectors.toList());
      }
    };
  }

  /** Transfer from trip & stop, walk, to stop & trip */
  static TripToTripTransfer<TestTripSchedule> tx(
          TestTripSchedule fromTrip, int fromStop,
          int walk,
          int toStop, TestTripSchedule toTrip
  ) {
    return new TripToTripTransfer<>(
        arrival(fromTrip, fromTrip.pattern().findStopPositionAfter(0, fromStop)),
        departure(toTrip, toTrip.pattern().findStopPositionAfter(0, toStop)),
        fromStop == toStop ? null : walk(toStop, walk)
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
