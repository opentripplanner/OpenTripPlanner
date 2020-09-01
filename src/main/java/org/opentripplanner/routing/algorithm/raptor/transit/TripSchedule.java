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

    /**
     * Raptor save memory by NOT storing the board/arrival stop positions in pattern; Hence we need
     * to resolve this when mapping into a itinerary leg.
     * <p/>
     * Find the pattern stop index for a given stop, and arrival/departure time. We need the time
     * in addition to the stop in cases were the trip pattern visit the same stop twice. Also the
     * time is not sufficient since more than one stop could have the exact same departure time.
     */
    int findStopPosInPattern(int stopIndex, int time, boolean departure);
}