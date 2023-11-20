package org.opentripplanner.raptor.rangeraptor.path;

import static org.opentripplanner.raptor.api.model.PathLegType.EGRESS;

import org.opentripplanner.raptor.api.model.PathLegType;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.api.view.EgressPathView;

/**
 * The purpose of this class is hold information about a destination arrival and compute the values
 * for arrival time and cost.
 * <p/>
 * Compared with the ParetoSet of each stop we need two extra criteria:
 * <ul>
 * <li>Number of transfers. The McRangeRaptor works in rounds, so
 * there is no need to include rounds in the intermediate stop pareto sets.
 * But to avoid that a later iteration delete an earlier result with less
 * transfers, transfers need to be added as a criterion to the final destination.
 *
 * <li>Travel time duration - Range Raptor works in iteration. So when a
 * later iteration makes it into the destination set - it should not erase
 * an earlier result unless it is faster. There is no check on total travel
 * duration for each stop, because it does not need to.
 *
 * </ul>
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class DestinationArrival<T extends RaptorTripSchedule> implements ArrivalView<T> {

  private final ArrivalView<T> previous;
  private final RaptorAccessEgress egress;
  private final int arrivalTime;
  private final int numberOfTransfers;
  private final int c1;
  private final int c2;

  public DestinationArrival(
    RaptorAccessEgress egress,
    ArrivalView<T> previous,
    int arrivalTime,
    int additionalC1,
    int c2
  ) {
    this.previous = previous;
    this.egress = egress;
    this.arrivalTime = arrivalTime;
    this.numberOfTransfers = previous.round() - 1;
    this.c1 = previous.c1() + additionalC1;
    this.c2 = c2;
  }

  @Override
  public int stop() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int round() {
    return 1 + numberOfTransfers + egress.numberOfRides();
  }

  @Override
  public int arrivalTime() {
    return arrivalTime;
  }

  @Override
  public int c1() {
    return c1;
  }

  @Override
  public int c2() {
    return c2;
  }

  @Override
  public ArrivalView<T> previous() {
    return previous;
  }

  @Override
  public PathLegType arrivedBy() {
    return EGRESS;
  }

  @Override
  public boolean arrivedAtDestination() {
    return true;
  }

  @Override
  public EgressPathView egressPath() {
    return () -> egress;
  }

  @Override
  public boolean arrivedOnBoard() {
    return false;
  }

  @Override
  public String toString() {
    return asString();
  }
}
