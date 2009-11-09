package org.opentripplanner.api.model;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class TripPlan {

    public int length;
    public Point from;
    public Point to;
    public List<Itinerary> itinerary;

    public TripPlan() {
        itinerary = new ArrayList<Itinerary>();
        itinerary.add(new Itinerary());
        itinerary.add(new Itinerary());

        from = new Point(-122.5, 45.5, "PDX");
        to = new Point(-122.5, 45.5, "Zoo");
        length = itinerary.size();
    }
}
