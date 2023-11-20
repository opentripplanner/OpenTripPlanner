package org.opentripplanner.raptor._data.stoparrival;

import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestTransfer;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.raptor.api.model.RaptorTransfer;
import org.opentripplanner.raptor.api.view.ArrivalView;

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

  public static ArrivalView<TestTripSchedule> access(
    int stop,
    int departureTime,
    int arrivalTime,
    int c1
  ) {
    return access(stop, departureTime, arrivalTime, c1, RaptorConstants.NOT_SET);
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

  public static ArrivalView<TestTripSchedule> bus(
    int round,
    int stop,
    int arrivalTime,
    int c1,
    int c2,
    TestTripSchedule trip,
    ArrivalView<TestTripSchedule> previous
  ) {
    return new Transit(round, stop, arrivalTime, c1, c2, trip, previous);
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
