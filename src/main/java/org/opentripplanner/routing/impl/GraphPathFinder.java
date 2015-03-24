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

import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.algorithm.strategies.EuclideanRemainingWeightHeuristic;
import org.opentripplanner.routing.algorithm.strategies.InterleavedBidirectionalHeuristic;
import org.opentripplanner.routing.algorithm.strategies.RemainingWeightHeuristic;
import org.opentripplanner.routing.algorithm.strategies.TrivialRemainingWeightHeuristic;
import org.opentripplanner.routing.automata.DFA;
import org.opentripplanner.routing.automata.Nonterminal;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.*;
import org.opentripplanner.routing.error.PathNotFoundException;
import org.opentripplanner.routing.error.VertexNotFoundException;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.pathparser.PathParser;
import org.opentripplanner.routing.spt.DominanceFunction;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.standalone.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * This class contains the logic for repeatedly building shortest path trees and accumulating paths through
 * the graph until the requested number of them have been found.
 * It is used in point-to-point (i.e. not one-to-many / analyst) routing.
 *
 * Its exact behavior will depend on whether the routing request allows transit.
 *
 * When using transit it will incorporate techniques from what we called "long distance" mode, which is designed to
 * provide reasonable response times when routing over large graphs (e.g. the entire Netherlands or New York State).
 * In this case it only uses the street network at the first and last legs of the trip, and all other transfers
 * between transit vehicles will occur via SimpleTransfer edges which are pre-computed by the graph builder.
 * 
 * More information is available on the OTP wiki at:
 * https://github.com/openplans/OpenTripPlanner/wiki/LargeGraphs
 *
 * One instance of this class should be constructed per search (i.e. per RoutingRequest: it is request-scoped).
 * Its behavior is undefined if it is reused for more than one search.
 *
 * It is very close to being an abstract library class with only static functions. However it turns out to be convenient
 * and harmless to have the OTPServer object etc. in fields, to avoid passing context around in function parameters.
 */
public class GraphPathFinder {

    private static final Logger LOG = LoggerFactory.getLogger(GraphPathFinder.class);
    private static final double DEFAULT_MAX_WALK = 2000;
    private static final double CLAMP_MAX_WALK = 15000;

    Router router;

    public GraphPathFinder(Router router) {
        this.router = router;
    }

    // Timeout in seconds relative to initial search begin time, for each new path found (generally decreasing)

