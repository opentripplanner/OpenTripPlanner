package org.opentripplanner.profile;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import org.joda.time.DateTimeZone;
import org.opentripplanner.analyst.SampleSet;
import org.opentripplanner.analyst.TimeSurface;
import org.opentripplanner.analyst.cluster.ResultEnvelope;
import org.opentripplanner.analyst.cluster.TaskStatistics;
import org.opentripplanner.analyst.scenario.AddTripPattern;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.DominanceFunction;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.util.Arrays;

/**
 * Perform one-to-many profile routing using repeated RAPTOR searches. In this context, profile routing means finding
 * the optimal itinerary for each departure moment in a given window, without trying to reconstruct the exact paths.
 *
 * In other contexts (Modeify-style point to point routing) we would want to include suboptimal but resonable paths
 * and retain enough information to reconstruct all those paths accounting for common trunk frequencies and stop clusters.
 *
 * This method is conceptually very similar to the work of the Minnesota Accessibility Observatory
 * (http://www.its.umn.edu/Publications/ResearchReports/pdfdownloadl.pl?id=2504)
 * They run repeated searches for each departure time in the window. We take advantage of the fact that the street
 * network is static (for the most part, assuming time-dependent turn restrictions and traffic are consistent across
 * the time window) and only run a fast transit search for each minute in the window.
 */
public class RepeatedRaptorProfileRouter {

    private static final Logger LOG = LoggerFactory.getLogger(RepeatedRaptorProfileRouter.class);

    public static final int MAX_DURATION = 10 * 60 * 2; // seconds

    public ProfileRequest request;

    public Graph graph;

    // The spacing in minutes between RAPTOR calls within the time window
    public int stepMinutes = 1;

    /** Three time surfaces for min, max, and average travel time over the given time window. */
    public TimeSurface.RangeSet timeSurfaceRangeSet;

    /** If not null, completely skip this agency during the calculations. */
    public String banAgency = null;

    /**
     * If this is null we will generate a throw-away raptor data table. If it is set, the provided table will be used
     * for routing. Ideally we'd handle the cacheing of reusable raptor tables inside this class, but this class is
     * a throw-away calculator instance and doesn't have access to the job IDs which would be the cache keys.
     */
    public RaptorWorkerData raptorWorkerData;

    private ShortestPathTree preTransitSpt;

    /** The sum of all earliest-arrival travel times to a given transit stop. Will be divided to create an average. */
    TObjectLongMap<TransitStop> accumulator = new TObjectLongHashMap<TransitStop>();

    /** The number of travel time observations added into the accumulator. The divisor when calculating an average. */
    TObjectIntMap<TransitStop> counts = new TObjectIntHashMap<TransitStop>();

    /** Samples to propagate times to */
    private SampleSet sampleSet;

    private PropagatedTimesStore propagatedTimesStore;

    // Set this field to an existing taskStatistics before routing if you want to collect performance information.
    public TaskStatistics ts = new TaskStatistics();

    // Set this field to true before routing if you want the full travel times included in your response.
    public boolean includeTimes = false;

    /**
     * Make a router to use for making time surfaces only.
     *
     * If you're building ResultSets, you should use the below method that uses a SampleSet;
     * otherwise you maximum and average may not be correct.
     *
     * If you want isochrones, you should use this method, or use a sampleset that is very fine. The
     * reason for this is that isochrones are interpolated from these points in Euclidean space, so the
     * points need to be very fine. Consider the case of three roads in an equilateral triangle, the edges
     * of which each take ten minutes to traverse. If you're standing at one vertex and want to go
     * halfway down the opposite edge, it is a fifteen minute trip. However, interpolating between the
     * vertices will not give fifteen minutes, it will give ten. The less granular your representation
     * is, the worse this problem will be.
     * TODO merge this with the 3-arg constructor, and just specify what happens when you have a null pointset.
     * This should allow us to get rid of some conditionals in the calling code.
     */
    @Deprecated // just call the one below with null
    public RepeatedRaptorProfileRouter(Graph graph, ProfileRequest request) {
        this(graph, request, null);
    }

    /**
     * Make a router to use for making ResultSets. This propagates times all the way to the samples, so that
     * average and maximum travel time are correct.
     * 
     * Samples are linked to two vertices at the ends of an edge, and it is possible that the average for the sample
     * (as well as the max) is lower than the average or the max at either of the vertices, because it may be that
     * every time the max at one vertex is occurring, a lower value is occurring at the other. This initially
     * seems improbable, but consider the case when there are two parallel transit services running out of phase.
     * It may be that some of the time it makes sense to go out of your house and turn left, and sometimes it makes
     * sense to turn right, depending on which is coming first.
     */
    public RepeatedRaptorProfileRouter (Graph graph, ProfileRequest request, SampleSet sampleSet) {
        this.request = request;
        this.graph = graph;
        this.sampleSet = sampleSet;
    }

