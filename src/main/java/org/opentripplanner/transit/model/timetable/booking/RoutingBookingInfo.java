package org.opentripplanner.transit.model.timetable.booking;

import java.util.Objects;
import java.util.Optional;
import org.opentripplanner.framework.lang.IntUtils;
import org.opentripplanner.framework.tostring.ToStringBuilder;

/**
 * This is the contract between booking info and the router. The router will enforce
 * this information if the request sets the earliest-booking-time request parameter.
 * <p>
 * Both {@code latestBookingTime} and {@code minimumBookingNotice} can be {@code null},
 * but at least one ot them must be none {@code null}.
 * <p>
 * This class is not used by Raptor directly, but used by the BookingTimeAccessEgress with
 * implement the RaptorAccessEgress interface.
 */
public final class RoutingBookingInfo {

  private static final int NOT_SET = -1_999_999;

  private final int latestBookingTime;
  private final int minimumBookingNotice;
  private final int legDurationInSeconds;
  private final int timeOffsetInSeconds;

  private RoutingBookingInfo(
    int latestBookingTime,
    int minimumBookingNotice,
    int legDurationInSeconds,
    int timeOffsetInSeconds
  ) {
    if (latestBookingTime == NOT_SET && minimumBookingNotice == NOT_SET) {
      throw new IllegalArgumentException(
        "Either latestBookingTime or minimumBookingNotice must be set."
      );
    }
    this.latestBookingTime = latestBookingTime;
    this.minimumBookingNotice = minimumBookingNotice;
    this.legDurationInSeconds =
      IntUtils.requireNotNegative(legDurationInSeconds, "legDurationInSeconds");
    this.timeOffsetInSeconds =
      IntUtils.requireNotNegative(timeOffsetInSeconds, "timeOffsetInSeconds");
  }

  public static RoutingBookingInfo.Builder of() {
    return new Builder();
  }

  /**
   * Check if requested board-time can be booked according to the booking info rules. See
   * {@link BookingInfo}.
   * <p>
   * If not the case, the RaptorConstants.TIME_NOT_SET is returned.
   */
  public boolean isThereEnoughTimeToBookForDeparture(int departureTime, int requestedBookingTime) {
    return isThereEnoughTimeToBook(departureTime + timeOffsetInSeconds, requestedBookingTime);
  }

  /**
   * Check if requested board-time can be booked according to the booking info rules. See
   * {@link BookingInfo}.
   * <p>
   * If not the case, the RaptorConstants.TIME_NOT_SET is returned.
   */
  public boolean isThereEnoughTimeToBookForArrival(int arrivalTime, int requestedBookingTime) {
    return isThereEnoughTimeToBook(
      arrivalTime - legDurationInSeconds + timeOffsetInSeconds,
      requestedBookingTime
    );
  }

  /**
   * Check if requested board-time can be booked according to the booking info rules. See
   * {@link BookingInfo}.
   * <p>
   * If not the case, the RaptorConstants.TIME_NOT_SET is returned.
   */
  private boolean isThereEnoughTimeToBook(int time, int requestedBookingTime) {
    // This can be optimized/simplified; it can be done before the search start since it
    // only depends on the latestBookingTime and requestedBookingTime, not the departure time.
    if (exceedsLatestBookingTime(requestedBookingTime)) {
      return false;
    }
    if (exceedsMinimumBookingNotice(time, requestedBookingTime)) {
      return false;
    }
    return true;
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
    return ToStringBuilder
      .of(RoutingBookingInfo.class)
      .addServiceTime("latestBookingTime", latestBookingTime, NOT_SET)
      .addDurationSec("minimumBookingNotice", minimumBookingNotice, NOT_SET)
      .toString();
  }

  private boolean exceedsLatestBookingTime(int requestedEarliestBookingTime) {
    return exist(latestBookingTime) && requestedEarliestBookingTime > latestBookingTime;
  }

  /**
   * Check if the given time is after (or eq to) the earliest time allowed according to the minimum
   * booking notice.
   */
  private boolean exceedsMinimumBookingNotice(int departureTime, int requestedBookingTime) {
    return (
      exist(minimumBookingNotice) && (departureTime - minimumBookingNotice < requestedBookingTime)
    );
  }

  private static boolean exist(int value) {
    return value != NOT_SET;
  }

  public static class Builder {

    private int latestBookingTime = NOT_SET;
    private int minimumBookingNotice = NOT_SET;
    private int legDurationInSeconds = 0;
    private int timeOffsetInSeconds = 0;

    /**
     * Convenience method to add booking info to builder.
     */
    public Builder withBookingInfo(BookingInfo bookingInfo) {
      if (bookingInfo.getLatestBookingTime() != null) {
        withLatestBookingTime(bookingInfo.getLatestBookingTime().relativeTimeSeconds());
      }
      if (bookingInfo.getMinimumBookingNotice() != null) {
        withMinimumBookingNotice((int) bookingInfo.getMinimumBookingNotice().toSeconds());
      }
      return this;
    }

    public Builder withLatestBookingTime(int latestBookingTime) {
      this.latestBookingTime = latestBookingTime;
      return this;
    }

    public Builder withMinimumBookingNotice(int minimumBookingNotice) {
      this.minimumBookingNotice = minimumBookingNotice;
      return this;
    }

    /**
     * The total time of the leg including any access and egress.
     * See {@link #withTimeOffsetInSeconds(int)}
     */
    public Builder withLegDurationInSeconds(int legDurationInSeconds) {
      this.legDurationInSeconds = legDurationInSeconds;
      return this;
    }

    /**
     * The offset is used to calculate when the "real" boardingTime is for the bookable service.
     * For example, when a Flex Service is part of access, there might be a walking section before
     * the flex service is boarded. In such a case the {@code timeOffsetInSeconds} should be set
     * to the time it takes to walk, before boarding the flex.
     */
    public Builder withTimeOffsetInSeconds(int timeOffsetInSeconds) {
      this.timeOffsetInSeconds = timeOffsetInSeconds;
      return this;
    }

    public Optional<RoutingBookingInfo> build() {
      if (latestBookingTime == NOT_SET && minimumBookingNotice == NOT_SET) {
        return Optional.empty();
      }

      return Optional.of(
        new RoutingBookingInfo(
          latestBookingTime,
          minimumBookingNotice,
          legDurationInSeconds,
          timeOffsetInSeconds
        )
      );
    }
  }
}
