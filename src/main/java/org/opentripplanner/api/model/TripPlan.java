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
    public Place from = null;
    
    /** The destination */
    public Place to = null;

    /**
     * A list of possible itineraries. The wrapper is named 'itineraries' while the
     * elements them self is named 'itinerary'.
     */
    @XmlElementWrapper(name="itineraries")
    @JsonProperty(value="itineraries")
    public List<Itinerary> itinerary = new ArrayList<Itinerary>();

    public TripPlan(Place from, Place to, Date date, List<Itinerary> itineraries) {
        this.from = from;
        this.to = to;
        this.date = date;
        this.itinerary.addAll(itineraries);
    }
}
