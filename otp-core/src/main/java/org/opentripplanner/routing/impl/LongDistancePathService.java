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

import static org.opentripplanner.routing.automata.Nonterminal.choice;
import static org.opentripplanner.routing.automata.Nonterminal.optional;
import static org.opentripplanner.routing.automata.Nonterminal.plus;
import static org.opentripplanner.routing.automata.Nonterminal.seq;
import static org.opentripplanner.routing.automata.Nonterminal.star;

import java.util.List;

import lombok.Setter;

import org.opentripplanner.routing.algorithm.strategies.DefaultRemainingWeightHeuristic;
import org.opentripplanner.routing.algorithm.strategies.InterleavedBidirectionalHeuristic;
import org.opentripplanner.routing.algorithm.strategies.RemainingWeightHeuristic;
import org.opentripplanner.routing.algorithm.strategies.TrivialRemainingWeightHeuristic;
import org.opentripplanner.routing.automata.DFA;
import org.opentripplanner.routing.automata.Nonterminal;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.OnboardEdge;
import org.opentripplanner.routing.edgetype.SimpleTransfer;
import org.opentripplanner.routing.edgetype.StationEdge;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.edgetype.TimedTransferEdge;
import org.opentripplanner.routing.edgetype.TransferEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.pathparser.PathParser;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.PathService;
import org.opentripplanner.routing.services.SPTService;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This PathService is intended to provide faster response times when routing over longer
 * distances (e.g. across the entire Netherlands). It only uses the street network at the first
 * and last legs of the trip, and all other transfers between transit vehicles will occur via
 * SimpleTransfer edges which must be created by the graph builder.
 * 
 * More information is available on the OTP wiki at:
 * https://github.com/openplans/OpenTripPlanner/wiki/LargeGraphs 
 */
public class LongDistancePathService implements PathService {

    private static final Logger LOG = LoggerFactory.getLogger(LongDistancePathService.class);

    @Autowired @Setter
    private GraphService graphService;
    
    @Autowired @Setter
    private SPTService sptService;

    @Setter 
    private double timeout = 0; // seconds
    
    @Override
    public List<GraphPath> getPaths(RoutingRequest options) {

        if (options == null) {
            LOG.error("PathService was passed a null routing request.");
            return null;
        }

        if (options.rctx == null) {
            options.setRoutingContext(graphService.getGraph(options.getRouterId()));
            options.rctx.pathParsers = new PathParser[] { new Parser() };
        }

        LOG.debug("rreq={}", options);
        
        RemainingWeightHeuristic heuristic;
        if (options.isDisableRemainingWeightHeuristic()) {
            heuristic = new TrivialRemainingWeightHeuristic();
        } else if (options.modes.isTransit()) {
            // Only use the BiDi heuristic for transit.
            heuristic = new InterleavedBidirectionalHeuristic(options.rctx.graph);
        } else {
            heuristic = new DefaultRemainingWeightHeuristic();
        }
        options.rctx.remainingWeightHeuristic = heuristic;
        /* In RoutingRequest, maxTransfers defaults to 2. Over long distances, we may see 
         * itineraries with far more transfers. We do not expect transfer limiting to improve
         * search times on the LongDistancePathService, so we set it to the maximum we ever expect
         * to see. Because people may use either the traditional path services or the 
         * LongDistancePathService, we do not change the global default but override it here. */
        options.setMaxTransfers(10);
        long searchBeginTime = System.currentTimeMillis();
        LOG.debug("BEGIN SEARCH");
        ShortestPathTree spt = sptService.getShortestPathTree(options, timeout);
        LOG.debug("END SEARCH ({} msec)", System.currentTimeMillis() - searchBeginTime);
        
        if (spt == null) { // timeout or other fail
            LOG.warn("SPT was null.");
            return null;
        }
        //spt.getPaths().get(0).dump();
        return spt.getPaths();
    }

    public static class Parser extends PathParser {

        private static final int STREET = 1;
        private static final int LINK = 2;
        private static final int STATION = 3;
        private static final int ONBOARD = 4;
        private static final int TRANSFER = 5;

        private static final DFA DFA;

        static {
            Nonterminal streetLeg  = plus(STREET);
            Nonterminal transitLeg = seq(plus(STATION), plus(ONBOARD), plus(STATION));
            Nonterminal stopToStop = seq(transitLeg, star(optional(TRANSFER), transitLeg));
            Nonterminal streetAndTransitItinerary = seq(streetLeg, LINK, stopToStop, LINK, streetLeg);
            // FIXME
            Nonterminal onboardItinerary = seq( plus(ONBOARD), plus(STATION), star(TRANSFER, transitLeg),
                    LINK, streetLeg);
            Nonterminal itinerary = 
                    choice(streetLeg, streetAndTransitItinerary, onboardItinerary, stopToStop);
            DFA = itinerary.toDFA().minimize();
            System.out.println(DFA.toGraphViz());
            System.out.println(DFA.dumpTable());
        }

        @Override
        protected DFA getDFA() {
            return DFA;
        }

        @Override
        public int terminalFor(State state) {
            Vertex v = state.getVertex();
            Edge e = state.getBackEdge();
            if (e == null) {
                throw new RuntimeException ("terminalFor should never be called on States without back edges!");
            }
            /* OnboardEdge currently includes BoardAlight edges. */
            if (e instanceof OnboardEdge) return ONBOARD;
            if (e instanceof StationEdge) return STATION;
            // There should perhaps be a shared superclass of all transfer edges to simplify this. 
            if (e instanceof TimedTransferEdge) return TRANSFER;
            if (e instanceof SimpleTransfer)    return TRANSFER;
            if (e instanceof TransferEdge)      return TRANSFER;
            if (e instanceof StreetTransitLink) return LINK;
            // Is it really correct to clasify all other edges as STREET?
            return STREET;
//            else {
//                LOG.debug("failed to tokenize path. vertex {} edge {}", v, e);
//                throw new RuntimeException("failed to tokenize path");
//            }
        }

    }
    
}

