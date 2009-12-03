package org.opentripplanner.api.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlElementWrapper;

/**
 *
 */
public class TripPlan {

    public Date date = null;
    public Place from = null;
    public Place to = null;
    
    public TripPlan() {}
    
    public TripPlan(Place from, Place to, Date date) {
        this.from = from;
        this.to = to;
        this.date = date;
    }
    
    @XmlElementWrapper(name="itineraries")
    public List<Itinerary> itinerary = new ArrayList<Itinerary>();

    public void addItinerary(Itinerary itinerary) {
        this.itinerary.add(itinerary);
    }
}
