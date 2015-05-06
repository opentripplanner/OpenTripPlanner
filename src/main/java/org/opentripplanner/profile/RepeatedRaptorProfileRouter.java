package org.opentripplanner.profile;

import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.map.hash.TObjectLongHashMap;

import java.util.Arrays;
import java.util.Map;

import org.joda.time.DateTimeZone;
import org.onebusaway.gtfs.model.Route;
import org.opentripplanner.analyst.ResultSet;
import org.opentripplanner.analyst.ResultSet.RangeSet;
import org.opentripplanner.analyst.SampleSet;
import org.opentripplanner.analyst.TimeSurface;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.algorithm.PathDiscardingRaptorStateStore;
import org.opentripplanner.routing.algorithm.Raptor;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.pathparser.InitialStopSearchPathParser;
import org.opentripplanner.routing.pathparser.PathParser;
import org.opentripplanner.routing.spt.DominanceFunction;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.trippattern.TripTimeSubset;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Perform profile routing using repeated RAPTOR searches.
 * 
 * This is conceptually very similar to the work of the Minnesota Accessibility Observatory
 * (http://www.its.umn.edu/Publications/ResearchReports/pdfdownloadl.pl?id=2504)
 * They run repeated searches. We take advantage of the fact that the street network is static
 * (for the most part, assuming time-dependent turn restrictions and traffic are consistent across the time window)
 * and only run a fast transit search for each minute in the window.
 */
public class RepeatedRaptorProfileRouter {

    private Logger LOG = LoggerFactory.getLogger(RepeatedRaptorProfileRouter.class);

    public static final int MAX_DURATION = 60 * 60 * 2; // seconds

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
        
        RaptorWorkerData raptorWorkerData;
        if (sampleSet == null)
        	raptorWorkerData = new RaptorWorkerData(graph, window);
        else
        	raptorWorkerData = new RaptorWorkerData(graph, window, sampleSet);
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
        
        int[] walkTimes;
        
        if (sampleSet == null) {
	        walkTimes = new int[Vertex.getMaxIndex()];
	        Arrays.fill(walkTimes, RaptorWorker.UNREACHED);
	        
	        for (State state : walkOnlySpt.getAllStates()) {
	        	int time = (int) state.getElapsedTimeSeconds();
	        	int vidx = state.getVertex().getIndex();
	        	int otime = walkTimes[vidx];
	        	
	        	if (otime == RaptorWorker.UNREACHED || otime > time)
	        		walkTimes[vidx] = time;
		        	
		    }
        }
        
        else {
        	// propagate walk times all the way to samples
        	TimeSurface walk = new TimeSurface(walkOnlySpt, false);
        	ResultSet walkRs = new ResultSet(sampleSet, walk, true);
        	walkTimes = walkRs.times;
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
        rr.dominanceFunction = new DominanceFunction.LeastWalk();
        rr.batch = true;
        rr.from = new GenericLocation(lat, lon);
        //rr.walkSpeed = request.walkSpeed;
        rr.to = rr.from;
        rr.setRoutingContext(graph);
        rr.rctx.pathParsers = new PathParser[] { new InitialStopSearchPathParser() };
        rr.dateTime = request.date.toDateMidnight(DateTimeZone.forTimeZone(graph.getTimeZone())).getMillis() / 1000 +
                request.fromTime;
       
        rr.maxWalkDistance = 2000;
        rr.softWalkLimiting = false;
        
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
