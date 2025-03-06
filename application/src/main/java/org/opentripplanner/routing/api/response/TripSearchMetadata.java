package org.opentripplanner.routing.api.response;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import javax.annotation.Nullable;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Meta-data about the trip search performed.
 */
public class TripSearchMetadata {

  /**
   * This is the time window used by the raptor search. The window is an optional parameter and OTP
   * might override it/dynamically assign a new value.
   */
  public Duration searchWindowUsed;

  /**
   * This is the suggested search time for the "previous page" or time window. Insert it together
   * with the {@link #searchWindowUsed} in the request to get a new set of trips preceding in the
   * time-window BEFORE the current search. No duplicate trips should be returned, unless a trip is
   * delayed and new realtime-data is available.
   *
   * @deprecated Use the PageInfo and request {@code nextCursor} and {@code previousCursor} instead.
   */
  @Deprecated
  public Instant prevDateTime;

  /**
   * This is the suggested search time for the "next page" or time window. Insert it together with
   * the {@link #searchWindowUsed} in the request to get a new set of trips following in the
   * time-window AFTER the current search. No duplicate trips should be returned, unless a trip is
   * delayed and new realtime-data is available.
   */
  @Deprecated
  public Instant nextDateTime;

  private TripSearchMetadata(
    Duration searchWindowUsed,
    Instant prevDateTime,
    Instant nextDateTime
  ) {
    this.searchWindowUsed = searchWindowUsed;
    this.prevDateTime = prevDateTime;
    this.nextDateTime = nextDateTime;
  }

  public static TripSearchMetadata createForArriveBy(
    Instant earliestDepartureTimeUsed,
    Duration searchWindowUsed,
    @Nullable Instant firstDepartureTime
  ) {
    Instant actualEdt = firstDepartureTime == null
      ? earliestDepartureTimeUsed
      // Round down to the minute before to avoid duplicates. This may cause missed itineraries.
      : firstDepartureTime.minusSeconds(60).truncatedTo(ChronoUnit.MINUTES);

    return new TripSearchMetadata(
      searchWindowUsed,
      actualEdt.minus(searchWindowUsed),
      earliestDepartureTimeUsed.plus(searchWindowUsed)
    );
  }

  public static TripSearchMetadata createForDepartAfter(
    Instant requestDepartureTime,
    Duration searchWindowUsed,
    Instant lastDepartureTime
  ) {
    Instant nextDateTime = lastDepartureTime == null
      ? requestDepartureTime.plus(searchWindowUsed)
      // There is no way to make this work properly. If we round down we get duplicates, if we
      // round up we might skip itineraries.
      : lastDepartureTime.plusSeconds(60).truncatedTo(ChronoUnit.MINUTES);

    return new TripSearchMetadata(
      searchWindowUsed,
      requestDepartureTime.minus(searchWindowUsed),
      nextDateTime
    );
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(TripSearchMetadata.class)
      .addDuration("searchWindowUsed", searchWindowUsed)
      .addObj("nextDateTime", nextDateTime)
      .addObj("prevDateTime", prevDateTime)
      .toString();
  }
}
