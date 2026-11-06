package org.opentripplanner.raptor.rangeraptor.destination;

import static org.opentripplanner.raptor.api.view.PathLegType.EGRESS;

import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.api.view.EgressPathView;
import org.opentripplanner.raptor.api.view.PathLegType;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

/**
 * A light-weight journey arrival at the destination, made of the last stop arrival and the egress. It holds the values
 * of the journey needed to compare it with other journeys: departure time, arrival time, number of transfers and cost.
 * The arrival chain is reachable through {@link #previous()}, so a path can be built from it when needed.
 * <p>
 * In addition to the criteria of the stop arrivals, the destination also needs a number of transfers and a time
 * criterion, because the arrivals come from different Range Raptor iterations.
 * See {@code org/opentripplanner/raptor/package.md}, section "Destination pareto-set".
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class DestinationArrival<T extends RaptorTripSchedule> implements ArrivalView<T> {

  private final ArrivalView<T> previous;
  private final RaptorAccessEgress egress;

  // The fields below are not final only to let a single probe be reused for qualification checks, see setProbeValues().
  private int iterationDepartureTime;
  private int departureTime;
  private int arrivalTime;
  private int numberOfTransfers;
  private int c1;
  private int c2;

  /**
   * @param iterationDepartureTime The departure time of the Range Raptor iteration.
   * @param departureTime The departure time of the journey in the search direction, with the
   *                      access time-shifted. See {@link TimeShiftedDepartureTime}. Use the
   *                      {@code iterationDepartureTime} if it is not needed.
   * @param arrivalTime The destination arrival time. If the egress has a time-penalty, the time
   *                    already includes it, since Raptor decorates the egress and adds the
   *                    penalty to the egress duration. It is the time used for comparison.
   */
  public DestinationArrival(
    RaptorAccessEgress egress,
    ArrivalView<T> previous,
    int iterationDepartureTime,
    int departureTime,
    int arrivalTime,
    int additionalC1,
    int c2
  ) {
    this.previous = previous;
    this.egress = egress;
    this.iterationDepartureTime = iterationDepartureTime;
    this.departureTime = departureTime;
    this.arrivalTime = arrivalTime;
    this.numberOfTransfers = previous.round() - 1 + egress.numberOfRides();
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
    int departureTime,
    int arrivalTime,
    int numberOfTransfers,
    int c1,
    int c2
  ) {
    if (previous != null || egress != null) {
      throw new IllegalStateException("Only a probe may be modified.");
    }
    this.iterationDepartureTime = iterationDepartureTime;
    this.departureTime = departureTime;
    this.arrivalTime = arrivalTime;
    this.numberOfTransfers = numberOfTransfers;
    this.c1 = c1;
    this.c2 = c2;
  }

  /**
   * The departure time of the Range Raptor iteration this arrival was found in, see the
   * "Destination pareto-set" section of {@code org/opentripplanner/raptor/package.md}.
   */
  public int iterationDepartureTime() {
    return iterationDepartureTime;
  }

  /**
   * The departure time of the journey in the search direction, with the access time-shifted, see
   * {@link TimeShiftedDepartureTime}. It is the journey arrival time in a reverse search.
   */
  public int departureTime() {
    return departureTime;
  }

  @Override
  public int stop() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int round() {
    return 1 + numberOfTransfers();
  }

  @Override
  public int numberOfTransfers() {
    return numberOfTransfers;
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
