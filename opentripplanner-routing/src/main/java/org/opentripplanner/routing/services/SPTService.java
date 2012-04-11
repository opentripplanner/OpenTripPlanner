package org.opentripplanner.routing.services;

import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.spt.ShortestPathTree;

public interface SPTService {
    
    public ShortestPathTree getShortestPathTree(TraverseOptions req); // traverseoptions should be renamed SPTRequest
    
    public ShortestPathTree getShortestPathTree(TraverseOptions req, double timeoutSeconds); 

}
