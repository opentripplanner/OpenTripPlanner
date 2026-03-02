package org.opentripplanner.ext.flex.template;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.transit.model.timetable.booking.RoutingBookingInfo;
import org.opentripplanner.utils.time.ServiceDateUtils;

/**
 * This class contains information used in a flex router, and depends on the date the search was
 * made on.
 */
public class FlexServiceDate {

  /** The local date */
  private final LocalDate serviceDate;

  /**
   * How many seconds does this date's "midnight" (12 hours before noon) differ from the "midnight"
   * of the date for the search.
   */
  private final int secondsFromStartOfTime;

  /**
   * Get the requested booking time as seconds since the start of service for this date.
   * Calculated relative to this specific service date's start-of-service.
   */
  private final int requestedBookingTime;

  private final Set<FlexTrip<?, ?>> tripsRunning;

  private FlexServiceDate(
    LocalDate serviceDate,
    int secondsFromStartOfTime,
    int requestedBookingTime,
    Collection<FlexTrip<?, ?>> tripsRunning
  ) {
    this.serviceDate = serviceDate;
    this.secondsFromStartOfTime = secondsFromStartOfTime;
    this.requestedBookingTime = requestedBookingTime;
    this.tripsRunning = tripsRunning != null ? new HashSet<>(tripsRunning) : Set.of();
  }

  public static FlexServiceDate of(
    LocalDate serviceDate,
    int secondsFromStartOfTime,
    @Nullable Instant requestedBookingTimeInstant,
    ZoneId timeZone,
    Collection<FlexTrip<?, ?>> tripsRunning
  ) {
    int requestedBookingTime;
    if (requestedBookingTimeInstant == null) {
      requestedBookingTime = RoutingBookingInfo.NOT_SET;
    } else {
      ZonedDateTime startOfService = ServiceDateUtils.asStartOfService(serviceDate, timeZone);
      requestedBookingTime = ServiceDateUtils.secondsSinceStartOfTime(
        startOfService,
        requestedBookingTimeInstant
      );
    }
    return new FlexServiceDate(
      serviceDate,
      secondsFromStartOfTime,
      requestedBookingTime,
      tripsRunning
    );
  }

  int secondsFromStartOfTime() {
    return secondsFromStartOfTime;
  }

  /**
   * Get the requested booking time as seconds since the start of service for this date.
   */
  int requestedBookingTime() {
    return requestedBookingTime;
  }

  /**
   * Return the service date this {@code FlexServiceDate} corresponds to.
   */
  public LocalDate serviceDate() {
    return serviceDate;
  }

  /**
   * Return all trips running on the service date this {@code FlexServiceDate} corresponds to.
   */
  public Set<FlexTrip<?, ?>> tripsRunning() {
    return tripsRunning;
  }

  /**
   * Return true if the given {@code flexTrip} is active and running on {@link #serviceDate}.
   */
  public boolean isTripRunning(FlexTrip<?, ?> flexTrip) {
    return tripsRunning.contains(flexTrip);
  }
}