    public ResultEnvelope route () {

        boolean isochrone = (sampleSet == null); // When no sample set is provided, we're making isochrones.
        boolean transit = (request.transitModes != null && request.transitModes.isTransit()); // Does the search involve transit at all?

        long computationStartTime = System.currentTimeMillis();
        LOG.info("Begin profile request");

        // Data tables may have been supplied by the caller (if they are cached). Otherwise generate a throw away one.
        // We only create data tables if transit is in use, otherwise they wouldn't serve any purpose.
        if (raptorWorkerData == null && transit) {
            long dataStart = System.currentTimeMillis();
            raptorWorkerData = getRaptorWorkerData(request, graph, sampleSet, ts);
            ts.raptorData = (int) (System.currentTimeMillis() - dataStart);
        }

        // Find the transit stops that are accessible from the origin, leaving behind an SPT behind of access
        // times to all reachable vertices.
        long initialStopStartTime = System.currentTimeMillis();
        // This will return null if we have no transit data, but will leave behind a pre-transit SPT.
        TIntIntMap transitStopAccessTimes = findInitialStops(false, raptorWorkerData);
        // Create an array containing the best travel time in seconds to each vertex in the graph when not using transit.
        int[] nonTransitTimes = new int[Vertex.getMaxIndex()];
        Arrays.fill(nonTransitTimes, Integer.MAX_VALUE);
        for (State state : preTransitSpt.getAllStates()) {
            // Note that we are using the walk distance divided by speed here in order to be consistent with the
            // least-walk optimization in the initial stop search (and the stop tree cache which is used at egress)
            // TODO consider why this matters, I'm using reported travel time from the states
            int time = (int) state.getElapsedTimeSeconds();
            int vidx = state.getVertex().getIndex();
            int otime = nonTransitTimes[vidx];
            // There may be dominated states in the SPT. Make sure we don't include them here.
            if (otime > time) {
                nonTransitTimes[vidx] = time;
            }
        }
        ts.initialStopSearch = (int) (System.currentTimeMillis() - initialStopStartTime);

        long walkSearchStart = System.currentTimeMillis(); // FIXME wasn't the walk search already performed above?

        // At this point we have an array of the travel times in seconds to each vertex in the graph without transit.
        // In the event that a pointset was supplied, our real targets are the points in the pointset, not the vertices
        // in the graph. Therefore we must replace the vertex-indexed array with a new point-indexed array.
        if (sampleSet != null) {
            nonTransitTimes = sampleSet.eval(nonTransitTimes);
        }
        ts.walkSearch = (int) (System.currentTimeMillis() - walkSearchStart);

        if (transit) {
            RaptorWorker worker = new RaptorWorker(raptorWorkerData, request);
            propagatedTimesStore = worker.runRaptor(graph, transitStopAccessTimes, nonTransitTimes, ts);
            ts.initialStopCount = transitStopAccessTimes.size();
        } else {
            // Nontransit case: skip transit routing and make a propagated times store based on only one row.
            propagatedTimesStore = new PropagatedTimesStore(graph, request, nonTransitTimes.length);
            int[][] singleRoundResults = new int[1][];
            singleRoundResults[0] = nonTransitTimes;
            propagatedTimesStore.setFromArray(singleRoundResults, new boolean[] {true},
                    PropagatedTimesStore.ConfidenceCalculationMethod.MIN_MAX);
        }
        for (int min : propagatedTimesStore.mins) {
            if (min != RaptorWorker.UNREACHED) ts.targetsReached++;
        }
        ts.compute = (int) (System.currentTimeMillis() - computationStartTime);
        LOG.info("Profile request finished in {} seconds", (ts.compute) / 1000.0);

        // Turn the results of the search into isochrone geometries or accessibility data as requested.
        long resultSetStart = System.currentTimeMillis();
        ResultEnvelope envelope = new ResultEnvelope();
        if (isochrone) {
            // No destination point set was provided and we're just making isochrones based on travel time to vertices,
            // rather than finding access times to a set of user-specified points.
            envelope = propagatedTimesStore.makeIsochronesForVertices();
        } else {
            // A destination point set was provided. We've found access times to a set of specified points.
            // TODO actually use those boolean params to calculate isochrones on a regular grid pointset
            // TODO maybe there's a better way to pass includeTimes in here from the clusterRequest,
            // maybe we should just provide the whole clusterRequest not just the wrapped profileRequest.
            envelope = propagatedTimesStore.makeResults(sampleSet, includeTimes, true, false);
        }
        ts.resultSets = (int) (System.currentTimeMillis() - resultSetStart);
        return envelope;
    }

