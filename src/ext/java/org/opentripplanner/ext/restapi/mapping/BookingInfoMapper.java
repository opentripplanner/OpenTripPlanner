package org.opentripplanner.ext.restapi.mapping;

import org.opentripplanner.ext.restapi.model.ApiBookingInfo;
import org.opentripplanner.model.BookingInfo;

public class BookingInfoMapper {

  static ApiBookingInfo mapBookingInfo(BookingInfo info, boolean isPickup) {
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
      // we only want to show the pick up message for pickups
      isPickup ? info.getPickupMessage() : null,
      // and only the drop off message for drop offs
      !isPickup ? info.getDropOffMessage() : null
    );
  }
}
