package org.opentripplanner.profile;

import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.map.hash.TObjectLongHashMap;

import java.util.Map;

import org.joda.time.DateTimeZone;
import org.onebusaway.gtfs.model.Route;
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

    private static final int MAX_TRANSFERS = 20;

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
    
    public RepeatedRaptorProfileRouter (Graph graph, ProfileRequest request) {
        this.request = request;
        this.graph = graph;
    }
    
    public void route () {
    	long computationStartTime = System.currentTimeMillis();
    	LOG.info("Begin profile request");
        LOG.info("Finding initial stops");
        
        TObjectIntMap<TransitStop> accessTimes = findInitialStops(false);
        
        LOG.info("Found {} initial transit stops", accessTimes.size());

        /** THIN WORKERS */
        LOG.info("Make data...");
        TimeWindow window = new TimeWindow(request.fromTime, request.toTime, graph.index.servicesRunning(request.date));
        RaptorWorkerData raptorWorkerData = new RaptorWorkerData(graph, window);
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

        RaptorWorker worker = new RaptorWorker(raptorWorkerData, request);
        PropagatedTimesStore propagatedTimesStore = worker.runRaptor(graph, accessTimes);
        timeSurfaceRangeSet = new TimeSurface.RangeSet();
        timeSurfaceRangeSet.min = new TimeSurface(this);
        timeSurfaceRangeSet.avg = new TimeSurface(this);
        timeSurfaceRangeSet.max = new TimeSurface(this);
        propagatedTimesStore.makeSurfaces(timeSurfaceRangeSet);
        
        // add walk-only options.
        for (State state : walkOnlySpt.getAllStates()) {
        	int time = (int) state.getElapsedTimeSeconds();
        	Vertex v = state.getVertex();
        	
        	int emin = timeSurfaceRangeSet.min.getTime(v);
        	if (emin == TimeSurface.UNREACHABLE || time < emin)
        		timeSurfaceRangeSet.min.times.put(v, time);
        	
        	int emax = timeSurfaceRangeSet.max.getTime(v);
        	if (emax == TimeSurface.UNREACHABLE || time < emax)
        		timeSurfaceRangeSet.max.times.put(v, time);
        	
        	// TODO: this assumes that you always choose either to walk or to take transit
        	// but there are cases when you might choose one or the other at different times within the window 
        	int eavg = timeSurfaceRangeSet.avg.getTime(v);
        	if (eavg == TimeSurface.UNREACHABLE || time < eavg)
        		timeSurfaceRangeSet.avg.times.put(v, time);
        }

        LOG.info("Profile request finished in {} seconds", (System.currentTimeMillis() - computationStartTime) / 1000.0);
    }

    /** find the boarding stops */
    private TObjectIntMap<TransitStop> findInitialStops(boolean dest) {
        double lat = dest ? request.toLat : request.fromLat;
        double lon = dest ? request.toLon : request.fromLon;
        QualifiedModeSet modes = dest ? request.accessModes : request.egressModes;
                
        RoutingRequest rr = new RoutingRequest(TraverseMode.WALK);
        rr.dominanceFunction = new DominanceFunction.EarliestArrival();
        rr.batch = true;
        rr.from = new GenericLocation(lat, lon);
        rr.walkSpeed = request.walkSpeed;
        rr.to = rr.from;
        rr.setRoutingContext(graph);
        rr.rctx.pathParsers = new PathParser[] { new InitialStopSearchPathParser() };
        rr.dateTime = request.date.toDateMidnight(DateTimeZone.forTimeZone(graph.getTimeZone())).getMillis() / 1000 +
                request.fromTime;
        // RoutingRequest dateTime defaults to currentTime.
        // If elapsed time is not capped, searches are very slow.
        rr.worstTime = (rr.dateTime + request.maxWalkTime * 60);
        AStar astar = new AStar();
        rr.longDistance = true;
        rr.setNumItineraries(1);

        ShortestPathTree spt = astar.getShortestPathTree(rr, 5); // timeout in seconds
        
        TObjectIntMap<TransitStop> accessTimes = new TObjectIntHashMap<TransitStop>(); 
        
        for (TransitStop tstop : graph.index.stopVertexForStop.values()) {
            State s = spt.getState(tstop);
            if (s != null) {
                accessTimes.put(tstop, (int) s.getElapsedTimeSeconds());
            }
        }

        this.walkOnlySpt = spt;

        rr.cleanup();
        return accessTimes;
    }
    

}
