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

  private final Instant pageDepartureTimeStart;
  private final Instant pageDepartureTimeEnd;
  private final Duration raptorSearchWindowUsed;
  private final Instant prevDateTime;
  private final Instant nextDateTime;

  private TripSearchMetadata(
    Instant pageDepartureTimeStart,
    Instant pageDepartureTimeEnd,
    Duration raptorSearchWindowUsed,
    Instant prevDateTime,
    Instant nextDateTime
  ) {
    this.pageDepartureTimeStart = pageDepartureTimeStart;
    this.pageDepartureTimeEnd = pageDepartureTimeEnd;
    this.raptorSearchWindowUsed = raptorSearchWindowUsed;
    this.prevDateTime = prevDateTime;
    this.nextDateTime = nextDateTime;
  }

  public static TripSearchMetadata createForArriveBy(
    Instant pageDepartureTimeStart,
    Instant pageDepartureTimeEnd,
    Instant earliestDepartureTimeUsed,
    Duration raptorSearchWindowUsed,
    @Nullable Instant firstDepartureTime
  ) {
    // Round down to the minute before to avoid duplicates. This may cause missed itineraries.
    Instant actualEdt = firstDepartureTime == null
      ? earliestDepartureTimeUsed
      : firstDepartureTime.minusSeconds(60).truncatedTo(ChronoUnit.MINUTES);

    return new TripSearchMetadata(
      pageDepartureTimeStart,
      pageDepartureTimeEnd,
      raptorSearchWindowUsed,
      actualEdt.minus(raptorSearchWindowUsed),
      earliestDepartureTimeUsed.plus(raptorSearchWindowUsed)
    );
  }

  public static TripSearchMetadata createForDepartAfter(
    Instant pageDepartureTimeStart,
    Instant pageDepartureTimeEnd,
    Instant requestDepartureTime,
    Duration raptorSearchWindowUsed,
    Instant lastDepartureTime
  ) {
    // There is no way to make this work properly with lastDepartureTime. If we round down we get
    // duplicates, if we round up we might skip itineraries.
    Instant nextDateTime = lastDepartureTime == null
      ? requestDepartureTime.plus(raptorSearchWindowUsed)
      : lastDepartureTime.plusSeconds(60).truncatedTo(ChronoUnit.MINUTES);

    return new TripSearchMetadata(
      pageDepartureTimeStart,
      pageDepartureTimeEnd,
      raptorSearchWindowUsed,
      requestDepartureTime.minus(raptorSearchWindowUsed),
      nextDateTime
    );
  }

  public Instant pageDepartureTimeStart() {
    return pageDepartureTimeStart;
  }

  public Instant pageDepartureTimeEnd() {
    return pageDepartureTimeEnd;
  }

  /**
   * This is the time window used by the raptor search. The window is an optional parameter and OTP
   * might override it/dynamically assign a new value.
   */
  public Duration raptorSearchWindowUsed() {
    return raptorSearchWindowUsed;
  }

  /**
   * @deprecated Use the PageInfo and request {@code nextCursor} and {@code previousCursor} instead.
   */
  @Deprecated
  public Instant prevDateTime() {
    return prevDateTime;
  }

  /**
   * @deprecated Use the PageInfo and request {@code nextCursor} and {@code previousCursor} instead.
   */
  @Deprecated
  public Instant nextDateTime() {
    return nextDateTime;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(TripSearchMetadata.class)
      .addObj("pageDepartureTimeStart", pageDepartureTimeStart)
      .addObj("pageDepartureTimeEnd", pageDepartureTimeEnd)
      .addDuration("searchWindowUsed", raptorSearchWindowUsed)
      .addObj("nextDateTime", nextDateTime)
      .addObj("prevDateTime", prevDateTime)
      .toString();
  }
}
