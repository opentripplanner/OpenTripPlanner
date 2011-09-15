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
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.RoutingService;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ContractionRoutingServiceImpl implements RoutingService {

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

        		GenericAStar aStar = getAStarInstance(options);

        		Graph _graph = hierarchies.getGraph();
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

    @Override
    public GraphPath route(Vertex fromVertex, Vertex toVertex, List<Vertex> intermediates,
            int time, TraverseOptions options) {

        Map<Vertex, HashMap<Vertex, GraphPath>> paths = new HashMap<Vertex, HashMap<Vertex, GraphPath>>();

        HashMap<Vertex, GraphPath> firstLegPaths = new HashMap<Vertex, GraphPath>();
        paths.put(fromVertex, firstLegPaths);

        // compute shortest paths between each pair of vertices
        for (Vertex v : intermediates) {

            /**
             * Find initial paths from the source vertex to the intermediate vertex
             */
            List<GraphPath> firstPaths = route(new State(time,fromVertex,options), v);
            if (!firstPaths.isEmpty()) {
                firstLegPaths.put(v, firstPaths.get(0));
            }

            /**
             * Find paths from this intermediate vertex to each other intermediate
             */
            HashMap<Vertex, GraphPath> outPaths = new HashMap<Vertex, GraphPath>();
            paths.put(v, outPaths);

            State intermediateState = new State(time,v,options);
            
            for (Vertex tv : intermediates) {
                /**
                 * We probably don't need to compute paths where the source and target vertex are
                 * the same
                 */
                if (v == tv)
                    continue;

                List<GraphPath> morePaths = route(intermediateState, tv);
                if (!morePaths.isEmpty()) {
                    outPaths.put(tv, morePaths.get(0));
                }
            }

            /**
             * Find paths from the intermediate vertex to the target vertex
             */
            List<GraphPath> lastPaths = route(intermediateState, toVertex);
            if (!lastPaths.isEmpty())
                outPaths.put(toVertex, lastPaths.get(0));
        }

        // compute shortest path overall
        HashSet<Vertex> vertices = new HashSet<Vertex>();
        vertices.addAll(intermediates);
        GraphPath shortestPath = TSPPathFinder.findShortestPath(toVertex, fromVertex, paths,
                vertices, time, options);
        return shortestPath;
    }
    
    /****
     * Private Methods
     ****/

    private GenericAStar getAStarInstance(TraverseOptions options) {
        GenericAStar aStar = AStar.getDefaultInstance();
        GenericAStarFactory factory = options.aStarSearchFactory;
        if (factory != null)
            aStar = factory.createAStarInstance();
        return aStar;
    }

}
