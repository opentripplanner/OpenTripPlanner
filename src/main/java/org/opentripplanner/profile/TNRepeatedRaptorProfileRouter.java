package org.opentripplanner.profile;

import com.google.common.collect.Iterables;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.opentripplanner.analyst.SampleSet;
import org.opentripplanner.analyst.cluster.AnalystClusterRequest;
import org.opentripplanner.analyst.cluster.ResultEnvelope;
import org.opentripplanner.analyst.cluster.TaskStatistics;
import org.opentripplanner.analyst.scenario.AddTripPattern;
import org.opentripplanner.profile.TNPropagatedTimesStore.ConfidenceCalculationMethod;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.streets.PointSetTimes;
import org.opentripplanner.streets.StreetRouter;
import org.opentripplanner.streets.LinkedPointSet;
import org.opentripplanner.transit.TransportNetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;

/**
 * This is an exact copy of RepeatedRaptorProfileRouter that's being modified to work with (new) TransitNetworks
 * instead of (old) Graphs. We can afford the maintainability nightmare of duplicating so much code because this is
 * intended to completely replace the old class sooner than later.
 *
 * We don't need to wait for point-to-point routing and detailed walking directions etc. to be available on the new
 * TransitNetwork code to do analysis work with it.
 *
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
public class TNRepeatedRaptorProfileRouter {

    private static final Logger LOG = LoggerFactory.getLogger(TNRepeatedRaptorProfileRouter.class);

    public AnalystClusterRequest clusterRequest;

    public ProfileRequest request;

    public TransportNetwork network;

    /**
     * If this is null we will generate a throw-away raptor data table. If it is set, the provided table will be used
     * for routing. Ideally we'd handle the cacheing of reusable raptor tables inside this class, but this class is
     * a throw-away calculator instance and doesn't have access to the job IDs which would be the cache keys.
     */
    public RaptorWorkerData raptorWorkerData;

    /** The number of travel time observations added into the accumulator. The divisor when calculating an average. */
    TObjectIntMap<TransitStop> counts = new TObjectIntHashMap<TransitStop>();

    /** Samples to propagate times to */
    private LinkedPointSet targets;

    private TNPropagatedTimesStore propagatedTimesStore;

    // Set this field to an existing taskStatistics before routing if you want to collect performance information.
    public TaskStatistics ts = new TaskStatistics();

    /**
     * Make a router to use for making ResultEnvelopes. This propagates travel times all the way to the target temporary
     * street vertices, so that average and maximum travel time are correct.
     *
     * Temp vertices are linked to two vertices at the ends of an edge, and it is possible that the average for the sample
     * (as well as the max) is lower than the average or the max at either of the vertices, because it may be that
     * every time the max at one vertex is occurring, a lower value is occurring at the other. This initially
     * seems improbable, but consider the case when there are two parallel transit services running out of phase.
     * It may be that some of the time it makes sense to go out of your house and turn left, and sometimes it makes
     * sense to turn right, depending on which is coming first.
     */
    public TNRepeatedRaptorProfileRouter(TransportNetwork network, AnalystClusterRequest clusterRequest,
                                         LinkedPointSet targets, TaskStatistics ts) {
        if (network.streetLayer != targets.streetLayer) {
            LOG.error("Transit network and target point set are not linked to the same street layer.");
        }
        this.clusterRequest = clusterRequest;
        this.network = network;
        this.targets = targets;
        this.request = clusterRequest.profileRequest;
        this.ts = ts;
    }

    public ResultEnvelope route () {

        long computationStartTime = System.currentTimeMillis();
        LOG.info("Beginning repeated RAPTOR profile request.");

        boolean isochrone = (targets == null); // When no sample set is provided, we're making isochrones. TODO explicit switch for this.
        boolean transit = (request.transitModes != null && request.transitModes.isTransit()); // Does the search involve transit at all?

        // Check that caller has supplied a LinkedPointSet and RaptorWorkerData when needed.
        // These are supplied by the caller because the caller maintains caches, and this router object is throw-away.
        if (targets == null) {
            throw new IllegalArgumentException("Caller must supply a LinkedPointSet.");
        }

        if (transit && raptorWorkerData == null) {
            LOG.error("Caller must supply RaptorWorkerData if transit is in use.");
            transit = false;
        }

        // WHAT WE WANT TO DO HERE:
        // - Make or get a LinkedPointSet (a regular grid if no PointSet is supplied).
        // - Use a streetRouter to explore a circular area around the origin point.
        // - Get the travel times to transit stops from the StreetRouter (A).
        // - Get the travel time to all targets in the PointSet using the StreetRouter's internal tree.
        // - Make RaptorWorkerData from the TransitLayer.
        // - Use a RepeatedRaptorProfileRouter, intialized with item A, to find travel times to all reachable transit stops.
        //   - The RepeatedRaptorProfileRouter propagates times out to the targets in the PointSet as it runs.
        // - Fetch the propagated results (which will eventually be called a PointSetTimeRange)
        // - Make a result envelope from them.

        // Get travel times to street vertices near the origin, and to initial stops if we're using transit.
        long initialStopStartTime = System.currentTimeMillis();
        StreetRouter streetRouter = new StreetRouter(network.streetLayer);
        // TODO add time and distance limits to routing, not just weight.
        // TODO apply walk and bike speeds and maxBike time.
        streetRouter.distanceLimitMeters = transit ? 2000 : 100_000; // FIXME arbitrary, and account for bike or car access mode
        streetRouter.setOrigin(request.fromLat, request.fromLon);
        streetRouter.route();
        ts.initialStopSearch = (int) (System.currentTimeMillis() - initialStopStartTime);

        // Find the travel time to every target without using any transit, based on the results in the StreetRouter.
        long walkSearchStart = System.currentTimeMillis();
        PointSetTimes nonTransitTimes = targets.eval(streetRouter::getTravelTimeToVertex);
        // According to the Javadoc we do in fact want to record elapsed time for a single eval call.
        ts.walkSearch = (int) (System.currentTimeMillis() - walkSearchStart);

        if (transit) {
            TNRaptorWorker worker = new TNRaptorWorker(raptorWorkerData, request);
            TIntIntMap transitStopAccessTimes = streetRouter.getReachedStops();
            propagatedTimesStore = worker.runRaptor(transitStopAccessTimes, nonTransitTimes, ts);
            ts.initialStopCount = transitStopAccessTimes.size();
        } else {
            // Nontransit case: skip transit routing and make a propagated times store based on only one row.
            // TODO skip the transit search inside the worker and avoid this conditional.
            propagatedTimesStore = new TNPropagatedTimesStore(nonTransitTimes.size());
            int[][] singleRoundResults = new int[][] {nonTransitTimes.travelTimes};
            propagatedTimesStore.setFromArray(singleRoundResults, ConfidenceCalculationMethod.MIN_MAX);
        }
        ts.targetsReached = propagatedTimesStore.countTargetsReached();
        ts.compute = (int) (System.currentTimeMillis() - computationStartTime);
        LOG.info("Profile request finished in {} seconds", (ts.compute) / 1000.0);

        // Turn the results of the search into isochrone geometries or accessibility data as requested.
        long resultSetStart = System.currentTimeMillis();
        ResultEnvelope envelope;
        if (isochrone) {
            // No destination point set was provided and we're just making isochrones based on travel time to vertices,
            // rather than finding access times to a set of user-specified points.
            envelope = propagatedTimesStore.makeIsochronesForVertices();
        } else {
            // A destination point set was provided. We've found access times to a set of specified points.
            // TODO actually use those boolean params to calculate isochrones on a regular grid pointset
            envelope = propagatedTimesStore.makeResults(targets.pointSet, clusterRequest.includeTimes, true, false);
        }
        ts.resultSets = (int) (System.currentTimeMillis() - resultSetStart);
        return envelope;
    }

    // TODO the below function has been replaced, but contains extra steps to set up scenario modifications. Carry those over to its replacement.
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
