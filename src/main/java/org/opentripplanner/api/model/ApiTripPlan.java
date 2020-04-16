package org.opentripplanner.api.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A TripPlan is a set of ways to get from point A to point B at time T.
 */
public class ApiTripPlan {

    /**  The time and date of travel */
    public Date date;
    
    /** The origin */
    public ApiPlace from;
    
    /** The destination */
    public ApiPlace to;

    /** List of itineraries. */
    public List<ApiItinerary> itineraries = new ArrayList<>();
}
