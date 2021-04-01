package org.opentripplanner.api.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

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
    @JsonProperty(value="itineraries")
    public List<Itinerary> itinerary = new ArrayList<Itinerary>();

    /**
     * Rental information about the bike rental networks used in the itinerary if applicable. This will be a map
     * where keys are network names and the values are the information associated with that particular network.
     */
    public Map<String, RentalInfo> bikeRentalInfo = null;

    /**
     * Rental information about the car rental networks used in the itinerary if applicable. This will be a map
     * where keys are network names and the values are the information associated with that particular network.
     */
    public Map<String, RentalInfo> carRentalInfo = null;

    /**
     * Rental information about the vehicle rental networks used in the itinerary if applicable. This will be a map
     * where keys are network names and the values are the information associated with that particular network.
     */
    public Map<String, RentalInfo> vehicleRentalInfo = null;

    public TripPlan() { }

    public TripPlan(Place from, Place to, Date date) {
        this.from = from;
        this.to = to;
        this.date = date;
    }

    public void addItinerary(Itinerary itinerary) {
        this.itinerary.add(itinerary);
    }
}
