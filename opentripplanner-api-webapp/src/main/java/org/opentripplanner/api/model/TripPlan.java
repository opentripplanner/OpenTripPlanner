package org.opentripplanner.api.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElementWrapper;

/**
 *
 */
public class TripPlan {

    public int length;
    public Place from;
    public Place to;
    
    @XmlElementWrapper(name="itineraries")
    public List<Itinerary> itinerary = new ArrayList<Itinerary>();

    public void addItinerary(Itinerary itinerary) {
        this.itinerary.add(itinerary);
        this.length = this.itinerary.size();
    }
}
