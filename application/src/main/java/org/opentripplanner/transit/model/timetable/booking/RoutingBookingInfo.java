package org.opentripplanner.transit.model.timetable.booking;

import java.time.Duration;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * This is the contract between booking info and the router. The router will enforce
 * this information if the request sets the earliest-booking-time request parameter.
 * <p>
 * Either {@code latestBookingTime} and {@code minimumBookingNotice} must be set to
 * an actual value, both can not be set to {@NOT_SET} simultaneously.
 * <p>
 * This class is not used by Raptor directly, but used by the BookingTimeAccessEgress which
 * implements the RaptorAccessEgress interface.
 */
public final class RoutingBookingInfo {

  public static final int NOT_SET = -1_999_999;
  private static final RoutingBookingInfo UNRESTRICTED = new RoutingBookingInfo();

  private final int requestedBookingTime;
  private final int latestBookingTime;
  private final int minimumBookingNotice;

  /** Unrestricted booking info. */
  private RoutingBookingInfo() {
    this.requestedBookingTime = NOT_SET;
    this.latestBookingTime = NOT_SET;
    this.minimumBookingNotice = NOT_SET;
  }

  private RoutingBookingInfo(
    int requestedBookingTime,
    int latestBookingTime,
    int minimumBookingNotice
  ) {
    if (notSet(requestedBookingTime)) {
      throw new IllegalArgumentException("The requestedBookingTime must be set.");
    }
    if (notSet(latestBookingTime) && notSet(minimumBookingNotice)) {
      throw new IllegalArgumentException(
        "At least latestBookingTime or minimumBookingNotice must be set."
      );
    }
    this.requestedBookingTime = requestedBookingTime;
    this.latestBookingTime = latestBookingTime;
    this.minimumBookingNotice = minimumBookingNotice;
  }

  public static RoutingBookingInfo.Builder of(int requestedBookingTime) {
    return new Builder(requestedBookingTime);
  }

  public static RoutingBookingInfo of(int requestedBookingTime, @Nullable BookingInfo bookingInfo) {
    return of(requestedBookingTime).withBookingInfo(bookingInfo).build();
  }

  /**
   * Return an instance without any booking restrictions.
   */
  public static RoutingBookingInfo unrestricted() {
    return UNRESTRICTED;
  }

  /**
   * Time-shift departureTime if the minimum-booking-notice requires it. If required, the
   * new time-shifted departureTime is returned, if not the given {@code departureTime} is
   * returned as is. For example, if a service is available between 12:00 and 15:00 and the
   * minimum booking notice is 30 minutes, the first available trip at 13:00
   * ({@code requestedBookingTime}) is 13:30.
   */
  public int earliestDepartureTime(int departureTime) {
    return notSet(minimumBookingNotice)
      ? departureTime
      : Math.max(minBookingNoticeLimit(), departureTime);
  }

  /**
   * Check if the given time is after (or eq to) the earliest time allowed according to the minimum
   * booking notice.
   */
  public boolean exceedsMinimumBookingNotice(int departureTime) {
    return exist(minimumBookingNotice) && departureTime < minBookingNoticeLimit();
  }

  public boolean exceedsLatestBookingTime() {
    return exist(latestBookingTime) && requestedBookingTime > latestBookingTime;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    var other = (RoutingBookingInfo) o;
    return (
      Objects.equals(latestBookingTime, other.latestBookingTime) &&
      Objects.equals(minimumBookingNotice, other.minimumBookingNotice)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(latestBookingTime, minimumBookingNotice);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(RoutingBookingInfo.class)
      .addServiceTime("latestBookingTime", latestBookingTime, NOT_SET)
      .addDurationSec("minimumBookingNotice", minimumBookingNotice, NOT_SET)
      .toString();
  }

  private int minBookingNoticeLimit() {
    return requestedBookingTime + minimumBookingNotice;
  }

  private static boolean exist(int value) {
    return value != NOT_SET;
  }

  private static boolean notSet(int value) {
    return value == NOT_SET;
  }

  public static class Builder {

    private final int requestedBookingTime;
    private int latestBookingTime;
    private int minimumBookingNotice;

    public Builder(int requestedBookingTime) {
      this.requestedBookingTime = requestedBookingTime;
      setUnrestricted();
    }

    /**
     * Convenience method to add booking info to builder.
     */
    Builder withBookingInfo(@Nullable BookingInfo bookingInfo) {
      // Clear booking
      if (bookingInfo == null) {
        setUnrestricted();
        return this;
      }
      withLatestBookingTime(bookingInfo.getLatestBookingTime());
      withMinimumBookingNotice(bookingInfo.getMinimumBookingNotice().orElse(null));
      return this;
    }

    public Builder withLatestBookingTime(@Nullable BookingTime latestBookingTime) {
      this.latestBookingTime = latestBookingTime == null
        ? NOT_SET
        : latestBookingTime.relativeTimeSeconds();
      return this;
    }

    public Builder withMinimumBookingNotice(@Nullable Duration minimumBookingNotice) {
      this.minimumBookingNotice = minimumBookingNotice == null
        ? NOT_SET
        : (int) minimumBookingNotice.toSeconds();
      return this;
    }

    public RoutingBookingInfo build() {
      if (notSet(requestedBookingTime)) {
        return unrestricted();
      }
      if (notSet(latestBookingTime) && notSet(minimumBookingNotice)) {
        return RoutingBookingInfo.unrestricted();
      }
      return new RoutingBookingInfo(requestedBookingTime, latestBookingTime, minimumBookingNotice);
    }

    private void setUnrestricted() {
      latestBookingTime = NOT_SET;
      minimumBookingNotice = NOT_SET;
    }
  }
}
