package org.opentripplanner.ext.restapi.mapping;

import org.opentripplanner.ext.restapi.model.ApiBookingInfo;
import org.opentripplanner.transit.model.timetable.booking.BookingInfo;

public class BookingInfoMapper {

  static ApiBookingInfo mapBookingInfoForPickup(BookingInfo info) {
    return mapBookingInfo(info, true);
  }

  static ApiBookingInfo mapBookingInfoForDropOff(BookingInfo info) {
    return mapBookingInfo(info, false);
  }

  /**
   * @param isPickup either pickup or dropOff message must be set, not both. We only want to show
   *                 the pick-up message for pickups, and the drop-off message for drop-offs.
   */
  private static ApiBookingInfo mapBookingInfo(BookingInfo info, boolean isPickup) {
    if (info == null) {
      return null;
    }

    return new ApiBookingInfo(
      ContactInfoMapper.mapContactInfo(info.getContactInfo()),
      BookingMethodMapper.mapBookingMethods(info.bookingMethods()),
      BookingTimeMapper.mapBookingTime(info.getEarliestBookingTime()),
      BookingTimeMapper.mapBookingTime(info.getLatestBookingTime()),
      info.getMinimumBookingNotice(),
      info.getMaximumBookingNotice(),
      info.getMessage(),
      isPickup ? info.getPickupMessage() : null,
      !isPickup ? info.getDropOffMessage() : null
    );
  }
}
