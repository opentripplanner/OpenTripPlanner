package org.opentripplanner.raptorlegacy._data.stoparrival;

import static org.opentripplanner.raptor.api.model.PathLegType.TRANSIT;

import org.opentripplanner.raptor.api.model.PathLegType;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.api.view.TransitPathView;
import org.opentripplanner.raptorlegacy._data.transit.TestTripSchedule;

/**
 *
 * @deprecated This was earlier part of Raptor and should not be used outside the Raptor
 *             module. Use the OTP model entities instead.
 */
@Deprecated
class Transit extends AbstractStopArrival implements TransitPathView<TestTripSchedule> {

  private final TestTripSchedule trip;

  Transit(
    int round,
    int stop,
    int arrivalTime,
    int c1,
    int c2,
    TestTripSchedule trip,
    ArrivalView<TestTripSchedule> previous
  ) {
    super(round, stop, arrivalTime, c1, c2, previous);
    this.trip = trip;
  }

  @Override
  public PathLegType arrivedBy() {
    return TRANSIT;
  }

  @Override
  public TransitPathView<TestTripSchedule> transitPath() {
    return this;
  }

  @Override
  public int boardStop() {
    return previous().stop();
  }

  @Override
  public TestTripSchedule trip() {
    return trip;
  }

  @Override
  public boolean arrivedOnBoard() {
    return true;
  }
}
