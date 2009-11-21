package org.opentripplanner.api.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElementWrapper;

/**
 *
 */
public class Itinerary {

    public TimeDistance timeDistance = new TimeDistance();

    public Fare fare = new Fare();

    @XmlElementWrapper(name = "legs")
    public List<Leg> leg = new ArrayList<Leg>();
    
    public void addLeg(Leg leg) {
        this.leg.add(leg);
    }
}
