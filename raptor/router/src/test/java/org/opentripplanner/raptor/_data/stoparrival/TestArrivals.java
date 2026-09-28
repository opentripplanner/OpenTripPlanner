package org.opentripplanner.raptor._data.stoparrival;

import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestTransfer;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.spi.RaptorConstants;
import org.opentripplanner.raptor.spi.RaptorTransfer;

public class TestArrivals {

  public static ArrivalView<TestTripSchedule> access(
    int stop,
    int arrivalTime,
    RaptorAccessEgress path,
    int c2
  ) {
    return new Access(stop, arrivalTime, path, c2);
  }

  public static ArrivalView<TestTripSchedule> access(
    int stop,
    int arrivalTime,
    RaptorAccessEgress path
  ) {
    return access(stop, arrivalTime, path, RaptorConstants.NOT_SET);
  }

  public static ArrivalView<TestTripSchedule> access(
    int stop,
    int departureTime,
    int arrivalTime,
    int c1,
    int c2
  ) {
    return access(
      stop,
      arrivalTime,
      TestAccessEgress.walk(stop, Math.abs(arrivalTime - departureTime), c1),
      c2
    );
  }

  public static ArrivalView<TestTripSchedule> transfer(
    int round,
    int arrivalTime,
    RaptorTransfer transfer,
    ArrivalView<TestTripSchedule> previous
  ) {
    return new Transfer(round, arrivalTime, transfer, previous);
  }

  public static ArrivalView<TestTripSchedule> transfer(
    int round,
    int stop,
    int departureTime,
    int arrivalTime,
    int extraCost,
    ArrivalView<TestTripSchedule> previous
  ) {
    return transfer(
      round,
      arrivalTime,
      TestTransfer.transfer(stop, Math.abs(arrivalTime - departureTime), extraCost),
      previous
    );
  }

  /// This finds the first boarding after the previous arrival. This might not be correct.
  /// A none zero board-slack or constrained transfer could cause problems, if needed, add andother
  /// factory method.
  public static ArrivalView<TestTripSchedule> bus(
    int round,
    int stop,
    int arrivalTime,
    int c1,
    int c2,
    TestTripSchedule trip,
    ArrivalView<TestTripSchedule> previous
  ) {
    int boardStopPosition = trip.findDepartureStopPosition(previous.arrivalTime(), previous.stop());
    return new Transit(round, stop, arrivalTime, c1, c2, boardStopPosition, trip, previous);
  }

  /// For reverse search: finds the alight stop position using the previous stop's arrival time.
  /// The previous stop is the alight stop in the real (forward) direction.
  public static ArrivalView<TestTripSchedule> busReverseSearch(
    int round,
    int stop,
    int arrivalTime,
    int c1,
    int c2,
    TestTripSchedule trip,
    ArrivalView<TestTripSchedule> previous
  ) {
    int alightStopPosition = trip.findArrivalStopPosition(previous.arrivalTime(), previous.stop());
    return new Transit(round, stop, arrivalTime, c1, c2, alightStopPosition, trip, previous);
  }

  public static ArrivalView<TestTripSchedule> egress(
    int departureTime,
    int arrivalTime,
    int c1,
    int c2,
    ArrivalView<TestTripSchedule> previous
  ) {
    return new Egress(
      departureTime,
      TestAccessEgress.walk(previous.stop(), Math.abs(arrivalTime - departureTime), c1),
      c2,
      previous
    );
  }
}
