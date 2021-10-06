package org.opentripplanner.transit.raptor.rangeraptor.path;

import org.opentripplanner.transit.raptor.api.view.ArrivalView;
import org.opentripplanner.transit.raptor.api.view.BoardAndAlightTime;


/**
 * This interface is used by the path mappers to find the board and alight times for a known trip.
 */
@FunctionalInterface
interface BoardAndAlightTimeSearch {

    /**
     * Search for board- and alight-times for the trip matching the given stop-arrival.
     */
    BoardAndAlightTime find(ArrivalView<?> arrival);
}
