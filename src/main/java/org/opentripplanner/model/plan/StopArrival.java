package org.opentripplanner.model.plan;

import org.opentripplanner.model.base.ToStringBuilder;

import java.util.Calendar;


/**
 * This class is used to represent a stop arrival event mostly for intermediate visits to a stops
 * along a route.
 */
public class StopArrival {
    public final Place place;
    /**
     * The time the rider will arrive at the place.
     */
    public final Calendar arrival;

    /**
     * The time the rider will depart the place.
     */
    public final Calendar departure;


    public StopArrival(Place place, Calendar arrival, Calendar departure) {
        this.place = place;
        this.arrival = arrival;
        this.departure = departure;
    }

    @Override
    public String toString() {
        return ToStringBuilder.of(StopArrival.class)
                .addCalTime("arrival",arrival)
                .addCalTime("departure", departure)
                .addObj("place", place)
                .toString();
    }
}
