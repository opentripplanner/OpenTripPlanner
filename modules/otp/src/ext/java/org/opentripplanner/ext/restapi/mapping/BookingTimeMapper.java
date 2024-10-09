package org.opentripplanner.ext.restapi.mapping;

import org.opentripplanner.ext.restapi.model.ApiBookingTime;
import org.opentripplanner.transit.model.timetable.booking.BookingTime;

public class BookingTimeMapper {

  static ApiBookingTime mapBookingTime(BookingTime time) {
    if (time == null) {
      return null;
    }
    return new ApiBookingTime(time.getTime().toSecondOfDay(), time.getDaysPrior());
  }
}