    /**
     * Find all transit stops accessible by streets around the origin, leaving behind a shortest path tree of the
     * reachable area in the field preTransitSpt.
     *
     * @param data the raptor data table to use. If this is null (i.e. there is no transit) range is extended,
     *             and we don't care if we actually find any stops, we just want the tree of on-street distances.
     */
    @VisibleForTesting
    public TIntIntMap findInitialStops(boolean dest, RaptorWorkerData data) {
        LOG.info("Finding initial stops");
        double lat = dest ? request.toLat : request.fromLat;
        double lon = dest ? request.toLon : request.fromLon;
        QualifiedModeSet modes = dest ? request.egressModes : request.accessModes;

        RoutingRequest rr = new RoutingRequest(modes);
        rr.batch = true;
        rr.from = new GenericLocation(lat, lon);
        //rr.walkSpeed = request.walkSpeed;
        rr.to = rr.from;
        rr.setRoutingContext(graph);
        rr.dateTime = request.date.toDateMidnight(DateTimeZone.forTimeZone(graph.getTimeZone())).getMillis() / 1000 +
                request.fromTime;
        rr.walkSpeed = request.walkSpeed;
        rr.bikeSpeed = request.bikeSpeed;

        //rr.modes.setWalk(true);

        if (data == null) {
            // Non-transit mode. Search out to the full 120 minutes.
            // Should really use directModes.
            rr.worstTime = rr.dateTime + RaptorWorker.MAX_DURATION;
            rr.dominanceFunction = new DominanceFunction.EarliestArrival();
        } else {
            // Transit mode, limit pre-transit travel.
            if (rr.modes.contains(TraverseMode.BICYCLE)) {
                rr.dominanceFunction = new DominanceFunction.EarliestArrival();
                rr.worstTime = rr.dateTime + request.maxBikeTime * 60;
            } else {
                // We use walk-distance limiting and a least-walk dominance function in order to be consistent with egress walking
                // which is implemented this way because walk times can change when walk speed changes. Also, walk times are floating
                // point and can change slightly when streets are split. Street lengths are internally fixed-point ints, which do not
                // suffer from roundoff. Great care is taken when splitting to preserve sums.
                // When cycling, this is not an issue; we already have an explicitly asymmetrical search (cycling at the origin, walking at the destination),
                // so we need not preserve symmetry.
                // We use the max walk time for the search at the origin, but we clamp it to MAX_WALK_METERS so that we don;t
                // have every transit stop in the state as an initial transit stop if someone sets max walk time to four days,
                // and so that symmetry is preserved.
                rr.maxWalkDistance = Math.min(request.maxWalkTime * 60 * request.walkSpeed, GraphIndex.MAX_WALK_METERS); // FIXME kind of arbitrary
                rr.softWalkLimiting = false;
                rr.dominanceFunction = new DominanceFunction.LeastWalk();
            }
        }

        rr.numItineraries = 1;
        rr.longDistance = true;

        AStar aStar = new AStar();
        preTransitSpt = aStar.getShortestPathTree(rr, 5);

        // Return nearest stops if we're using transit,
        // otherwise return null and leave preTransitSpt around for later use.
        if (data != null) {
            TIntIntMap accessTimes = data.findStopsNear(preTransitSpt, graph, rr.modes.contains(TraverseMode.BICYCLE), request.walkSpeed);
            LOG.info("Found {} transit stops", accessTimes.size());
            return accessTimes;
        } else {
            return null;
        }
    }

    /** Create RAPTOR worker data from a graph, profile request and sample set (the last of which may be null */
    public static RaptorWorkerData getRaptorWorkerData (ProfileRequest request, Graph graph, SampleSet sampleSet, TaskStatistics ts) {
        LOG.info("Make data...");
        long startData = System.currentTimeMillis();

        // assign indices for added transit stops
        // note that they only need be unique in the context of this search.
        // note also that there may be, before this search is over, vertices with higher indices (temp vertices)
        // but they will not be transit stops.
        if (request.scenario != null && request.scenario.modifications != null) {
            for (AddTripPattern atp : Iterables
                    .filter(request.scenario.modifications, AddTripPattern.class)) {
                atp.materialize(graph);
            }
        }

        // convert from joda to java - ISO day of week with monday == 1
        DayOfWeek dayOfWeek = DayOfWeek.of(request.date.getDayOfWeek());

        TimeWindow window = new TimeWindow(request.fromTime, request.toTime + RaptorWorker.MAX_DURATION,
                graph.index.servicesRunning(request.date), dayOfWeek);

        RaptorWorkerData raptorWorkerData;
        if (sampleSet == null)
            raptorWorkerData = new RaptorWorkerData(graph, window, request, ts);
        else
            raptorWorkerData = new RaptorWorkerData(graph, window, request, sampleSet,
                    ts);

        ts.raptorData = (int) (System.currentTimeMillis() - startData);

        LOG.info("done");

        return raptorWorkerData;
    }
}
