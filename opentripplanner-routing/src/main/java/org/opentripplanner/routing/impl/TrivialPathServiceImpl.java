package org.opentripplanner.routing.impl;

import java.util.Collections;
import java.util.List;

import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.PathService;
import org.opentripplanner.routing.services.SPTService;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.springframework.beans.factory.annotation.Autowired;

public class TrivialPathServiceImpl implements PathService {

    @Autowired GraphService graphService;
    
    @Autowired SPTService sptService;
    
    @Override
    public List<GraphPath> getPaths(TraverseOptions options) {
        ShortestPathTree spt = sptService.getShortestPathTree(options);
        if (spt == null)
            return Collections.emptyList();
        return spt.getPaths();
    }

}
