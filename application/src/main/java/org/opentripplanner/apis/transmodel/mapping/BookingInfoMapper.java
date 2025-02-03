package org.opentripplanner.apis.transmodel.mapping;

import org.opentripplanner.transit.model.timetable.booking.BookingInfo;
import org.opentripplanner.transit.model.timetable.booking.BookingTime;

/**
 * Maps the {@link BookingInfo} to enum value (as a string) returned by the API.
 */
public class BookingInfoMapper {

  public static String mapToBookWhen(BookingInfo bookingInfo) {
    if (bookingInfo.getMinimumBookingNotice().isPresent()) {
      return null;
    }
    BookingTime latestBookingTime = bookingInfo.getLatestBookingTime();
    BookingTime earliestBookingTime = bookingInfo.getEarliestBookingTime();

    // Try to deduce the original enum from stored values
    if (earliestBookingTime == null) {
      if (latestBookingTime == null) {
        return "timeOfTravelOnly";
      } else if (latestBookingTime.getDaysPrior() == 1) {
        return "untilPreviousDay";
      } else if (latestBookingTime.getDaysPrior() == 0) {
        return "advanceAndDayOfTravel";
      } else {
        return "other";
      }
    } else if (
      earliestBookingTime.getDaysPrior() == 0 &&
      (latestBookingTime == null || latestBookingTime.getDaysPrior() == 0)
    ) {
      return "dayOfTravelOnly";
    } else {
      return "other";
    }
  }
}
