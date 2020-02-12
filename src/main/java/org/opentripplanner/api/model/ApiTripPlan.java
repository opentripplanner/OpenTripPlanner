package org.opentripplanner.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.xml.bind.annotation.XmlElementWrapper;
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

    /**
     * A list of possible itineraries. The wrapper is named 'itineraries' while the
     * elements them self is named 'itinerary'.
     */
    @XmlElementWrapper(name="itineraries")
    @JsonProperty(value="itineraries")
    public List<ApiItinerary> itinerary = new ArrayList<>();
}
