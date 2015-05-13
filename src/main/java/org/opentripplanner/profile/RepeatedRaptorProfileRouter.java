package org.opentripplanner.profile;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import org.joda.time.DateTimeZone;
import org.opentripplanner.analyst.ResultSet;
import org.opentripplanner.analyst.SampleSet;
import org.opentripplanner.analyst.TimeSurface;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.pathparser.InitialStopSearchPathParser;
import org.opentripplanner.routing.pathparser.PathParser;
import org.opentripplanner.routing.spt.DominanceFunction;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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

    private Logger LOG = LoggerFactory.getLogger(RepeatedRaptorProfileRouter.class);

    public static final int MAX_DURATION = 10 * 60 * 2; // seconds

    public ProfileRequest request;

    public Graph graph;

    // The spacing in minutes between RAPTOR calls within the time window
    public int stepMinutes = 1;

    /** Three time surfaces for min, max, and average travel time over the given time window. */
    public TimeSurface.RangeSet timeSurfaceRangeSet;

    /** If not null, completely skip this agency during the calculations. */
    public String banAgency = null;

    private ShortestPathTree walkOnlySpt;

    /** The sum of all earliest-arrival travel times to a given transit stop. Will be divided to create an average. */
    TObjectLongMap<TransitStop> accumulator = new TObjectLongHashMap<TransitStop>();

    /** The number of travel time observations added into the accumulator. The divisor when calculating an average. */
    TObjectIntMap<TransitStop> counts = new TObjectIntHashMap<TransitStop>();

    /** Samples to propagate times to */
    private SampleSet sampleSet;

    private PropagatedTimesStore propagatedTimesStore;

    /**
     * Make a router to use for making time surfaces only.
     * If you're building ResultSets, you should use the below method that uses a SampleSet; otherwise you maximum and average may not be correct.
     */
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

    public void route () {
        long computationStartTime = System.currentTimeMillis();
        LOG.info("Begin profile request");
        LOG.info("Finding initial stops");

        TObjectIntMap<TransitStop> accessTimes = findInitialStops(false);

        LOG.info("Found {} initial transit stops", accessTimes.size());

        /** THIN WORKERS */
        LOG.info("Make data...");
        TimeWindow window = new TimeWindow(request.fromTime, request.toTime + RaptorWorker.MAX_DURATION, graph.index.servicesRunning(request.date));

        Set<String> bannedRoutes = request.bannedRoutes == null ? null : new HashSet<String>(request.bannedRoutes);

        RaptorWorkerData raptorWorkerData;
        if (sampleSet == null)
            raptorWorkerData = new RaptorWorkerData(graph, window, bannedRoutes);
        else
            raptorWorkerData = new RaptorWorkerData(graph, window, bannedRoutes, sampleSet);
        LOG.info("Done.");
        // TEST SERIALIZED SIZE and SPEED
        //        try {
        //            LOG.info("serializing...");
        //            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("/Users/abyrd/worker.data"));
        //            out.writeObject(raptorWorkerData);
        //            out.close();
        //            LOG.info("done");
        //        } catch(IOException i) {
        //            i.printStackTrace();
        //        }

        int[] timesAtVertices = new int[Vertex.getMaxIndex()];
        Arrays.fill(timesAtVertices, TimeSurface.UNREACHABLE);

        for (State state : walkOnlySpt.getAllStates()) {
            // Note that we are using the walk distance divided by speed here in order to be consistent with the
            // least-walk optimization in the initial stop search (and the stop tree cache which is used at egress)
            int time = (int) (state.getWalkDistance() / request.walkSpeed);
            int vidx = state.getVertex().getIndex();
            int otime = timesAtVertices[vidx];

            // There may be dominated states in the SPT. Make sure we don't include them here.
            if (otime == TimeSurface.UNREACHABLE || otime > time)
                timesAtVertices[vidx] = time;

        }

        int[] walkTimes;

        if (sampleSet == null) {
            walkTimes = timesAtVertices;
        }
        else {
            walkTimes = sampleSet.eval(timesAtVertices);
        }

        RaptorWorker worker = new RaptorWorker(raptorWorkerData, request);
        propagatedTimesStore = worker.runRaptor(graph, accessTimes, walkTimes);

        if (sampleSet == null) {
            timeSurfaceRangeSet = new TimeSurface.RangeSet();
            timeSurfaceRangeSet.min = new TimeSurface(this);
            timeSurfaceRangeSet.avg = new TimeSurface(this);
            timeSurfaceRangeSet.max = new TimeSurface(this);
            propagatedTimesStore.makeSurfaces(timeSurfaceRangeSet);
        }

        LOG.info("Profile request finished in {} seconds", (System.currentTimeMillis() - computationStartTime) / 1000.0);
    }

    /** find the boarding stops */
    private TObjectIntMap<TransitStop> findInitialStops(boolean dest) {
        double lat = dest ? request.toLat : request.fromLat;
        double lon = dest ? request.toLon : request.fromLon;
        QualifiedModeSet modes = dest ? request.accessModes : request.egressModes;

        RoutingRequest rr = new RoutingRequest(TraverseMode.WALK);
        rr.batch = true;
        rr.from = new GenericLocation(lat, lon);
        //rr.walkSpeed = request.walkSpeed;
        rr.to = rr.from;
        rr.setRoutingContext(graph);
        rr.rctx.pathParsers = new PathParser[] { new InitialStopSearchPathParser() };
        rr.dateTime = request.date.toDateMidnight(DateTimeZone.forTimeZone(graph.getTimeZone())).getMillis() / 1000 +
                request.fromTime;

        // We use walk-distance limiting and a least-walk dominance function in order to be consistent with egress walking
        // which is implemented this way because walk times can change when walk speed changes. Also, walk times are floating
        // point and can change slightly when streets are split. Street lengths are internally fixed-point ints, which do not
        // suffer from roundoff. Great care is taken when splitting to preserve sums.
        rr.maxWalkDistance = 2000;
        rr.softWalkLimiting = false;
        rr.dominanceFunction = new DominanceFunction.LeastWalk();

        AStar astar = new AStar();
        rr.longDistance = true;
        rr.setNumItineraries(1);

        ShortestPathTree spt = astar.getShortestPathTree(rr, 5); // timeout in seconds

        TObjectIntMap<TransitStop> accessTimes = new TObjectIntHashMap<TransitStop>(); 

        for (TransitStop tstop : graph.index.stopVertexForStop.values()) {
            State s = spt.getState(tstop);
            if (s != null) {
                // note that we calculate the time based on the walk speed here rather than
                // based on the time. this matches what we do in the stop tree cache.
                accessTimes.put(tstop, (int) (s.getWalkDistance() / request.walkSpeed));
            }
        }

        this.walkOnlySpt = spt;

        rr.cleanup();
        return accessTimes;
    }

    /** Make a result set range set, optionally including times */
    public ResultSet.RangeSet makeResults (boolean includeTimes) {
        return propagatedTimesStore.makeResults(sampleSet, includeTimes);
    }
}
