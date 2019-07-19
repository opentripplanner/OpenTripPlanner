package org.opentripplanner.routing.algorithm.raptor.transit;

import com.conveyal.r5.otp2.api.transit.TripScheduleInfo;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.trippattern.TripTimes;

/**
 * Extension of TripScheduleInfo passed through Range Raptor searches to be able to retrieve the original trip
 * from the path when creating itineraries.
 */

public interface TripSchedule extends TripScheduleInfo {
    TripTimes getOriginalTripTimes();
    TripPattern getOriginalTripPattern();
    int getServiceCode();
}
