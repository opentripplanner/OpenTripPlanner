package org.opentripplanner.api.model;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;

import javax.ws.rs.Path;
import javax.xml.bind.annotation.XmlRootElement;

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
