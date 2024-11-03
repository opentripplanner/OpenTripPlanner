package org.opentripplanner.raptorlegacy._data.stoparrival;

import org.opentripplanner.raptor.api.model.PathLegType;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.api.view.EgressPathView;
import org.opentripplanner.raptorlegacy._data.transit.TestTripSchedule;
import org.opentripplanner.utils.time.TimeUtils;

/**
 * @deprecated This was earlier part of Raptor and should not be used outside the Raptor
 *             module. Use the OTP model entities instead.
 */
@Deprecated
public class Egress extends AbstractStopArrival {

  private final RaptorAccessEgress egressPath;

  public Egress(
    int arrivalTime,
    RaptorAccessEgress egressPath,
    int c2,
    ArrivalView<TestTripSchedule> previous
  ) {
    super(previous.round(), previous.stop(), arrivalTime, egressPath.c1(), c2, previous);
    this.egressPath = egressPath;
  }

  @Override
  public EgressPathView egressPath() {
    return () -> egressPath;
  }

  @Override
  public String toString() {
    return String.format(
      "Egress { round: %d, stop: %d, arrival-time: %s $%d }",
      round(),
      stop(),
      TimeUtils.timeToStrCompact(arrivalTime()),
      c1()
    );
  }

  @Override
  public PathLegType arrivedBy() {
    return PathLegType.EGRESS;
  }

  @Override
  public boolean arrivedOnBoard() {
    return egressPath.stopReachedOnBoard();
  }
}
