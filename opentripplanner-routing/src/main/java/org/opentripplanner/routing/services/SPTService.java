package org.opentripplanner.routing.services;

import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.spt.ShortestPathTree;

public interface SPTService {
    
    public ShortestPathTree getSPT(TraverseOptions req); // traverseoptions should be renamed SPTRequest
    
}
