package org.opentripplanner.routing.impl.raptor;

import java.io.Serializable;

import org.onebusaway.gtfs.model.AgencyAndId;

public class RaptorInterlineData implements Serializable {
    private static final long serialVersionUID = -590861028792593164L;

    public RaptorRoute fromRoute;
    public RaptorRoute toRoute;
    public AgencyAndId fromTripId;
    public AgencyAndId toTripId;

    public int fromPatternIndex = -1;
    public int fromTripIndex = -1;
    public int toPatternIndex = -1;
    public int toTripIndex = -1;

}
