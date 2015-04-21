package org.opentripplanner.profile;

import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import org.onebusaway.gtfs.model.AgencyAndId;
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

    /** Three time surfaces for min, max, and average travel time over the given time window. */
    public TimeSurface.RangeSet timeSurfaceRangeSet;

    /** Minimum maximum earliest-arrival travel time at each transit stop over the given time window. */
    public TObjectIntMap<TransitStop> mins = new TObjectIntHashMap<TransitStop>(3000, 0.75f, Integer.MAX_VALUE);
    public TObjectIntMap<TransitStop> maxs = new TObjectIntHashMap<TransitStop>(3000, 0.75f, Integer.MIN_VALUE);

    /** If not null, completely skip this route during the calculations. Format agency:id */
    public AgencyAndId banRoute = null;

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

        /** The current iteration number (start minute within the time window) for display purposes only. */
        int i = 1;

        /** A compacted tabular representation of all the patterns that are running on this date in this time window. */
        Map<TripPattern, TripTimeSubset> timetables =
                TripTimeSubset.indexGraph(graph, request.date, request.fromTime, request.toTime + MAX_DURATION);

        /** If a route is banned, remove all patterns belonging to that route from the timetable. */
        if (banRoute != null) {
            Route route = graph.index.routeForId.get(banRoute);
            LOG.info("Banning route {}", route);
            int n = 0;
            for (TripPattern pattern : graph.index.patternsForRoute.get(route)) {
                timetables.remove(pattern);
                n++;
            }
            LOG.info("Removed {} patterns.", n);
        }

        /** Iterate over all minutes in the time window, running a RAPTOR search at each minute. */
        for (int startTime = request.toTime - 60; startTime >= request.fromTime; startTime -= 60) {

        // Create a state store which will be reused calling RAPTOR with each departure time in reverse order.
        // This causes portions of the solution that do not change to be reused and should provide some speedup
        // over naively creating a new, empty state store for each minute.
        PathDiscardingRaptorStateStore rss = new PathDiscardingRaptorStateStore(MAX_TRANSFERS + 2);

            // Log progress every thirty minutes (iterations)
            if (++i % 30 == 0) {
                LOG.info("Completed {} RAPTOR searches", i);
            }

        	// The departure time has changed; adjust the maximum clock time that the state store will retain
            rss.maxTime = startTime + MAX_DURATION;
        	
        	// Reset the counter. This is important if reusing the state store from one call to the next.
        	rss.restart();
        	
        	// Find the arrival times at the initial transit stops
        	for (TObjectIntIterator<TransitStop> it = accessTimes.iterator(); it.hasNext();) {
        		it.advance();
        		// this is "transfer" from the origin
        		rss.put(it.key(), startTime + it.value(), true);
        	}
            
            // Call the RAPTOR algorithm for this start time
            Raptor raptor = new Raptor(graph, MAX_TRANSFERS, request.walkSpeed, rss, startTime, request.date, timetables);
            raptor.run();
            
            // Loop over all transit stops reached by RAPTOR at this minute,
            // updating min, max, and average travel time across all minutes (for the entire time window)
            for (TObjectIntIterator<TransitStop> it = raptor.iterator(); it.hasNext();) {
                it.advance();
                
                int et = it.value() - startTime;
                
                // In the dynamic programming (range-RAPTOR) method, some travel times can be left over from a previous
                // RAPTOR call at a later departure time, and therefore be greater than the time limit
//                if (et > MAX_DURATION)
//                	continue;
                
                TransitStop v = it.key();
                if (et < mins.get(v))
                    mins.put(v, et);
                
                if (et > maxs.get(v))
                    maxs.put(v, et);
                
                accumulator.putIfAbsent(v, 0);
                counts.putIfAbsent(v, 0);
                
                accumulator.adjustValue(v, et);
                counts.increment(v);
            }

        }
        
        // Disabled until we're sure transit routing works right
        // LOG.info("Profile request complete, propagating to the street network");
        // makeSurfaces();
        
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
        ShortestPathTree spt = astar.getShortestPathTree(rr, 5); // timeout in seconds
        
        TObjectIntMap<TransitStop> accessTimes = new TObjectIntHashMap<TransitStop>(); 
        
        for (TransitStop tstop : graph.index.stopVertexForStop.values()) {
            State s = spt.getState(tstop);
            if (s != null) {
                accessTimes.put(tstop, (int) s.getElapsedTimeSeconds());
            }
        }
        
        // initialize propagation with direct modes
        timeSurfaceRangeSet = new TimeSurface.RangeSet();
        timeSurfaceRangeSet.min = new TimeSurface(spt, false);
        timeSurfaceRangeSet.max = new TimeSurface(spt, false);
        timeSurfaceRangeSet.avg = new TimeSurface(spt, false);
        
        rr.cleanup();

        /*
        TransitStop bestStop = null;
        int bestTime = Integer.MAX_VALUE;
        for (TransitStop stop : accessTimes.keySet()) {
            int etime = accessTimes.get(stop);
            if (etime < bestTime) {
                bestTime = etime;
                bestStop = stop;
            }
        }
        LOG.info("{} at {} sec", bestStop, bestTime);
        accessTimes.clear();
        accessTimes.put(bestStop, bestTime);
        */
        return accessTimes;
    }
    
    private void makeSurfaces () {
	    LOG.info("Propagating from transit stops to the street network...");
	    // Grab a cached map of distances to street intersections from each transit stop
	    StopTreeCache stopTreeCache = graph.index.getStopTreeCache();
	    // Iterate over all nondominated rides at all clusters
	    for (TransitStop tstop : mins.keySet()) {
	        int lb0 = mins.get(tstop);
	        int ub0 = maxs.get(tstop);
	        int avg0 = (int) (accumulator.get(tstop) / counts.get(tstop));

            // Iterate over street intersections in the vicinity of this particular transit stop.
            // Shift the time range at this transit stop, merging it into that for all reachable street intersections.
            TObjectIntMap<Vertex> distanceToVertex = stopTreeCache.getDistancesForStop(tstop);
            for (TObjectIntIterator<Vertex> iter = distanceToVertex.iterator(); iter.hasNext(); ) {
                iter.advance();
                Vertex vertex = iter.key();
                // distance in meters over walkspeed in meters per second --> seconds
                int egressWalkTimeSeconds = (int) (iter.value() / request.walkSpeed);
                if (egressWalkTimeSeconds > request.maxWalkTime * 60) {
                    continue;
                }
                int propagated_min = lb0 + egressWalkTimeSeconds;
                int propagated_max = ub0 + egressWalkTimeSeconds;
                // TODO: we can't take the min propagated average and call it an average
                int propagated_avg = avg0 + egressWalkTimeSeconds;
                int existing_min = timeSurfaceRangeSet.min.times.get(vertex);
                int existing_max = timeSurfaceRangeSet.max.times.get(vertex);
                int existing_avg = timeSurfaceRangeSet.avg.times.get(vertex);
                // FIXME this is taking the least lower bound and the least upper bound
                // which is not necessarily wrong but it's a crude way to perform the combination
                if (existing_min == TimeSurface.UNREACHABLE || existing_min > propagated_min) {
                    timeSurfaceRangeSet.min.times.put(vertex, propagated_min);
                }
                if (existing_max == TimeSurface.UNREACHABLE || existing_max > propagated_max) {
                    timeSurfaceRangeSet.max.times.put(vertex, propagated_max);
                }
                if (existing_avg == TimeSurface.UNREACHABLE || existing_avg > propagated_avg) {
                    timeSurfaceRangeSet.avg.times.put(vertex, propagated_avg);
                }
	        }
	    }
	    
	    LOG.info("Done with propagation.");
	    /* Store the results in a field in the router object. */
	}

}
