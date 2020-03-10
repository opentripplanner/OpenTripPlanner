package org.opentripplanner.transit.raptor.api.transit;

public interface BoardAndAlightSlackProvider<T extends RaptorTripSchedule> {

    /**
     * The board slack for the first transit b
     * @param pattern
     * @return
     */
    default int accessBoardSlack(TripPatternInfo<T> pattern) { return 0; }
    default int boardSlack(TripPatternInfo<T> pattern) { return 0; }
    default int alightSlack(TripPatternInfo<T> pattern) { return 0; }
}
