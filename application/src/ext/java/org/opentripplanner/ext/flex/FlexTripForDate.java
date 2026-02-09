package org.opentripplanner.ext.flex;

import java.time.LocalDate;
import org.opentripplanner.ext.flex.trip.FlexTrip;

public record FlexTripForDate(
  /** The service date of the trip pattern. */
  LocalDate serviceDate,
  /**The running date on which the first trip departs. Not necessarily the same as the service date. */
  LocalDate startOfRunningPeriod,
  /** The running date on which the last trip arrives.  */
  LocalDate endOfRunningPeriod,
  /** The FlexTrip that runs on the service date. */
  FlexTrip<?, ?> flexTrip
) {}
