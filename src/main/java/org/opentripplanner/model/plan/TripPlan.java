package org.opentripplanner.model.plan;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * A TripPlan is a set of ways to get from point A to point B at time T.
 */
public class TripPlan {

    /**  The time and date of travel */
    public final Date date;

    /** The origin */
    public final Place from;

    /** The destination */
    public final Place to;

    public final List<Itinerary> itineraries;

    public TripPlan(Place from, Place to, Date date, Collection<Itinerary> itineraries) {
        this.from = from;
        this.to = to;
        this.date = date;
        this.itineraries = List.copyOf(itineraries);
    }

    @Override
    public String toString() {
        return "TripPlan{" + "date=" + date + ", from=" + from + ", to=" + to
                + ", itineraries=" + itineraries + '}';
    }
}