    /**
     * Repeatedly build shortest path trees, retaining the best path to the destination after each try.
     * For search N, all trips used in itineraries retained from trips 0..(N-1) are "banned" to create variety.
     * The goal direction heuristic is reused between tries, which means the later tries have more information to
     * work with (in the case of the more sophisticated bidirectional heuristic, which improves over time).
     */
    public List<GraphPath> getPaths(RoutingRequest options) {

        if (options == null) {
            LOG.error("PathService was passed a null routing request.");
            return null;
        }

        // Reuse one instance of AStar for all N requests, which are carried out sequentially
        AStar aStar = new AStar();
        if (options.rctx == null) {
            options.setRoutingContext(router.graph);
            /* Use a pathparser that constrains the search to use SimpleTransfers. */
            options.rctx.pathParsers = new PathParser[] { new Parser() };
        }
        // If this Router has a GraphVisualizer attached to it, set it as a callback for the AStar search
        if (router.graphVisualizer != null) {
            aStar.setTraverseVisitor(router.graphVisualizer.traverseVisitor);
            // options.disableRemainingWeightHeuristic = true; // DEBUG
        }

        // without transit, we'd just just return multiple copies of the same on-street itinerary
        if (!options.modes.isTransit()) {
            options.numItineraries = 1;
        }
        options.dominanceFunction = new DominanceFunction.MinimumWeight(); // FORCING the dominance function to weight only
        LOG.debug("rreq={}", options);

        RemainingWeightHeuristic heuristic;
        if (options.disableRemainingWeightHeuristic) {
            heuristic = new TrivialRemainingWeightHeuristic();
        } else if (options.modes.isTransit()) {
           // Only use the BiDi heuristic for transit.
            heuristic = new InterleavedBidirectionalHeuristic(options.rctx.graph);
        } else {
            heuristic = new EuclideanRemainingWeightHeuristic();
        }
        // heuristic = new TrivialRemainingWeightHeuristic(); // DEBUG

        options.rctx.remainingWeightHeuristic = heuristic;
        /* In RoutingRequest, maxTransfers defaults to 2. Over long distances, we may see 
         * itineraries with far more transfers. We do not expect transfer limiting to improve
         * search times on the LongDistancePathService, so we set it to the maximum we ever expect
         * to see. Because people may use either the traditional path services or the 
         * LongDistancePathService, we do not change the global default but override it here. */
        options.maxTransfers = 4;
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
        while (paths.size() < options.numItineraries) {
            // TODO pull all this timeout logic into a function near org.opentripplanner.util.DateUtils.absoluteTimeout()
            int timeoutIndex = paths.size();
            if (timeoutIndex >= router.timeouts.length) {
                timeoutIndex = router.timeouts.length - 1;
            }
            double timeout = searchBeginTime + (router.timeouts[timeoutIndex] * 1000);
            timeout -= System.currentTimeMillis(); // absolute to relative
            timeout /= 1000; // msec to seconds
            if (timeout <= 0) {
                // must catch this case where advancing to the next (lower) timeout value means the search is timed out
                // before it even begins, because a negative relative timeout will mean "no timeout" in the SPT call.
                options.rctx.aborted = true;
                break;
            }
            ShortestPathTree spt = aStar.getShortestPathTree(options, timeout);
            if (spt == null) {
                LOG.warn("SPT was null."); // unknown failure
                return null;
            }
            if (options.rctx.aborted) {
                break; // search timed out or was gracefully aborted for some other reason.
            }
            List<GraphPath> newPaths = spt.getPaths();
            if (newPaths.isEmpty()) {
                break;
            }
            // Find all trips used in this path and ban them for the remaining searches
            for (GraphPath path : newPaths) {
                for (State state : path.states) {
                    AgencyAndId tripId = state.getTripId();
                    if (tripId != null) options.banTrip(tripId);
                }
            }
            paths.addAll(newPaths);
            LOG.debug("we have {} paths", paths.size());
        }
        LOG.debug("END SEARCH ({} msec)", System.currentTimeMillis() - searchBeginTime);
        Collections.sort(paths, new PathWeightComparator());
        return paths;
    }

    /* TODO eliminate the need for pathparsers. They are theoretically efficient but arcane and problematic. */

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

    /* Try to find N paths through the Graph */
    public List<GraphPath> graphPathFinderEntryPoint (RoutingRequest request) {

        // We used to perform a protective clone of the RoutingRequest here.
        // There is no reason to do this if we don't modify the request.
        // Any code that changes them should be performing the copy!

        List<GraphPath> paths = null;
        try {
            paths = getGraphPathsConsideringIntermediates(request);
            if (paths == null && request.wheelchairAccessible) {
                // There are no paths that meet the user's slope restrictions.
                // Try again without slope restrictions, and warn the user in the response.
                RoutingRequest relaxedRequest = request.clone();
                relaxedRequest.maxSlope = Double.MAX_VALUE;
                request.rctx.slopeRestrictionRemoved = true;
                paths = getGraphPathsConsideringIntermediates(relaxedRequest);
            }
            request.rctx.debugOutput.finishedCalculating();
        } catch (VertexNotFoundException e) {
            LOG.info("Vertex not found: " + request.from + " : " + request.to);
            throw e;
        }

        if (paths == null || paths.size() == 0) {
            LOG.debug("Path not found: " + request.from + " : " + request.to);
            request.rctx.debugOutput.finishedRendering(); // make sure we still report full search time
            throw new PathNotFoundException();
        }

        /* Detect and report that most obnoxious of bugs: path reversal asymmetry. */
        Iterator<GraphPath> gpi = paths.iterator();
        while (gpi.hasNext()) {
            GraphPath graphPath = gpi.next();
            // TODO check, is it possible that arriveBy and time are modifed in-place by the search?
            if (request.arriveBy) {
                if (graphPath.states.getLast().getTimeSeconds() > request.dateTime) {
                    LOG.error("A graph path arrives after the requested time. This implies a bug.");
                    gpi.remove();
                }
            } else {
                if (graphPath.states.getFirst().getTimeSeconds() < request.dateTime) {
                    LOG.error("A graph path leaves before the requested time. This implies a bug.");
                    gpi.remove();
                }
            }
        }
        return paths;
    }

