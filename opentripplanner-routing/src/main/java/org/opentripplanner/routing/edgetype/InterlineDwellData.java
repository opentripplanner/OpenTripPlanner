package org.opentripplanner.routing.edgetype;

import java.io.Serializable;

import org.onebusaway.gtfs.model.AgencyAndId;

/** 
 * A vehicle's wait between the end of one run and the beginning of another run on the same block 
 * */
public class InterlineDwellData implements Serializable {

    private static final long serialVersionUID = 1L;

    public int dwellTime;

    public int patternIndex;

    public AgencyAndId trip;
    
    public InterlineDwellData(int dwellTime, int patternIndex, AgencyAndId trip) {
        this.dwellTime = dwellTime;
        this.patternIndex = patternIndex;
        this.trip = trip;
    }
}