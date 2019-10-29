package org.opentripplanner.transit.raptor.speed_test.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
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

    /** A list of possible itineraries */
    @XmlElementWrapper(name="itineraries")
    @XmlElement(name = "itinerary")
    @JsonProperty(value="itineraries")
    private List<Itinerary> itineraries = new ArrayList<Itinerary>();

    public TripPlan() { }

    public Collection<Itinerary> getItineraries() {
        return itineraries;
    }

    public void addItinerary(Itinerary itinerary) {
        if(itinerary != null) {
            this.itineraries.add(itinerary);
        }
    }

    public void sort() {
        itineraries.sort(Comparator.comparing(o -> o.endTime));
    }

}
