package org.opentripplanner.api.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlElementWrapper;

/**
 *
 */
public class Itinerary {

    public long duration = 0;
    public Date startTime = null;
    public Date endTime = null;

    public Double walkTime = 0.0;
    public Double transitTime = 0.0;
    public Double waitingTime = 0.0;
    
    public Double walkDistance = 0.0;

    public Integer transfers = 0;

    public Fare fare = new Fare();

    @XmlElementWrapper(name = "legs")
    public List<Leg> leg = new ArrayList<Leg>();
    
    public void addLeg(Leg leg) {
        this.leg.add(leg);
    }
}
