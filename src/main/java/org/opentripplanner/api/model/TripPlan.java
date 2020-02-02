package org.opentripplanner.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.xml.bind.annotation.XmlElementWrapper;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A TripPlan is a set of ways to get from point A to point B at time T.
 */
public class TripPlan {

    /**  The time and date of travel */
    public Date date = null;
    
    /** The origin */
    public ApiPlace from = null;
    
    /** The destination */
    public ApiPlace to = null;

    /**
     * A list of possible itineraries. The wrapper is named 'itineraries' while the
     * elements them self is named 'itinerary'.
     */
    @XmlElementWrapper(name="itineraries")
    @JsonProperty(value="itineraries")
    public List<ApiItinerary> itinerary = new ArrayList<ApiItinerary>();

    public TripPlan(ApiPlace from, ApiPlace to, Date date, List<ApiItinerary> itineraries) {
        this.from = from;
        this.to = to;
        this.date = date;
        this.itinerary.addAll(itineraries);
    }
}
