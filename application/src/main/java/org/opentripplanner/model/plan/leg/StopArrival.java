package org.opentripplanner.model.plan.leg;

import javax.annotation.Nullable;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * This class is used to represent a stop arrival event mostly for intermediate visits to a stops
 * along a route.
 */
public final class StopArrival {

  public final Place place;

  @Nullable
  public final LegCallTime arrival;

  @Nullable
  public final LegCallTime departure;

  @Nullable
  public final Integer stopPosInPattern;

  @Nullable
  public final Integer gtfsStopSequence;

  @Nullable
  public final ViaLocationType viaLocationType;

  /**
   * @param arrival          The time the rider will arrive at the place.
   * @param departure        The time the rider will depart the place.
   * @param stopPosInPattern For transit trips, the stop index (numbered from zero from the start of
   *                         the trip).
   * @param gtfsStopSequence For transit trips, the sequence number of the stop. Per GTFS, these
   *                         numbers are increasing.
   * @param viaLocationType  Categorization for a via location, if the place is a via location in
   *                         the request.
   */
  public StopArrival(
    Place place,
    @Nullable LegCallTime arrival,
    @Nullable LegCallTime departure,
    @Nullable Integer stopPosInPattern,
    @Nullable Integer gtfsStopSequence,
    @Nullable ViaLocationType viaLocationType
  ) {
    this.place = place;
    this.arrival = arrival;
    this.departure = departure;
    this.stopPosInPattern = stopPosInPattern;
    this.gtfsStopSequence = gtfsStopSequence;
    this.viaLocationType = viaLocationType;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(StopArrival.class)
      .addObj("arrival", arrival)
      .addObj("departure", departure)
      .addObj("place", place)
      .addNum("stopPosInPattern", stopPosInPattern)
      .addNum("gtfsStopSequence", gtfsStopSequence)
      .addObj("viaLocationType", viaLocationType)
      .toString();
  }
}
