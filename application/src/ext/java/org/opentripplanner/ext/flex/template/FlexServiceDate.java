package org.opentripplanner.ext.flex.template;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.opentripplanner.ext.flex.trip.FlexTrip;

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

  private final int requestedBookingTime;

  private final Set<FlexTrip<?, ?>> tripsRunning;

  public FlexServiceDate(
    LocalDate serviceDate,
    int secondsFromStartOfTime,
    int requestedBookingTime,
    Collection<FlexTrip<?, ?>> tripsRunning
  ) {
    this.serviceDate = serviceDate;
    this.secondsFromStartOfTime = secondsFromStartOfTime;
    this.requestedBookingTime = requestedBookingTime;
    this.tripsRunning = new HashSet<>(tripsRunning);
  }

  LocalDate serviceDate() {
    return serviceDate;
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
   * Return true if the given {@code flexTrip} is active and running on {@link #serviceDate}.
   */
  public boolean isTripRunning(FlexTrip<?, ?> flexTrip) {
    return tripsRunning.contains(flexTrip);
  }
}
