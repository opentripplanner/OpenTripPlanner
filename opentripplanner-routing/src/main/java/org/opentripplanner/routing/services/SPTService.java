package org.opentripplanner.routing.services;

import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.spt.ShortestPathTree;

public interface SPTService {
    
    public ShortestPathTree getShortestPathTree(RoutingRequest req); // traverseoptions should be renamed SPTRequest
    
    public ShortestPathTree getShortestPathTree(RoutingRequest req, double timeoutSeconds); 

}
