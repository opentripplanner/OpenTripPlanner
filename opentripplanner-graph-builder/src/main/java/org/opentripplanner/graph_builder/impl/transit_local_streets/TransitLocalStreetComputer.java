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

package org.opentripplanner.graph_builder.impl.transit_local_streets;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.opentripplanner.common.model.T2;
import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.routing.algorithm.GenericDijkstra;
import org.opentripplanner.routing.algorithm.strategies.TransitLocalStreetService;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.raptor.MaxWalkState;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransitLocalStreetComputer implements GraphBuilder {
    private static Logger log = LoggerFactory.getLogger(TransitLocalStreetComputer.class);
    private boolean saveShortestPaths = false;

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
        HashSet<Vertex> transitShortestPathVertices = new HashSet<Vertex>();
        
        // This is necessary for storing walk distances by vertex index
        graph.rebuildVertexAndEdgeIndices();

        RoutingRequest walk = new RoutingRequest(TraverseMode.WALK);
        RoutingRequest wheelchair = new RoutingRequest(TraverseMode.WALK);
        wheelchair.wheelchairAccessible = true;
        RoutingRequest bikeflat = new RoutingRequest(TraverseMode.BICYCLE);
        bikeflat.optimize = OptimizeType.FLAT;
        RoutingRequest bikesafe = new RoutingRequest(TraverseMode.BICYCLE);
        bikesafe.optimize = OptimizeType.SAFE;
        RoutingRequest bikequick = new RoutingRequest(TraverseMode.BICYCLE);
        bikequick.optimize = OptimizeType.QUICK;
        RoutingRequest[] requests = new RoutingRequest[] { walk, wheelchair, bikeflat, bikesafe,
                bikequick };

        int i = 0;

        final Collection<Vertex> allVertices = graph.getVertices();
        HashMap<Vertex, HashMap<Vertex, int[]>> paths = new HashMap<Vertex, HashMap<Vertex, int[]>>();
        HashMap<Vertex, HashMap<Vertex, T2<Double, Integer>>> costs = new HashMap<Vertex, HashMap<Vertex, T2<Double, Integer>>>();
        for (Vertex v : allVertices) {
            ++i;
            if (i % 1000 == 0) {
                log.debug(i + " / " + allVertices.size());
            }
            if (!(v instanceof TransitStop)) {
                continue;
            }
            // find SPT from this transit stop according to various modes; get all transit
            // stops in spt; get path to said stops; mark vertices
            for (RoutingRequest req : requests) {
                req.setRoutingContext(graph, v, null);
                req.setMaxWalkDistance(3000);
                GenericDijkstra dijkstra = new GenericDijkstra(req);
                State origin = new MaxWalkState(v, req);
                ShortestPathTree spt = dijkstra.getShortestPathTree(origin);
                HashMap<Vertex, int[]> map = null;
                HashMap<Vertex, T2<Double, Integer>> cost = null;
                if (req == walk && saveShortestPaths ) {
                    map = new HashMap<Vertex, int[]>();
                    cost = new HashMap<Vertex, T2<Double, Integer>>();
                    paths.put(v, map);
                    costs.put(v, cost);
                }
                int[] path = new int[1000];

                for (State s : spt.getAllStates()) {
                    Vertex destStopVertex = s.getVertex();
                    if (destStopVertex instanceof TransitStop) {
                        int pathIndex = 0;
                        if (req == walk && saveShortestPaths ) {
                           cost.put(destStopVertex, new T2<Double, Integer>(s.getWalkDistance(), (int) s.getElapsedTime()));
                        }
                        while (s != null) {
                            transitShortestPathVertices.add(s.getVertex());
                            path[pathIndex++] = s.getVertex().getIndex();
                            s = s.getBackState();
                        }
                        if (req == walk && saveShortestPaths ) {
                            map.put(destStopVertex, Arrays.copyOf(path, pathIndex));
                        }

                    }
                }
            }
        }
        TransitLocalStreetService service = new TransitLocalStreetService(
                transitShortestPathVertices, paths, costs);
        graph.putService(TransitLocalStreetService.class, service);
    }

    @Override
    public List<String> provides() {
        return Arrays.asList("transit_local_streets");
    }

    @Override
    public List<String> getPrerequisites() {
        return Arrays.asList("streets", "transit", "linking");
    }

    @Override
    public void checkInputs() {
        // nothing to do
    }

}
