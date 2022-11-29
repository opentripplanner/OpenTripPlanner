package org.opentripplanner.routing.algorithm.transferoptimization.services;

import java.util.Arrays;
import java.util.List;
import org.opentripplanner.raptor._data.transit.TestTransfer;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.path.TransitPathLeg;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.routing.algorithm.transferoptimization.model.TripStopTime;
import org.opentripplanner.routing.algorithm.transferoptimization.model.TripToTripTransfer;

/**
 * Mock the TransferGenerator
 */
public class TransferGeneratorDummy {

  private static final int D0s = 0;

  /** Transfer from trip & stop, walk, to stop & trip */
  public static TripToTripTransfer<TestTripSchedule> tx(
    TestTripSchedule fromTrip,
    int fromStop,
    int walkDuration,
    int toStop,
    TestTripSchedule toTrip
  ) {
    return createTripToTripTransfer(fromTrip, fromStop, walkDuration, toStop, toTrip);
  }

  /** Transfer from trip via same stop to trip */
  public static TripToTripTransfer<TestTripSchedule> tx(
    TestTripSchedule fromTrip,
    int sameStop,
    TestTripSchedule toTrip
  ) {
    return createTripToTripTransfer(fromTrip, sameStop, D0s, sameStop, toTrip);
  }

  /** Transfer from transfer constraints - same stop */
  public static TripToTripTransfer<TestTripSchedule> tx(
    TestTransferBuilder<TestTripSchedule> builder
  ) {
    return createTripToTripTransfer(builder, D0s);
  }

  /** Transfer from transfer constraints - with walking */
  public static TripToTripTransfer<TestTripSchedule> tx(
    TestTransferBuilder<TestTripSchedule> builder,
    int walkDuration
  ) {
    return createTripToTripTransfer(builder, walkDuration);
  }

  @SafeVarargs
  static TransferGenerator<TestTripSchedule> dummyTransferGenerator(
    final List<TripToTripTransfer<TestTripSchedule>>... transfers
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

  /* private methods */

  private static TripToTripTransfer<TestTripSchedule> createTripToTripTransfer(
    TestTripSchedule fromTrip,
    int fromStop,
    int walkDuration,
    int toStop,
    TestTripSchedule toTrip
  ) {
    var pathTransfer = fromStop == toStop ? null : TestTransfer.transfer(toStop, walkDuration);

    return new TripToTripTransfer<>(
      departure(fromTrip, fromStop),
      arrival(toTrip, toStop),
      pathTransfer,
      null
    );
  }

  private static TripToTripTransfer<TestTripSchedule> createTripToTripTransfer(
    TestTransferBuilder<TestTripSchedule> builder,
    int walkDuration
  ) {
    int fromStop = builder.getFromStopIndex();
    int toStop = builder.getToStopIndex();
    var pathTransfer = fromStop == toStop ? null : TestTransfer.transfer(toStop, walkDuration);

    return new TripToTripTransfer<>(
      departure(builder.getFromTrip(), builder.getFromStopIndex()),
      arrival(builder.getToTrip(), builder.getToStopIndex()),
      pathTransfer,
      builder.build()
    );
  }

  private static <T extends RaptorTripSchedule> TripStopTime<T> departure(T trip, int stopIndex) {
    return TripStopTime.departure(trip, trip.pattern().findStopPositionAfter(0, stopIndex));
  }

  private static <T extends RaptorTripSchedule> TripStopTime<T> arrival(T trip, int stopIndex) {
    return TripStopTime.arrival(trip, trip.pattern().findStopPositionAfter(0, stopIndex));
  }
}
