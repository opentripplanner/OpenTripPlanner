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

package org.opentripplanner.graph_builder.impl;

import static org.opentripplanner.routing.automata.Nonterminal.seq;
import static org.opentripplanner.routing.automata.Nonterminal.star;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Iterables;
import org.opentripplanner.api.resource.CoordinateArrayListSequence;
import org.opentripplanner.common.geometry.DistanceLibrary;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.graph_builder.annotation.StopNotLinkedForTransfers;
import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.routing.algorithm.EarliestArrivalSPTService;
import org.opentripplanner.routing.automata.DFA;
import org.opentripplanner.routing.automata.Nonterminal;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.PathwayEdge;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.SimpleTransfer;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.pathparser.PathParser;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

/**
 * {@link GraphBuilder} plugin that links up the stops of a transit network among themselves.
 */
public class StreetfulStopLinker implements GraphBuilder {
    private static Logger LOG = LoggerFactory.getLogger(StreetfulStopLinker.class);

    int maxDuration = 60 * 10;

    DistanceLibrary distanceLibrary = SphericalDistanceLibrary.getInstance();

    public List<String> provides() {
        return Arrays.asList("linking");
    }

    public List<String> getPrerequisites() {
        return Arrays.asList("street to transit");
    }

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
        final Parser parser[] = new Parser[] {new Parser()};
        GeometryFactory geometryFactory = GeometryUtils.getGeometryFactory();
        EarliestArrivalSPTService earliestArrivalSPTService = new EarliestArrivalSPTService();
        earliestArrivalSPTService.maxDuration = (maxDuration);

        for (TransitStop ts : Iterables.filter(graph.getVertices(), TransitStop.class)) {
            // Only link street linkable stops
            if (!ts.isStreetLinkable())
                continue;
            LOG.trace("linking stop '{}' {}", ts.getStop(), ts);

            // Determine the set of pathway/transfer destinations
            Set<TransitStop> pathwayDestinations = new HashSet<TransitStop>();
            for (Edge e : ts.getOutgoing()) {
                if (e instanceof PathwayEdge || e instanceof SimpleTransfer) {
                    if (e.getToVertex() instanceof TransitStop) {
                        TransitStop to = (TransitStop) e.getToVertex();
                        pathwayDestinations.add(to);
                    }
                }
            }

            int n = 0;
            RoutingRequest routingRequest = new RoutingRequest(TraverseMode.WALK);
            routingRequest.clampInitialWait = (0L);
            routingRequest.setRoutingContext(graph, ts, null);
            routingRequest.rctx.pathParsers = parser;
            ShortestPathTree spt = earliestArrivalSPTService.getShortestPathTree(routingRequest);

            if (spt != null) {
                for (State state : spt.getAllStates()) {
                    Vertex vertex = state.getVertex();
                    if (ts == vertex) continue;

                    if (vertex instanceof TransitStop) {
                        TransitStop other = (TransitStop) vertex;
                        if (!other.isStreetLinkable())
                            continue;
                        if (pathwayDestinations.contains(other)) {
                            LOG.trace("Skipping '{}', {}, already connected.", other.getStop(),
                                    other);
                            continue;
                        }
                        double distance = 0.0;
                        GraphPath graphPath = new GraphPath(state, false);
                        CoordinateArrayListSequence coordinates = new CoordinateArrayListSequence();

                        for (Edge edge : graphPath.edges) {
                            if (edge instanceof StreetEdge) {
                                LineString geometry = edge.getGeometry();

                                if (geometry != null) {
                                    if (coordinates.size() == 0) {
                                        coordinates.extend(geometry.getCoordinates());
                                    } else {
                                        coordinates.extend(geometry.getCoordinates(), 1);
                                    }
                                }

                                distance += edge.getDistance();
                            }
                        }

                        if (coordinates.size() < 2) {   // Otherwise the walk step generator breaks.
                            ArrayList<Coordinate> coordinateList = new ArrayList<Coordinate>(2);
                            coordinateList.add(graphPath.states.get(1).getVertex().getCoordinate());
                            State lastState = graphPath.states.getLast().getBackState();
                            coordinateList.add(lastState.getVertex().getCoordinate());
                            coordinates = new CoordinateArrayListSequence(coordinateList);
                        }

                        LineString geometry = geometryFactory.createLineString(new
                                PackedCoordinateSequence.Double(coordinates.toCoordinateArray()));
                        LOG.trace("  to stop: '{}' {} ({}m) [{}]", other.getStop(), other, distance, geometry);
                        new SimpleTransfer(ts, other, distance, geometry);
                        n++;
                    }
                }
            }

            LOG.trace("linked to {} others.", n);
            if (n == 0) {
                LOG.warn(graph.addBuilderAnnotation(new StopNotLinkedForTransfers(ts)));
            }
        }
    }

    @Override
    public void checkInputs() {
        // No inputs
    }

    private static class Parser extends PathParser {
        private static final int OTHER  = 0;
        private static final int STREET = 1;
        private static final int LINK   = 2;

        private final DFA DFA;

        Parser() {
            Nonterminal streets   = star(STREET);

            Nonterminal itinerary = seq(LINK, streets, LINK);

            DFA = itinerary.toDFA().minimize();
        }

        @Override
        public int terminalFor(State state) {
            Edge edge = state.getBackEdge();

            if (edge instanceof StreetEdge)   return STREET;
            if (edge instanceof StreetTransitLink) return LINK;

            return OTHER;
        }
        
        @Override
		protected
        DFA getDFA() {
        	return this.DFA;
        }
    }
}
