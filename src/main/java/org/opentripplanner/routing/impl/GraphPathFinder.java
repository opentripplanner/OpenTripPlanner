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
import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.algorithm.strategies.EuclideanRemainingWeightHeuristic;
import org.opentripplanner.routing.algorithm.strategies.InterleavedBidirectionalHeuristic;
import org.opentripplanner.routing.algorithm.strategies.RemainingWeightHeuristic;
import org.opentripplanner.routing.algorithm.strategies.TrivialRemainingWeightHeuristic;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.LegSwitchingEdge;
import org.opentripplanner.routing.error.PathNotFoundException;
import org.opentripplanner.routing.error.VertexNotFoundException;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.DominanceFunction;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.standalone.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

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
            // The special long-distance heuristic should be sufficient to constrain the search to the right area.
        }
        // If this Router has a GraphVisualizer attached to it, set it as a callback for the AStar search
        if (router.graphVisualizer != null) {
            aStar.setTraverseVisitor(router.graphVisualizer.traverseVisitor);
            // options.disableRemainingWeightHeuristic = true; // DEBUG
        }

        // Without transit, we'd just just return multiple copies of the same on-street itinerary.
        if (!options.modes.isTransit()) {
            options.numItineraries = 1;
        }
        options.dominanceFunction = new DominanceFunction.MinimumWeight(); // FORCING the dominance function to weight only
        LOG.debug("rreq={}", options);

        // Choose an appropriate heuristic for goal direction.
        RemainingWeightHeuristic heuristic;
        if (options.disableRemainingWeightHeuristic) {
            heuristic = new TrivialRemainingWeightHeuristic();
        } else if (options.modes.isTransit()) {
            // Only use the BiDi heuristic for transit. It is not very useful for on-street modes.
            // heuristic = new InterleavedBidirectionalHeuristic(options.rctx.graph);
            // Use a simplistic heuristic until BiDi heuristic is improved, see #2153
            heuristic = new InterleavedBidirectionalHeuristic();
        } else {
            heuristic = new EuclideanRemainingWeightHeuristic();
        }
        options.rctx.remainingWeightHeuristic = heuristic;

        /* In RoutingRequest, maxTransfers defaults to 2. Over long distances, we may see
         * itineraries with far more transfers. We do not expect transfer limiting to improve
         * search times on the LongDistancePathService, so we set it to the maximum we ever expect
         * to see. Because people may use either the traditional path services or the 
         * LongDistancePathService, we do not change the global default but override it here. */
        options.maxTransfers = 4;
        // Now we always use what used to be called longDistance mode. Non-longDistance mode is no longer supported.
        options.longDistance = true;

        /* In long distance mode, maxWalk has a different meaning than it used to.
         * It's the radius around the origin or destination within which you can walk on the streets.
         * If no value is provided, max walk defaults to the largest double-precision float.
         * This would cause long distance mode to do unbounded street searches and consider the whole graph walkable. */
        if (options.maxWalkDistance == Double.MAX_VALUE) options.maxWalkDistance = DEFAULT_MAX_WALK;
        if (options.maxWalkDistance > CLAMP_MAX_WALK) options.maxWalkDistance = CLAMP_MAX_WALK;
        long searchBeginTime = System.currentTimeMillis();
        LOG.debug("BEGIN SEARCH");
        List<GraphPath> paths = Lists.newArrayList();
        while (paths.size() < options.numItineraries) {
            // TODO pull all this timeout logic into a function near org.opentripplanner.util.DateUtils.absoluteTimeout()
            int timeoutIndex = paths.size();
            if (timeoutIndex >= router.timeouts.length) {
                timeoutIndex = router.timeouts.length - 1;
            }
            double timeout = searchBeginTime + (router.timeouts[timeoutIndex] * 1000);
            timeout -= System.currentTimeMillis(); // Convert from absolute to relative time
            timeout /= 1000; // Convert milliseconds to seconds
            if (timeout <= 0) {
                // Catch the case where advancing to the next (lower) timeout value means the search is timed out
                // before it even begins. Passing a negative relative timeout in the SPT call would mean "no timeout".
                options.rctx.aborted = true;
                break;
            }
            aStar.getShortestPathTree(options, timeout);
            if (options.rctx.aborted) {
                break; // Search timed out or was gracefully aborted for some other reason.
            }
            // Don't dig through the SPT object, just ask the A star algorithm for the states that reached the target.
            List<GraphPath> newPaths = aStar.getPathsToTarget();
            if (newPaths.isEmpty()) {
                break;
            }
            // Find all trips used in this path and ban them for the remaining searches
            for (GraphPath path : newPaths) {
                // path.dump();
                List<AgencyAndId> tripIds = path.getTrips();
                for (AgencyAndId tripId : tripIds) {
                    options.banTrip(tripId);
                }
                if (tripIds.isEmpty()) {
                    // This path does not use transit (is entirely on-street). Do not repeatedly find the same one.
                    options.onlyTransitTrips = true;
                }
            }
            paths.addAll(newPaths);
            LOG.debug("we have {} paths", paths.size());
        }
        LOG.debug("END SEARCH ({} msec)", System.currentTimeMillis() - searchBeginTime);
        Collections.sort(paths, new PathComparator(options.arriveBy));
        return paths;
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
