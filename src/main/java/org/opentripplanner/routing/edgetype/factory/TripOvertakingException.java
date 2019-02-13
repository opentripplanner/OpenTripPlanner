package org.opentripplanner.routing.edgetype.factory;

import org.opentripplanner.model.Trip;

public class TripOvertakingException extends RuntimeException {

    /**
     * Thrown when a trip overtakes another trip on the same pattern. 
     */
    private static final long serialVersionUID = 1L;
    public Trip overtaker, overtaken;
    public int stopIndex;
    
    public TripOvertakingException(Trip overtaker, Trip overtaken, int stopIndex) {
        this.overtaker = overtaker;
        this.overtaken = overtaken;
        this.stopIndex = stopIndex;
    }

    @Override
    public String getMessage() {
        return "Possible GTFS feed error: Trip " + overtaker + " overtakes trip " + overtaken
        + " (which has the same stops) at stop index "
        + stopIndex + " This will be handled correctly but inefficiently.";
    }
    
}
