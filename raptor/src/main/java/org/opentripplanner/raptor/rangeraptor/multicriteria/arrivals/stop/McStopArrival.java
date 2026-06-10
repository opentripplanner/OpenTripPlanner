package org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.stop;

import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

/**
 * Abstract super class for multi-criteria stop arrival.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public abstract sealed class McStopArrival<T extends RaptorTripSchedule>
  implements ArrivalView<T>
  permits AbstractStopArrivalC2, AccessStopArrival, TransitStopArrival, TransferStopArrival {

  private final McStopArrival<T> previous;
  private final int round;
  private final int stop;
  private final int arrivalTime;
  private final int travelDuration;
  private final int c1;

  /**
   * Transit or transfer.
   *
   * @param previous     the previous arrival visited for the current trip
   * @param round        the RangeRaptor round
   * @param stop         stop index for this arrival
   * @param arrivalTime  the arrival time for this stop index
   * @param c1           the accumulated criteria-one(cost) at this stop arrival
   */
  protected McStopArrival(McStopArrival<T> previous, int round, int stop, int arrivalTime, int c1) {
    this.previous = previous;
    this.round = round;
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
    int round
  ) {
    this.previous = null;
    this.round = round;
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
    return round;
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

  /**
   * Add the given amount of slack to the arrival-time. This is used to add extraordinary
   * wait-time to an arrival - for example, in via-search where a minimum-wait-time can be set.
   */
  public abstract McStopArrival<T> addSlackToArrivalTime(int slack);

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
}
