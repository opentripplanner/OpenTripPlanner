package org.opentripplanner.api.model;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class Itinerary {
    public TimeDistance timeDistance;

    public Fare fare;

    public List<Leg> leg;

    public Itinerary() {
        timeDistance = new TimeDistance();
        fare = new Fare();
        leg = new ArrayList<Leg>();
        leg.add(new Leg());
        leg.add(new Leg());
        leg.add(new Leg());
    }
}
