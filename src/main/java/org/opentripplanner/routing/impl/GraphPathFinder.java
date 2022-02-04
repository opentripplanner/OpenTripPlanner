package org.opentripplanner.routing.impl;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.opentripplanner.routing.algorithm.astar.AStar;
import org.opentripplanner.routing.algorithm.astar.strategies.DurationSkipEdgeStrategy;
import org.opentripplanner.routing.algorithm.astar.strategies.EuclideanRemainingWeightHeuristic;
import org.opentripplanner.routing.algorithm.astar.strategies.RemainingWeightHeuristic;
import org.opentripplanner.routing.algorithm.astar.strategies.TrivialRemainingWeightHeuristic;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.error.PathNotFoundException;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.routing.spt.DominanceFunction;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.standalone.server.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class contains the logic for repeatedly building shortest path trees and accumulating paths through
 * the graph until the requested number of them have been found.
 * It is used in point-to-point (i.e. not one-to-many / analyst) routing.
 *
 * Its exact behavior will depend on whether the routing request allows transit.
 *
 * When using transit it will incorporate techniques from what we called "long distance" mode,
 * which is designed to provide reasonable response times when routing over large graphs (e.g.
 * the entire Netherlands or New York State). In this case it only uses the street network at the
 * first and last legs of the trip, and all other transfers between transit vehicles will occur
 * via PathTransfer edges which are pre-computed by the graph builder.
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

    Router router;

    public GraphPathFinder(Router router) {
        this.router = router;
    }

    /**
     * This no longer does "trip banning" to find multiple itineraries.
     * It just searches once trying to find a non-transit path.
     */
    public List<GraphPath> getPaths(RoutingRequest options) {
        if (options == null) {
            LOG.error("PathService was passed a null routing request.");
            return null;
        }
        if (options.streetSubRequestModes.isTransit()) {
            throw new UnsupportedOperationException("Transit search not supported");
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

        options.dominanceFunction = new DominanceFunction.MinimumWeight(); // FORCING the dominance function to weight only
        LOG.debug("rreq={}", options);

        // Choose an appropriate heuristic for goal direction.
        RemainingWeightHeuristic heuristic;
        if (options.disableRemainingWeightHeuristic || options.oneToMany) {
            heuristic = new TrivialRemainingWeightHeuristic();
        } else {
            heuristic = new EuclideanRemainingWeightHeuristic();
        }
        options.rctx.remainingWeightHeuristic = heuristic;
        
        long searchBeginTime = System.currentTimeMillis();
        LOG.debug("BEGIN SEARCH");

        double timeout = searchBeginTime + router.streetRoutingTimeoutSeconds() * 1000;
        timeout -= System.currentTimeMillis(); // Convert from absolute to relative time
        timeout /= 1000; // Convert milliseconds to seconds
        if (timeout <= 0) {
            // Catch the case where advancing to the next (lower) timeout value means the search is timed out
            // before it even begins. Passing a negative relative timeout in the SPT call would mean "no timeout".
            options.rctx.aborted = true;
            return null;
        }
        // Don't dig through the SPT object, just ask the A star algorithm for the states that reached the target.
        // Use the maxDirectStreetDurationSeconds as the limit here, as this class is used for point-to-point routing
        aStar.setSkipEdgeStrategy(new DurationSkipEdgeStrategy(options.maxDirectStreetDurationSeconds));
        aStar.getShortestPathTree(options, timeout, null);

        List<GraphPath> paths = aStar.getPathsToTarget();

        LOG.debug("we have {} paths", paths.size());
        LOG.debug("END SEARCH ({} msec)", System.currentTimeMillis() - searchBeginTime);
        Collections.sort(paths, options.getPathComparator(options.arriveBy));
        return paths;
    }

    /**
     *  Try to find N paths through the Graph
     */
    public List<GraphPath> graphPathFinderEntryPoint (RoutingRequest request) {
        long reqTime = request.getDateTime().getEpochSecond();

        // We used to perform a protective clone of the RoutingRequest here.
        // There is no reason to do this if we don't modify the request.
        // Any code that changes them should be performing the copy!

        List<GraphPath> paths;
        try {
            paths = getPaths(request);
            if (paths == null && request.wheelchairAccessible) {
                // There are no paths that meet the user's slope restrictions.
                // Try again without slope restrictions, and warn the user in the response.
                RoutingRequest relaxedRequest = request.clone();
                relaxedRequest.maxWheelchairSlope = Double.MAX_VALUE;
                request.rctx.slopeRestrictionRemoved = true;
                paths = getPaths(relaxedRequest);
            }
        } catch (RoutingValidationException e) {
            if (e.getRoutingErrors().get(0).code.equals(RoutingErrorCode.LOCATION_NOT_FOUND))
            LOG.info("Vertex not found: " + request.from + " : " + request.to);
            throw e;
        }

        // Detect and report that most obnoxious of bugs: path reversal asymmetry.
        // Removing paths might result in an empty list, so do this check before the empty list check.
        if (paths != null) {
            Iterator<GraphPath> gpi = paths.iterator();
            while (gpi.hasNext()) {
                GraphPath graphPath = gpi.next();
                // TODO check, is it possible that arriveBy and time are modifed in-place by the search?
                if (request.arriveBy) {
                    if (graphPath.states.getLast().getTimeSeconds() > reqTime) {
                        LOG.error("A graph path arrives after the requested time. This implies a bug.");
                        gpi.remove();
                    }
                } else {
                    if (graphPath.states.getFirst().getTimeSeconds() < reqTime) {
                        LOG.error("A graph path leaves before the requested time. This implies a bug.");
                        gpi.remove();
                    }
                }
            }
        }

        if (paths == null || paths.size() == 0) {
            LOG.debug("Path not found: " + request.from + " : " + request.to);
            throw new PathNotFoundException();
        }

        return paths;
    }
}
