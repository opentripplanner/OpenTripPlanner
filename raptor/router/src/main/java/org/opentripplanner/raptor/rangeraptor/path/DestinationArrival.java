package org.opentripplanner.raptor.rangeraptor.path;

import static org.opentripplanner.raptor.api.view.PathLegType.EGRESS;

import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.api.view.EgressPathView;
import org.opentripplanner.raptor.api.view.PathLegType;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

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
 * </ul>
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class DestinationArrival<T extends RaptorTripSchedule> implements ArrivalView<T> {

  private final ArrivalView<T> previous;
  private final RaptorAccessEgress egress;
  private int iterationDepartureTime;
  private int arrivalTime;

  private int numberOfTransfers;
  private int c1;
  private int c2;

  /**
   * @param arrivalTime The destination arrival time. If the egress has a time-penalty, the time
   *                    already includes it, since Raptor decorates the egress and adds the
   *                    penalty to the egress duration. It is the time used for comparison.
   */
  public DestinationArrival(
    RaptorAccessEgress egress,
    ArrivalView<T> previous,
    int iterationDepartureTime,
    int arrivalTime,
    int additionalC1,
    int c2
  ) {
    this.previous = previous;
    this.egress = egress;
    this.iterationDepartureTime = iterationDepartureTime;
    this.arrivalTime = arrivalTime;
    // transit-only transfers; egress rides are added dynamically via round()
    this.numberOfTransfers = previous.round() - 1;
    this.c1 = previous.c1() + additionalC1;
    this.c2 = c2;
  }

  /**
   * Create a mutable probe for pareto qualification checks only. The probe has no arrival chain
   * ({@link #previous()} returns {@code null}) and must never be used for path building or be
   * added to a pareto set. Create ONE probe and reuse it with {@link #setProbeValues}; this
   * avoids allocating an object for every qualification check.
   * <p>
   * A probe is mutable and NOT thread-safe. Do not share a probe between threads; create one
   * probe per thread if qualification checks are run in parallel.
   */
  static <T extends RaptorTripSchedule> DestinationArrival<T> createProbe() {
    return new DestinationArrival<>();
  }

  /** Used by {@link #createProbe()} only. */
  private DestinationArrival() {
    this.previous = null;
    this.egress = null;
  }

  /**
   * Set the criteria values of a probe created with {@link #createProbe()}. Real arrivals are
   * immutable in practice, so this is only allowed on a probe. Not thread-safe, see
   * {@link #createProbe()}.
   */
  void setProbeValues(
    int iterationDepartureTime,
    int arrivalTime,
    int numberOfTransfers,
    int c1,
    int c2
  ) {
    if (previous != null || egress != null) {
      throw new IllegalStateException("Only a probe may be modified.");
    }
    this.iterationDepartureTime = iterationDepartureTime;
    this.arrivalTime = arrivalTime;
    this.numberOfTransfers = numberOfTransfers;
    this.c1 = c1;
    this.c2 = c2;
  }

  public int iterationDepartureTime() {
    return iterationDepartureTime;
  }

  @Override
  public int stop() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int round() {
    return 1 + numberOfTransfers + (egress != null ? egress.numberOfRides() : 0);
  }

  @Override
  public int numberOfTransfers() {
    return round() - 1;
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
