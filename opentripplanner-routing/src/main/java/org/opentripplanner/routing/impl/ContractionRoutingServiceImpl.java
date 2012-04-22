/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.algorithm.GenericAStar;
import org.opentripplanner.routing.algorithm.strategies.GenericAStarFactory;
import org.opentripplanner.routing.contraction.ContractionHierarchy;
import org.opentripplanner.routing.contraction.ContractionHierarchySet;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.edgetype.LegSwitchingEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.RoutingService;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

public class ContractionPathServiceImpl implements PathService {

    private static final Logger LOG = LoggerFactory.getLogger(ContractionRoutingServiceImpl.class);
    private static final int MAX_TRIES = 4;
    private GraphService _graphService;
    
    @Autowired
    public void setGraphService(GraphService graphService) {
        _graphService = graphService;
    }

    @Override
    public List<GraphPath> route(State origin, Vertex target) {
        
        TraverseOptions options = origin.getOptions();

        ContractionHierarchySet hierarchies = _graphService.getContractionHierarchySet();

        ContractionHierarchy hierarchy = null;
        if (hierarchies != null)
            hierarchy = hierarchies.getHierarchy(options);
        
        int tries = 0;
        
        // this loop allows retrying the search with progressively longer walking distances
        while (tries < MAX_TRIES) {
        	if (options.remainingWeightHeuristic != null) {
        		options.remainingWeightHeuristic.reset();
        	}
        	tries += 1;
        	LOG.debug("try number {} ; max walk distance = {}", tries, options.getMaxWalkDistance());
        	if (hierarchy == null) {
        	        LOG.debug("No contraction hierarchies for this mode, falling back on A*.");

        		GenericAStar aStar = getAStarInstance(options);

        		Graph _graph = _graphService.getGraph();
        		ShortestPathTree spt = aStar.getShortestPathTree(_graph, origin, target);
        		if (spt == null) {
        		    // search failed, likely due to timeout
        		    return null;
        		}
        		List<GraphPath> paths = spt.getPaths(target, true);
        		if (paths == null || paths.isEmpty()) {
        		    // no paths found, retry with increased walking distance
        		    options.setMaxWalkDistance(options.getMaxWalkDistance() * 2);
        		    continue; 
        		} else {
        		    return paths;
        		}
        	}
                LOG.debug("Contraction hierarchies exist for this mode, using them.");

        	Vertex fromVertex = options.isArriveBy() ? target : origin.getVertex();
        	Vertex toVertex = options.isArriveBy() ? origin.getVertex() : target;
        	        
        	GraphPath path = hierarchy.getShortestPath(fromVertex, toVertex, origin.getTime(), options);
        	if (path == null) {
        	    options.setMaxWalkDistance(options.getMaxWalkDistance() * 2);
        	    continue;
        	} else {
        	    return Arrays.asList(path);
        	}
        }
        return Collections.emptyList();
    }

}
