package org.opentripplanner.routing.impl;

import com.google.common.collect.Lists;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.api.resource.DebugOutput;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.algorithm.strategies.EuclideanRemainingWeightHeuristic;
import org.opentripplanner.routing.algorithm.strategies.InterleavedBidirectionalHeuristic;
import org.opentripplanner.routing.algorithm.strategies.RemainingWeightHeuristic;
import org.opentripplanner.routing.algorithm.strategies.TrivialRemainingWeightHeuristic;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.LegSwitchingEdge;
import org.opentripplanner.routing.edgetype.TransitBoardAlight;
import org.opentripplanner.routing.error.PathNotFoundException;
import org.opentripplanner.routing.error.VertexNotFoundException;
import org.opentripplanner.routing.flex.DeviatedRouteGraphModifier;
import org.opentripplanner.routing.flex.FlagStopGraphModifier;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.DominanceFunction;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.standalone.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

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

        RoutingRequest originalReq = options.clone();

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
        RemainingWeightHeuristic reversedSearchHeuristic;
        if (options.disableRemainingWeightHeuristic) {
            heuristic = new TrivialRemainingWeightHeuristic();
            reversedSearchHeuristic = new TrivialRemainingWeightHeuristic();
        } else if (options.modes.isTransit()) {
            // Only use the BiDi heuristic for transit. It is not very useful for on-street modes.
            // heuristic = new InterleavedBidirectionalHeuristic(options.rctx.graph);
            // Use a simplistic heuristic until BiDi heuristic is improved, see #2153
            heuristic = new InterleavedBidirectionalHeuristic();
            reversedSearchHeuristic = new InterleavedBidirectionalHeuristic();
        } else {
            heuristic = new EuclideanRemainingWeightHeuristic();
            reversedSearchHeuristic = new EuclideanRemainingWeightHeuristic();
        }
        options.rctx.remainingWeightHeuristic = heuristic;


        /* In RoutingRequest, maxTransfers defaults to 2. But as discussed in #2522, you can't limit the number of
         * transfers in our routing algorithm. This is a resource limiting problem, like imposing a walk limit or
         * not optimizing on arrival time in a time-dependent network (both of which we have done / do but need
         * to systematically eliminate).
         */
        options.maxTransfers = 4; // should probably be Integer.MAX_VALUE;

        // OTP now always uses what used to be called longDistance mode. Non-longDistance mode is no longer supported.
        options.longDistance = true;

        /* maxWalk has a different meaning than it used to. It's the radius around the origin or destination within
         * which you can walk on the streets. An unlimited value would cause the bidi heuristic to do unbounded street
         * searches and consider the whole graph walkable.
         *
         * After the limited areas of the street network around the origin and destination are explored, the
         * options.maxWalkDistance will be set to unlimited for similar reasons to maxTransfers above. That happens
         * in method org.opentripplanner.routing.algorithm.strategies.InterleavedBidirectionalHeuristic.initialize
         */
        if (options.maxWalkDistance == Double.MAX_VALUE) options.maxWalkDistance = DEFAULT_MAX_WALK;
        if (options.maxWalkDistance > CLAMP_MAX_WALK) options.maxWalkDistance = CLAMP_MAX_WALK;
        if (options.modes.isTransit() && router.graph.useFlexService) {
            // create temporary flex stops/hops (just once even if we run multiple searches)
            FlagStopGraphModifier flagStopGraphModifier = new FlagStopGraphModifier(router.graph);
            DeviatedRouteGraphModifier deviatedRouteGraphModifier = new DeviatedRouteGraphModifier(router.graph);
            flagStopGraphModifier.createForwardHops(options);
            if (options.flexUseReservationServices) {
                deviatedRouteGraphModifier.createForwardHops(options);
            }
            flagStopGraphModifier.createBackwardHops(options);
            if (options.flexUseReservationServices) {
                deviatedRouteGraphModifier.createBackwardHops(options);
            }
        }
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
            // Don't dig through the SPT object, just ask the A star algorithm for the states that reached the target.
            aStar.getShortestPathTree(options, timeout);

            if (options.rctx.aborted) {
                break; // Search timed out or was gracefully aborted for some other reason.
            }
            List<GraphPath> newPaths = aStar.getPathsToTarget();
            if (newPaths.isEmpty()) {
                break;
            }

            // Do a full reversed search to compact the legs
            if(options.compactLegsByReversedSearch){
                newPaths = compactLegsByReversedSearch(aStar, originalReq, options, newPaths, timeout, reversedSearchHeuristic);
            }

            // Find all trips used in this path and ban them for the remaining searches
            for (GraphPath path : newPaths) {
                // path.dump();
                List<FeedScopedId> tripIds = path.getTrips();
                List<FeedScopedId> callAndRideTripIds = path.getCallAndRideTrips();
                for (FeedScopedId tripId : tripIds) {
                    if (!callAndRideTripIds.contains(tripId)) {
                        options.banTrip(tripId);
                    }
                }
                if (tripIds.isEmpty()) {
                    // This path does not use transit (is entirely on-street). Do not repeatedly find the same one.
                    options.onlyTransitTrips = true;
                }
                // Call-and-Ride trips should not use regular trip-banning, since call-and-ride trips can beused in
                // multiple ways (e.g. from origin to destination, or from origin to a transfer stop.) Instead,
                // after an itinerary which uses call-and-ride is found, reduce the allowable call-and-ride duration
                // so that the same leg cannot be found in a subsequent search.
                if (tripIds.size() < 2) {
                    int duration = path.getCallAndRideDuration();
                    if (duration > 0) { // only true if there are call-and-ride legs
                        int constantLimit = Math.min(0, duration - options.flexReduceCallAndRideSeconds);
                        int ratioLimit = (int) Math.round(options.flexReduceCallAndRideRatio * duration);
                        options.flexMaxCallAndRideSeconds = Math.min(constantLimit, ratioLimit);
                    }
                }
            }

            paths.addAll(newPaths.stream()
                    .filter(path -> {
                        double duration = options.useRequestedDateTimeInMaxHours
                            ? options.arriveBy
                                ? options.dateTime - path.getStartTime()
                                : path.getEndTime() - options.dateTime
                            : path.getDuration();
                        return duration < options.maxHours * 60 * 60;
                    })
                    .collect(Collectors.toList()));

            LOG.debug("we have {} paths", paths.size());
        }
        LOG.debug("END SEARCH ({} msec)", System.currentTimeMillis() - searchBeginTime);
        Collections.sort(paths, options.getPathComparator(options.arriveBy));
        return paths;
    }

    /**
     * Do a full reversed search to compact the legs of the path.
     *
     * By doing a reversed search we are looking for later departures that will still be in time for transfer
     * to the next trip, shortening the transfer wait time. Also considering other routes than the ones found
     * in the original search.
     *
     * For arrive-by searches, we are looking to shorten transfer wait time and rather arrive earlier.
     */
    private List<GraphPath> compactLegsByReversedSearch(AStar aStar, RoutingRequest originalReq, RoutingRequest options,
                                                        List<GraphPath> newPaths, double timeout,
                                                        RemainingWeightHeuristic remainingWeightHeuristic){
        List<GraphPath> reversedPaths = new ArrayList<>();
        for(GraphPath newPath : newPaths){
            State targetAcceptedState = options.arriveBy ? newPath.states.getLast().reverse() : newPath.states.getLast();
            if(targetAcceptedState.stateData.getNumBooardings() < 2) {
                reversedPaths.add(newPath);
                continue;
            }
            final long arrDepTime = targetAcceptedState.getTimeSeconds();
            LOG.debug("Dep time: " + new Date(newPath.getStartTime() * 1000));
            LOG.debug("Arr time: " + new Date(newPath.getEndTime() * 1000));

            // find first/last transit stop
            Vertex transitStop = null;
            long transitStopTime = arrDepTime;
            while (transitStop == null) {
                if(targetAcceptedState.backEdge instanceof TransitBoardAlight){
                    if(options.arriveBy){
                        transitStop = targetAcceptedState.backEdge.getFromVertex();
                    }else{
                        transitStop = targetAcceptedState.backEdge.getToVertex();
                    }
                    transitStopTime = targetAcceptedState.getTimeSeconds();
                }
                targetAcceptedState = targetAcceptedState.getBackState();
            }

            // find the path from transitStop to origin/destination
            Vertex fromVertex = options.arriveBy ? options.rctx.fromVertex : transitStop;
            Vertex toVertex = options.arriveBy ? transitStop : options.rctx.toVertex;
            RoutingRequest reversedTransitRequest = createReversedTransitRequest(originalReq, options, fromVertex, toVertex,
                    arrDepTime, new EuclideanRemainingWeightHeuristic());
            aStar.getShortestPathTree(reversedTransitRequest, timeout);
            List<GraphPath> pathsToTarget = aStar.getPathsToTarget();
            if(pathsToTarget.isEmpty()){
                reversedPaths.add(newPath);
                continue;
            }
            GraphPath walkPath = pathsToTarget.get(0);

            // do the reversed search to/from transitStop
            Vertex fromTransVertex = options.arriveBy ? transitStop : options.rctx.fromVertex;
            Vertex toTransVertex = options.arriveBy ? options.rctx.toVertex: transitStop;
            RoutingRequest reversedMainRequest = createReversedMainRequest(originalReq, options, fromTransVertex,
                    toTransVertex, transitStopTime, remainingWeightHeuristic);
            aStar.getShortestPathTree(reversedMainRequest, timeout);

            List<GraphPath> newRevPaths = aStar.getPathsToTarget();
            if (newRevPaths.isEmpty()) {
                reversedPaths.add(newPath);
            }else{
                List<GraphPath> joinedPaths = new ArrayList<>();
                for(GraphPath newRevPath : newRevPaths){
                    LOG.debug("REV Dep time: " + new Date(newRevPath.getStartTime() * 1000));
                    LOG.debug("REV Arr time: " + new Date(newRevPath.getEndTime() * 1000));
                    List<GraphPath> concatenatedPaths = Arrays.asList(newRevPath, walkPath);
                    if(options.arriveBy){
                        Collections.reverse(concatenatedPaths);
                    }
                    GraphPath joinedPath = joinPaths(concatenatedPaths);

                    if((!options.arriveBy && joinedPath.states.getFirst().getTimeInMillis() > options.dateTime * 1000) ||
                            (options.arriveBy && joinedPath.states.getLast().getTimeInMillis() < options.dateTime * 1000)){
                        joinedPaths.add(joinedPath);
                        if(newPaths.size() > 1){
                            for (FeedScopedId tripId : joinedPath.getTrips()) {
                                options.banTrip(tripId);
                            }
                        }
                    }
                }
                reversedPaths.addAll(joinedPaths);
            }
        }
        return reversedPaths.isEmpty() ? newPaths : reversedPaths;
    }



    private RoutingRequest createReversedTransitRequest(RoutingRequest originalReq, RoutingRequest options, Vertex fromVertex,
                                                 Vertex toVertex, long arrDepTime, RemainingWeightHeuristic remainingWeightHeuristic){

        RoutingRequest request = createReversedRequest(originalReq, options, fromVertex, toVertex,
                arrDepTime, new EuclideanRemainingWeightHeuristic());
        if((originalReq.parkAndRide || originalReq.kissAndRide) && !originalReq.arriveBy){
            request.parkAndRide = false;
            request.kissAndRide = false;
            request.modes.setCar(false);
        }
        request.maxWalkDistance = CLAMP_MAX_WALK;
        return request;
    }

    private RoutingRequest createReversedMainRequest(RoutingRequest originalReq, RoutingRequest options, Vertex fromVertex,
                                                        Vertex toVertex, long dateTime, RemainingWeightHeuristic remainingWeightHeuristic){
        RoutingRequest request = createReversedRequest(originalReq, options, fromVertex,
                toVertex, dateTime, remainingWeightHeuristic);
        if((originalReq.parkAndRide || originalReq.kissAndRide) && originalReq.arriveBy){
            request.parkAndRide = false;
            request.kissAndRide = false;
            request.modes.setCar(false);
        }
        return request;

    }

    private RoutingRequest createReversedRequest(RoutingRequest originalReq, RoutingRequest options, Vertex fromVertex,
                                                 Vertex toVertex, long dateTime, RemainingWeightHeuristic remainingWeightHeuristic){
        RoutingRequest reversedOptions = originalReq.clone();
        reversedOptions.dateTime = dateTime;
        reversedOptions.setArriveBy(!originalReq.arriveBy);
        reversedOptions.setRoutingContext(router.graph, fromVertex, toVertex);
        reversedOptions.dominanceFunction = new DominanceFunction.MinimumWeight();
        reversedOptions.rctx.remainingWeightHeuristic = remainingWeightHeuristic;
        reversedOptions.maxTransfers = 4;
        reversedOptions.longDistance = true;
        reversedOptions.bannedTrips = options.bannedTrips;
        return reversedOptions;
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

        // Detect and report that most obnoxious of bugs: path reversal asymmetry.
        // Removing paths might result in an empty list, so do this check before the empty list check.
        if (paths != null) {
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
        }

        if (paths == null || paths.size() == 0) {
            LOG.debug("Path not found: " + request.from + " : " + request.to);
            request.rctx.debugOutput.finishedRendering(); // make sure we still report full search time
            throw new PathNotFoundException();
        }

        return paths;
    }

    /**
     * Break up a RoutingRequest with intermediate places into separate requests, in the given order.
     *
     * If there are no intermediate places, issue a single request. Otherwise process the places
     * list [from, i1, i2, ..., to] either from left to right (if {@code request.arriveBy==false})
     * or from right to left (if {@code request.arriveBy==true}). In the latter case the order of
     * the requested subpaths is (i2, to), (i1, i2), and (from, i1) which has to be reversed at
     * the end.
     */
    private List<GraphPath> getGraphPathsConsideringIntermediates (RoutingRequest request) {
        Collection<Vertex> temporaryVertices = new ArrayList<>();
        if (request.hasIntermediatePlaces()) {
            List<GenericLocation> places = Lists.newArrayList(request.from);
            places.addAll(request.intermediatePlaces);
            places.add(request.to);
            long time = request.dateTime;

            List<GraphPath> paths = new ArrayList<>();
            DebugOutput debugOutput = null;
            int placeIndex = (request.arriveBy ? places.size() - 1 : 1);

            while (0 < placeIndex && placeIndex < places.size()) {
                RoutingRequest intermediateRequest = request.clone();
                intermediateRequest.setNumItineraries(1);
                intermediateRequest.dateTime = time;
                intermediateRequest.from = places.get(placeIndex - 1);
                intermediateRequest.to = places.get(placeIndex);
                intermediateRequest.rctx = null;
                intermediateRequest.setRoutingContext(router.graph, temporaryVertices);

                if (debugOutput != null) {// Restore the previous debug info accumulator
                    intermediateRequest.rctx.debugOutput = debugOutput;
                } else {// Store the debug info accumulator
                    debugOutput = intermediateRequest.rctx.debugOutput;
                }

                List<GraphPath> partialPaths = getPaths(intermediateRequest);
                if (partialPaths.size() == 0) {
                    return partialPaths;
                }

                GraphPath path = partialPaths.get(0);
                paths.add(path);
                time = (request.arriveBy ? path.getStartTime() : path.getEndTime());
                placeIndex += (request.arriveBy ? -1 : +1);
            }
            request.setRoutingContext(router.graph);
            request.rctx.debugOutput = debugOutput;
            if (request.arriveBy) {
                Collections.reverse(paths);
            }
            return Collections.singletonList(joinPaths(paths));
        } else {
            return getPaths(request);
        }
    }

    private static GraphPath joinPaths(List<GraphPath> paths) {
        State lastState = paths.get(0).states.getLast();
        GraphPath newPath = new GraphPath(lastState, false);
        Vertex lastVertex = lastState.getVertex();

        //With more paths we should allow more transfers
        lastState.getOptions().maxTransfers *= paths.size();

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
