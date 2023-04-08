package org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals;

import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.model.RelaxFunction;
import org.opentripplanner.raptor.api.view.ArrivalView;

/**
 * Abstract super class for multi-criteria stop arrival.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public abstract class McStopArrival<T extends RaptorTripSchedule> implements ArrivalView<T> {

  private final McStopArrival<T> previous;
  /**
   * We want transits to dominate transfers, so we increment the round not only between RangeRaptor
   * rounds, but for transits and transfers also. The access path is paretoRound 0, the first
   * transit path is 1. The following transfer path, if it exists, is paretoRound 2, and the next
   * transit is 3, and so on.
   * <p/>
   * The relationship between Range Raptor round and paretoRound can be described by this formula:
   * <pre>
   *     Range Raptor round =  (paretoRound + 1) / 2
   * </pre>
   */
  private final int paretoRound;
  private final int stop;
  private final int arrivalTime;
  private final int travelDuration;
  private final int c1;

  /**
   * Transit or transfer.
   *
   * @param previous             the previous arrival visited for the current trip
   * @param paretoRoundIncrement the increment to add to the paretoRound
   * @param stop                 stop index for this arrival
   * @param arrivalTime          the arrival time for this stop index
   * @param c1                   the accumulated criteria-one(cost) at this stop arrival
   */
  protected McStopArrival(
    McStopArrival<T> previous,
    int paretoRoundIncrement,
    int stop,
    int arrivalTime,
    int c1
  ) {
    this.previous = previous;
    this.paretoRound = previous.paretoRound + paretoRoundIncrement;
    this.stop = stop;
    this.arrivalTime = arrivalTime;
    this.travelDuration = previous.travelDuration() + (arrivalTime - previous.arrivalTime());
    this.c1 = c1;
  }

  /**
   * Initial state - first stop visited during the RAPTOR algorithm.
   */
  protected McStopArrival(
    int stop,
    int departureTime,
    int travelDuration,
    int initialC1,
    int paretoRound
  ) {
    this.previous = null;
    this.paretoRound = paretoRound;
    this.stop = stop;
    this.arrivalTime = departureTime + travelDuration;
    this.travelDuration = travelDuration;
    this.c1 = initialC1;
  }

  @Override
  public final int stop() {
    return stop;
  }

  @Override
  public final int round() {
    return (paretoRound + 1) / 2;
  }

  protected final int paretoRound() {
    return paretoRound;
  }

  @Override
  public final int arrivalTime() {
    return arrivalTime;
  }

  public final int c1() {
    return c1;
  }

  @Override
  public final McStopArrival<T> previous() {
    return previous;
  }

  public final int travelDuration() {
    return travelDuration;
  }

  public McStopArrival<T> timeShiftNewArrivalTime(int newArrivalTime) {
    throw new UnsupportedOperationException("No accessEgress for transfer stop arrival");
  }

  @Override
  public final int hashCode() {
    throw new IllegalStateException("Avoid using hashCode() and equals() for this class.");
  }

  @Override
  public final boolean equals(Object o) {
    throw new IllegalStateException("Avoid using hashCode() and equals() for this class.");
  }

  @Override
  public String toString() {
    return asString();
  }

  /**
   * @return previous state or throw a NPE if no previousArrival exist.
   */
  protected final int previousStop() {
    return previous.stop;
  }

  /**
   * Compare arrivalTime, paretoRound and c1.
   */
  protected static boolean compareBase(McStopArrival<?> l, McStopArrival<?> r) {
    // This is important with respect to performance. Using the short-circuit logical OR(||) is
    // faster than bitwise inclusive OR(|) (even between boolean expressions)
    return (
      l.arrivalTime() < r.arrivalTime() || l.paretoRound() < r.paretoRound() || l.c1() < r.c1()
    );
  }

  /**
   * Compare arrivalTime, paretoRound and c1, relaxing arrivalTime and c1.
   */
  protected static boolean relaxedCompareBase(
    final RelaxFunction relaxC1,
    McStopArrival<?> l,
    McStopArrival<?> r
  ) {
    return (
      l.arrivalTime() < r.arrivalTime() ||
      l.paretoRound() < r.paretoRound() ||
      l.c1() < relaxC1.relax(r.c1())
    );
  }

  /**
   * Compare arrivedOnBoard. On-board arrival dominate arrive by transfer(foot) since
   * you can continue on foot; hence has more options.
   */
  protected static boolean compareArrivedOnBoard(McStopArrival<?> l, McStopArrival<?> r) {
    return l.arrivedOnBoard() && !r.arrivedOnBoard();
  }
}
