package org.opentripplanner.model.plan;

import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * This class is used to represent a stop arrival event mostly for intermediate visits to a stops
 * along a route.
 */
public final class StopArrival {

  public final Place place;
  public final LegCallTime arrival;
  public final LegCallTime departure;
  public final Integer stopPosInPattern;
  public final Integer gtfsStopSequence;
  public final boolean canceled;

  /**
   * @param arrival          The time the rider will arrive at the place.
   * @param departure        The time the rider will depart the place.
   * @param stopPosInPattern For transit trips, the stop index (numbered from zero from the start of
   *                         the trip).
   * @param gtfsStopSequence For transit trips, the sequence number of the stop. Per GTFS, these
   *                         numbers are increasing.
   */
  public StopArrival(
    Place place,
    LegCallTime arrival,
    LegCallTime departure,
    Integer stopPosInPattern,
    Integer gtfsStopSequence,
    boolean canceled
  ) {
    this.place = place;
    this.arrival = arrival;
    this.departure = departure;
    this.stopPosInPattern = stopPosInPattern;
    this.gtfsStopSequence = gtfsStopSequence;
    this.canceled = canceled;
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(StopArrival.class)
      .addObj("arrival", arrival)
      .addObj("departure", departure)
      .addObj("place", place)
      .toString();
  }
}