    /**
     * Break up a RoutingRequest with intermediate places into separate requests, in the given order.
     * If there are no intermediate places, issue a single request.
     */
    private List<GraphPath> getGraphPathsConsideringIntermediates (RoutingRequest request) {
        if (request.hasIntermediatePlaces()) {
            long time = request.dateTime;
            GenericLocation from = request.from;
            List<GenericLocation> places = Lists.newLinkedList(request.intermediatePlaces);
            places.add(request.to);
            request.clearIntermediatePlaces();
            List<GraphPath> paths = new ArrayList<>();

            for (GenericLocation to : places) {
                request.dateTime = time;
                request.from = from;
                request.to = to;
                request.rctx = null;
                request.setRoutingContext(router.graph);
                // TODO request only one itinerary here

                List<GraphPath> partialPaths = getPaths(request);
                if (partialPaths == null || partialPaths.size() == 0) {
                    return null;
                }

                GraphPath path = partialPaths.get(0);
                paths.add(path);
                from = to;
                time = path.getEndTime();
            }

            return Arrays.asList(joinPaths(paths));
        } else {
            return getPaths(request);
        }
    }

    private static GraphPath joinPaths(List<GraphPath> paths) {
        State lastState = paths.get(0).states.getLast();
        GraphPath newPath = new GraphPath(lastState, false);
        Vertex lastVertex = lastState.getVertex();
        for (GraphPath path : paths.subList(1, paths.size())) {
            lastState = newPath.states.getLast();
            // add a leg-switching state
            LegSwitchingEdge legSwitchingEdge = new LegSwitchingEdge(lastVertex, lastVertex);
            lastState = legSwitchingEdge.traverse(lastState);
            newPath.edges.add(legSwitchingEdge);
            newPath.states.add(lastState);
            // add the next subpath
            for (Edge e : path.edges) {
                lastState = e.traverse(lastState);
                newPath.edges.add(e);
                newPath.states.add(lastState);
            }
            lastVertex = path.getEndVertex();
        }
        return newPath;
    }

/*
    TODO reimplement
    This should probably be done with a special value in the departure/arrival time.

    public static TripPlan generateFirstTrip(RoutingRequest request) {
        request.setArriveBy(false);

        TimeZone tz = graph.getTimeZone();

        GregorianCalendar calendar = new GregorianCalendar(tz);
        calendar.setTimeInMillis(request.dateTime * 1000);
        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.AM_PM, 0);
        calendar.set(Calendar.SECOND, graph.index.overnightBreak);

        request.dateTime = calendar.getTimeInMillis() / 1000;
        return generate(request);
    }

    public static TripPlan generateLastTrip(RoutingRequest request) {
        request.setArriveBy(true);

        TimeZone tz = graph.getTimeZone();

        GregorianCalendar calendar = new GregorianCalendar(tz);
        calendar.setTimeInMillis(request.dateTime * 1000);
        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.AM_PM, 0);
        calendar.set(Calendar.SECOND, graph.index.overnightBreak);
        calendar.add(Calendar.DAY_OF_YEAR, 1);

        request.dateTime = calendar.getTimeInMillis() / 1000;

        return generate(request);
    }
*/

}
