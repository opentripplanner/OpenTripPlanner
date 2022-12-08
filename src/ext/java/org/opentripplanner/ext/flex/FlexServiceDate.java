package org.opentripplanner.ext.flex;

import gnu.trove.set.TIntSet;
import java.time.LocalDate;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.transit.service.TransitService;

/**
 * This class contains information used in a flex router, and depends on the date the search was
 * made on.
 */
public class FlexServiceDate {

  /** The local date */
  public final LocalDate serviceDate;

  /**
   * How many seconds does this date's "midnight" (12 hours before noon) differ from the "midnight"
   * of the date for the search.
   */
  public final int secondsFromStartOfTime;

  /** Which services are running on the date. */
  public final TIntSet servicesRunning;

  public FlexServiceDate(
    LocalDate serviceDate,
    int secondsFromStartOfTime,
    TIntSet servicesRunning
  ) {
    this.serviceDate = serviceDate;
    this.secondsFromStartOfTime = secondsFromStartOfTime;
    this.servicesRunning = servicesRunning;
  }

  boolean isFlexTripRunning(FlexTrip flexTrip, TransitService transitService) {
    return (
      servicesRunning != null &&
      servicesRunning.contains(
        transitService.getServiceCodeForId(flexTrip.getTrip().getServiceId())
      )
    );
  }
}
