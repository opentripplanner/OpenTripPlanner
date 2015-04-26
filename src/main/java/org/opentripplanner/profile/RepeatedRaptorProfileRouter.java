package org.opentripplanner.profile;

import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.map.hash.TObjectLongHashMap;
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
import org.opentripplanner.routing.spt.DominanceFunction;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.trippattern.TripTimeSubset;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

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
    public int stepMinutes = 2;

    /** Three time surfaces for min, max, and average travel time over the given time window. */
    public TimeSurface.RangeSet timeSurfaceRangeSet;

    /** If not null, completely skip this agency during the calculations. */
    public String banAgency = null;

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

        RaptorWorker worker = new RaptorWorker(raptorWorkerData);
        PropagatedTimesStore propagatedTimesStore = worker.runRaptor(graph, accessTimes);
        propagatedTimesStore.makeSurfaces(timeSurfaceRangeSet);

        if (true) {
            return;
        }

        /** A compacted tabular representation of all the patterns that are running on this date in this time window. */
        Map<TripPattern, TripTimeSubset> timetables =
                TripTimeSubset.indexGraph(graph, request.date, request.fromTime, request.toTime + MAX_DURATION);

        /** If a route is banned, remove all patterns belonging to that route from the timetable. */
        if (banAgency != null) {
            for (Route route : graph.index.routeForId.values()) {
                if (route.getAgency().getId().equals(banAgency)) {
                    LOG.info("Banning route {}", route);
                    int n = 0;
                    for (TripPattern pattern : graph.index.patternsForRoute.get(route)) {
                        timetables.remove(pattern);
                        n++;
                    }
                    LOG.info("Removed {} patterns.", n);
                }
            }
        }

        // Create a state store which will be reused calling RAPTOR with each departure time in reverse order.
        // This causes portions of the solution that do not change to be reused and should provide some speedup
        // over naively creating a new, empty state store for each minute.
        PathDiscardingRaptorStateStore rss = new PathDiscardingRaptorStateStore(MAX_TRANSFERS * 2 + 1);

        // Summary stats across all minutes of the time window
        PropagatedTimesStore windowSummary = new PropagatedTimesStore(graph);
        // PropagatedHistogramsStore windowHistograms = new PropagatedHistogramsStore(90);

        /** Iterate over all minutes in the time window, running a RAPTOR search at each minute. */
        for (int i = 0, departureTime = request.toTime - 60 * stepMinutes;
             departureTime >= request.fromTime;
             departureTime -= 60 * stepMinutes) {

            // Log progress every N iterations
            if (++i % 5 == 0) {
                LOG.info("Completed {} RAPTOR searches", i);
            }

        	// The departure time has changed; adjust the maximum clock time that the state store will retain
            rss.maxTime = departureTime + MAX_DURATION;
        	
        	// Reset the counter. This is important if reusing the state store from one call to the next.
        	rss.restart();
        	
        	// Find the arrival times at the initial transit stops
        	for (TObjectIntIterator<TransitStop> it = accessTimes.iterator(); it.hasNext();) {
        		it.advance();
        		rss.put(it.key(), departureTime + it.value(), true); // store walk times for reachable transit stops
        	}
            
            // Call the RAPTOR algorithm for this particular departure time
            Raptor raptor = new Raptor(graph, MAX_TRANSFERS, request.walkSpeed, rss, departureTime, request.date, timetables);
            raptor.run();

            // Propagate minimum travel times out to vertices in the street network
            StopTreeCache stopTreeCache = graph.index.getStopTreeCache();
            TObjectIntIterator<TransitStop> resultIterator = raptor.iterator();
            int[] minsPerVertex = new int[Vertex.getMaxIndex()];
            while (resultIterator.hasNext()) {
                resultIterator.advance();
                TransitStop transitStop = resultIterator.key();
                int arrivalTime = resultIterator.value();
                if (arrivalTime == Integer.MAX_VALUE) continue; // stop was not reached in this round (why was it included in map?)
                int elapsedTime = arrivalTime - departureTime;
                stopTreeCache.propagateStop(transitStop, elapsedTime, request.walkSpeed, minsPerVertex);
            }
            // We now have the minimum travel times to reach each street vertex for this departure minute.
            // Accumulate them into the summary statistics for the entire time window.
            windowSummary.mergeIn(minsPerVertex);

            // The Holy Grail: histograms per street vertex. It actually seems as fast or faster than summary stats.
            // windowHistograms.mergeIn(minsPerVertex);
        }

        LOG.info("Profile request complete, creating time surfaces.");
        windowSummary.makeSurfaces(timeSurfaceRangeSet);
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
        
        // RoutingRequest dateTime defaults to currentTime.
        // If elapsed time is not capped, searches are very slow.
        rr.worstTime = (rr.dateTime + request.maxWalkTime * 60);
        AStar astar = new AStar();
        rr.longDistance = true;
        rr.setNumItineraries(1);

        // Set a path parser to avoid strange edge sequences.
        // TODO choose a path parser that actually works here, or reuse nearbyStopFinder!
        //rr.rctx.pathParsers = new PathParser[] { new BasicPathParser() };
        ShortestPathTree spt = astar.getShortestPathTree(rr, 5); // timeout in seconds
        
        TObjectIntMap<TransitStop> accessTimes = new TObjectIntHashMap<TransitStop>(); 
        
        for (TransitStop tstop : graph.index.stopVertexForStop.values()) {
            State s = spt.getState(tstop);
            if (s != null) {
                accessTimes.put(tstop, (int) s.getElapsedTimeSeconds());
            }
        }

        // Initialize time surfaces (which will hold the final results) using the on-street search parameters
        timeSurfaceRangeSet = new TimeSurface.RangeSet();
        timeSurfaceRangeSet.min = new TimeSurface(spt, false);
        timeSurfaceRangeSet.max = new TimeSurface(spt, false);
        timeSurfaceRangeSet.avg = new TimeSurface(spt, false);

        rr.cleanup();
        return accessTimes;
    }
    

}
