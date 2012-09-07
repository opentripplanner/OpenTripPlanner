package org.opentripplanner.routing.impl.raptor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.common.model.T2;

public class RaptorData implements Serializable {

    private static final long serialVersionUID = -2387510738104439133L;

    public RaptorStop[] stops;

    public List<RaptorRoute> routes = new ArrayList<RaptorRoute>();

    public List<RaptorRoute>[] routesForStop;

    public HashMap<AgencyAndId, RaptorStop> raptorStopsForStopId = new HashMap<AgencyAndId, RaptorStop>();

    public RegionData regionData;

    //unused
    public List<T2<Double, RaptorStop>>[] nearbyStops;

    public MaxTransitRegions maxTransitRegions;

}
