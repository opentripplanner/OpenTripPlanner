package org.opentripplanner.api.rest.mapping;

import org.opentripplanner.api.rest.model.ApiBookingTime;
import org.opentripplanner.model.BookingTime;

public class BookingTimeMapper {

    static ApiBookingTime mapBookingTime(BookingTime time) {
        if (time == null) {return null;}
        return new ApiBookingTime(time.getTime().toSecondOfDay(), time.getDaysPrior());
    }

}
