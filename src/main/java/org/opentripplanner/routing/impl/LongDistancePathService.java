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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.onebusaway.gtfs.model.AgencyAndId;
import static org.opentripplanner.routing.automata.Nonterminal.choice;
import static org.opentripplanner.routing.automata.Nonterminal.optional;
import static org.opentripplanner.routing.automata.Nonterminal.plus;
import static org.opentripplanner.routing.automata.Nonterminal.seq;
import static org.opentripplanner.routing.automata.Nonterminal.star;

import org.opentripplanner.routing.algorithm.strategies.DefaultRemainingWeightHeuristic;
import org.opentripplanner.routing.algorithm.strategies.InterleavedBidirectionalHeuristic;
import org.opentripplanner.routing.algorithm.strategies.RemainingWeightHeuristic;
import org.opentripplanner.routing.algorithm.strategies.TrivialRemainingWeightHeuristic;
import org.opentripplanner.routing.automata.DFA;
import org.opentripplanner.routing.automata.Nonterminal;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.*;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.pathparser.PathParser;
import org.opentripplanner.routing.services.PathService;
import org.opentripplanner.routing.services.SPTService;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.opentripplanner.routing.automata.Nonterminal.*;

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

    private static final double DEFAULT_MAX_WALK = 2000;
    private static final double CLAMP_MAX_WALK = 15000;

    private Graph graph;
    private SPTServiceFactory sptServiceFactory;

    public LongDistancePathService(Graph graph, SPTServiceFactory sptServiceFactory) {
        this.graph = graph;
        this.sptServiceFactory = sptServiceFactory;
    }

    // Timeout in seconds relative to initial search begin time, for each new path found (generally decreasing)
    private static double[] timeouts = new double[] {5, 2, 1, 0.5, 0.25};
	private SPTVisitor sptVisitor;
    
    @Override
    public List<GraphPath> getPaths(RoutingRequest options) {

        if (options == null) {
            LOG.error("PathService was passed a null routing request.");
            return null;
        }
        
        SPTService sptService = this.sptServiceFactory.instantiate();

        if (options.rctx == null) {
            options.setRoutingContext(graph);
            /* Use a pathparser that constrains the search to use SimpleTransfers. */
            options.rctx.pathParsers = new PathParser[] { new Parser() };
        }

        LOG.debug("rreq={}", options);

        RemainingWeightHeuristic heuristic;
        if (options.disableRemainingWeightHeuristic) {
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
        options.setMaxTransfers(4);
        options.longDistance = true;

        /* In long distance mode, maxWalk has a different meaning. It's the radius around the origin or destination
         * within which you can walk on the streets. If no value is provided, max walk defaults to the largest
         * double-precision float. This would cause long distance mode to do unbounded street searches and consider
         * the whole graph walkable. */
        if (options.maxWalkDistance == Double.MAX_VALUE) options.maxWalkDistance = DEFAULT_MAX_WALK;
        if (options.maxWalkDistance > CLAMP_MAX_WALK) options.maxWalkDistance = CLAMP_MAX_WALK;
        long searchBeginTime = System.currentTimeMillis();
        LOG.debug("BEGIN SEARCH");
        List<GraphPath> paths = Lists.newArrayList();
        Set<AgencyAndId> bannedTrips = Sets.newHashSet();
        while (paths.size() < options.numItineraries && paths.size() < timeouts.length) {
            double timeout = searchBeginTime + (timeouts[paths.size()] * 1000) - System.currentTimeMillis();
            // if (timeout <= 0) break; ADD THIS LINE TO MAKE TIMEOUTS ACTUALLY WORK WHEN NEGATIVE
            ShortestPathTree spt = sptService.getShortestPathTree(options, timeout);
            if (spt == null) { // timeout or other fail
                LOG.warn("SPT was null.");
                return null;
            }
            if (options.rctx.aborted) break;
//            if( this.sptVisitor!=null ){
//                this.sptVisitor.spt = spt;
//            }
            List<GraphPath> newPaths = spt.getPaths();
            if (newPaths.isEmpty()) break;
            // Find all trips used in this path and ban them
            for (GraphPath path : newPaths) {
                for (State state : path.states) {
                    AgencyAndId tripId = state.getTripId();
                    if (tripId != null) options.banTrip(tripId);
                }
            }
            //spt.getPaths().get(0).dump();
            paths.addAll(newPaths);
            LOG.debug("we have {} paths", paths.size());
        }
        LOG.debug("END SEARCH ({} msec)", System.currentTimeMillis() - searchBeginTime);
        Collections.sort(paths, new PathWeightComparator());
        return paths;
    }

    public static class Parser extends PathParser {

        static final int STREET       = 1;
        static final int LINK         = 2;
        static final int STATION      = 3;
        static final int ONBOARD      = 4;
        static final int TRANSFER     = 5;
        static final int STATION_STOP = 6;
        static final int STOP_STATION = 7;

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
            Nonterminal end = choice(seq(LINK, optional(streetLeg)), seq(optional(TRANSFER), optional(STOP_STATION)));

            /* An itinerary that includes a ride on public transit. It might begin on- or offboard. 
             * if it begins onboard, it doesn't necessarily have subsequent transit legs. */
            Nonterminal transitItinerary = choice( 
                    seq(beginning, middle, end),
                    seq(onboardBeginning, optional(middle), end));
            
            /* A streets-only itinerary, which might begin or end at a stop or its station, 
             * but does not actually ride transit. */
            Nonterminal streetItinerary = choice(TRANSFER, seq(
                    optional(STATION_STOP), optional(LINK), 
                    streetLeg,
                    optional(LINK), optional(STOP_STATION)));
            
            Nonterminal itinerary = choice(streetItinerary, transitItinerary);
            
            DFA = itinerary.toDFA().minimize();
            // System.out.println(DFA.toGraphViz());
            // System.out.println(DFA.dumpTable());
        }

        @Override
        protected DFA getDFA() {
            return DFA;
        }

        /**
         * The terminal is normally based exclusively on the backEdge, i.e. each terminal represents
         * exactly one edge in the path. In case of @link{StationStopEdge}, however, the type of the
         * current vertex also determines what kind of terminal this is.
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
            if (e instanceof StationStopEdge) {
                return state.getVertex() instanceof TransitStop ? STATION_STOP : STOP_STATION;
            }
            // There should perhaps be a shared superclass of all transfer edges to simplify this. 
            if (e instanceof SimpleTransfer)    return TRANSFER;
            if (e instanceof TransferEdge)      return TRANSFER;
            if (e instanceof TimedTransferEdge) return TRANSFER;
            if (e instanceof StreetTransitLink) return LINK;
            if (e instanceof PathwayEdge)       return LINK;
            // Is it really correct to clasify all other edges as STREET?
            return STREET;
        }

    }

	@Override
	public void setSPTVisitor(SPTVisitor vis) {
		this.sptVisitor = vis;
	}
    
}
