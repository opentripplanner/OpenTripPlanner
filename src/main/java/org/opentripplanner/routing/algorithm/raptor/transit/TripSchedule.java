package org.opentripplanner.routing.algorithm.raptor.transit;

import org.opentripplanner.model.TripPattern;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.transit.raptor.api.transit.TripScheduleInfo;

/**
 * Extension of TripScheduleInfo passed through Range Raptor searches to be able to retrieve the
 * original trip from the path when creating itineraries.
 */
public interface TripSchedule extends TripScheduleInfo {

    /**
     * TODO OTP2 - Add JavaDoc
     */
    TripTimes getOriginalTripTimes();

    /**
     * TODO OTP2 - Add JavaDoc
     */
    TripPattern getOriginalTripPattern();

}