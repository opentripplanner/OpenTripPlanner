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

import java.util.Collections;
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
import org.opentripplanner.routing.edgetype.StationStopEdge;
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
        List<GraphPath> paths = spt.getPaths();
        Collections.sort(paths, new PathWeightComparator());
        return paths;
    }

    public static class Parser extends PathParser {

        private static final int STREET       = 1;
        private static final int LINK         = 2;
        private static final int STATION      = 3;
        private static final int ONBOARD      = 4;
        private static final int TRANSFER     = 5;
        private static final int STATION_STOP = 6;

        private static final DFA DFA;

        static {

            /* A StreetLeg is one or more street edges. */
            Nonterminal streetLeg = plus(STREET);

            /* A TransitLeg is a ride on transit, including preboard and prealight edges at its 
             * ends. It begins and ends at a TransitStop vertex. 
             * Note that these are STATION* rather than STATION+ because some transfer edges
             * (timed transfer edges) connect arrival and depart vertices. Requiring a STATION
             * edge would prevent them from being traversed. */
            Nonterminal transitLeg = seq(star(STATION), plus(ONBOARD), star(STATION));
            
            /* A beginning gets us from the path's initial vertex to the first transit stop it 
             * passes through (its first board location). We may want to transfer at the beginning 
             * of an itinerary that begins at a station or stop, and does not use streets. */
            Nonterminal beginning = choice(seq(optional(streetLeg), LINK), seq(optional(STATION_STOP), optional(TRANSFER)));
            
            /* Begin on board transit, ending up at another stop where the "middle" can take over. */
            Nonterminal onboardBeginning = seq(plus(ONBOARD), plus(STATION), optional(TRANSFER));
            
            /* Ride transit at least one time, chaining transit legs together with single transfer edges. */
            Nonterminal middle = seq(transitLeg, star(optional(TRANSFER), transitLeg));

            /* And end gets us from the last stop to the final vertex. It is the same as a beginning, 
             * but with the sub-sequences reversed. This must cover 6 different cases: 
             * 1. leave the station and optionally walk, 
             * 2. stay at the stop where we are, 
             * 3. stay at the stop where we are but go to its parent station, 
             * 4. transfer and stay at the target stop, 
             * 5. transfer and move to the target stop's parent station. */
            Nonterminal end = choice(seq(LINK, optional(streetLeg)), seq(optional(TRANSFER), optional(STATION_STOP)));

            /* An itinerary that includes a ride on public transit. It might begin on- or offboard. 
             * if it begins onboard, it doesn't necessarily have subsequent transit legs. */
            Nonterminal transitItinerary = choice( 
                    seq(beginning, middle, end),
                    seq(onboardBeginning, optional(middle), end));
            
            /* A streets-only itinerary, which might begin or end at a stop or its station, 
             * but does not actually ride transit. */
            Nonterminal streetItinerary = seq( 
                    optional(STATION_STOP), optional(LINK), 
                    streetLeg,
                    optional(LINK), optional(STATION_STOP)); 
            
            Nonterminal itinerary = choice(streetItinerary, transitItinerary);
            
            DFA = itinerary.toDFA().minimize();
            System.out.println(DFA.toGraphViz());
            System.out.println(DFA.dumpTable());
        }

        @Override
        protected DFA getDFA() {
            return DFA;
        }

        /**
         * The terminal is based exclusively on the backEdge, i.e. each terminal represents 
         * exactly one edge in the path.
         */
        @Override
        public int terminalFor(State state) {
            Edge e = state.getBackEdge();
            if (e == null) {
                throw new RuntimeException ("terminalFor should never be called on States without back edges!");
            }
            /* OnboardEdge currently includes BoardAlight edges. */
            if (e instanceof OnboardEdge)       return ONBOARD;
            if (e instanceof StationEdge)       return STATION;
            if (e instanceof StationStopEdge)   return STATION_STOP;
            // There should perhaps be a shared superclass of all transfer edges to simplify this. 
            if (e instanceof SimpleTransfer)    return TRANSFER;
            if (e instanceof TransferEdge)      return TRANSFER;
            if (e instanceof TimedTransferEdge) return TRANSFER;
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

