package org.opentripplanner.routing.algorithm.raptor.transit;

import org.opentripplanner.model.TripPattern;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

import java.time.LocalDate;

/**
 * Extension of RaptorTripSchedule passed through Raptor searches to be able to retrieve the
 * original trip from the path when creating itineraries.
 */
public interface TripSchedule extends RaptorTripSchedule {

    /**
     * TODO OTP2 - Add JavaDoc
     */
    TripTimes getOriginalTripTimes();

    /**
     * TODO OTP2 - Add JavaDoc
     */
    TripPattern getOriginalTripPattern();

    LocalDate getServiceDate();
}