package org.opentripplanner.routing.impl.raptor;

import java.io.Serializable;

public class MaxTransitRegions implements Serializable {
    private static final long serialVersionUID = 397823877868846217L;

    public int[][][] maxTransit;
    
    //this is a giant hack; we should be using true julian dates
    public int startYear;
    public int startMonth;
    public int startDay;

    public double minSpeed;

    public double maxDistance;

}
