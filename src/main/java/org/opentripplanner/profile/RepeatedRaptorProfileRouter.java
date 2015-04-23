package org.opentripplanner.profile;

import gnu.trove.iterator.TIntIntIterator;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import gnu.trove.procedure.TIntObjectProcedure;

import java.util.Arrays;
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

    private static final int MAX_TRANSFERS = 3;

    public ProfileRequest request;

    public Graph graph;

    // The spacing in minutes between RAPTOR calls within the time window
    public int stepMinutes = 1;

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
        // There is one more ride than transfer (hence MAX_TRANSFERS + 1), two rounds per ride (one for riding and one
        // for transferring), and one additional round for the initial walk.
        PathDiscardingRaptorStateStore rss = new PathDiscardingRaptorStateStore((MAX_TRANSFERS + 1) * 2 + 1);

        // figure out how many iterations we will do
        int iterations = (request.toTime - request.fromTime) / 60 * stepMinutes + 1; 
        
        // all the times you could reach every transit stop
        TIntObjectMap<int[]> timesAtStops = new TIntObjectHashMap<int[]>();
        
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

           for (TObjectIntIterator<TransitStop> it = rss.iterator(); it.hasNext();) {
        	   it.advance();
        	   
        	   int time = it.value();
        	   int tsidx = it.key().getIndex();
        	   
        	   if (time == Integer.MAX_VALUE) continue;
        	   
        	   time -= departureTime;
        	   
        	   int[] times;
        	   if (timesAtStops.containsKey(tsidx)) {
        		   times = timesAtStops.get(tsidx);
        	   }
        	   else {
        		   times = new int[iterations];
        		   Arrays.fill(times, Integer.MAX_VALUE);
        		   timesAtStops.put(tsidx, times);
        	   }
        	   
        	   times[i] = time;
           }
        }

        LOG.info("Profile request complete, creating time surfaces.");
        
        makeSurfaces(timesAtStops, iterations);
        
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

        // Initialize time surfaces (which will hold the final results) using the on-street search parameters
        timeSurfaceRangeSet = new TimeSurface.RangeSet();
        timeSurfaceRangeSet.min = new TimeSurface(spt, false);
        timeSurfaceRangeSet.max = new TimeSurface(spt, false);
        timeSurfaceRangeSet.avg = new TimeSurface(spt, false);

        rr.cleanup();
        return accessTimes;
    }
    

    private void makeSurfaces (final TIntObjectMap<int[]> timesAtStops, final int iterations) {
	    LOG.info("Propagating from transit stops to the street network...");
	    // Grab a cached map of distances to street intersections from each transit stop
	    StopTreeCache stopTreeCache = graph.index.getStopTreeCache();
	    // Iterate over all vertices that are near transit stops
	    stopTreeCache.distancesForVertex.forEachEntry(new TIntObjectProcedure<TIntIntMap>() {

			@Override
			public boolean execute(int vidx, TIntIntMap distancesToTransitStops) {
				Vertex v = graph.getVertexById(vidx);
				
				// get the best travel time to this vertex at every minute
				int[] bestTravelTimeAtMinute = new int[iterations];
				Arrays.fill(bestTravelTimeAtMinute, Integer.MAX_VALUE);
				
				for (TIntIntIterator it = distancesToTransitStops.iterator(); it.hasNext();) {
					it.advance();
					int tsidx = it.key();
					int[] timesAtStop = timesAtStops.get(tsidx);
					
					// this stop is never reachable
					if (timesAtStop == null)
						continue;
					
					for (int i = 0; i < timesAtStop.length; i++) {
						int time = timesAtStop[i];
						
						// this stop is not reachable at this minute
						if (time == Integer.MAX_VALUE)
							continue;
						
						// propagate
						// this does not affect the original array.
						time += it.value() / request.walkSpeed;
						
						// find the best travel time to this vertex
						if (time < bestTravelTimeAtMinute[i])
							bestTravelTimeAtMinute[i] = time;
					}
				}
				
				// get the average min and max
				int min = Integer.MAX_VALUE;
				int max = Integer.MIN_VALUE;
				long sum = 0;
				int count = 0;
				
				for (int time : bestTravelTimeAtMinute) {
					if (time == Integer.MAX_VALUE) continue;
					
					if (time < min) min = time;
					if (time > max) max = time;
					
					sum += time;
					count++;
				}
				
				// unreachable
				// note that min and max must be defined if count > 0
				if (count == 0) return true;
				
				int avg = (int) (sum / count);
				
				// we still need to compare and not just blindly put, in case walking is faster
				// TODO: do we ever want to mix transit and walking (e.g. max is walking but min is transit)
				// this is clearly a scenario that can exist in the world: if you're going five blocks, it might be fastest
				// to take the transit line if it's there, otherwise walk rather than waiting. 
				int existingMin = timeSurfaceRangeSet.min.times.get(v);
				if (existingMin == TimeSurface.UNREACHABLE || min < existingMin)
					timeSurfaceRangeSet.min.times.put(v, min);
				
				int existingMax = timeSurfaceRangeSet.max.times.get(v);
				if (existingMax == TimeSurface.UNREACHABLE || max < existingMax)
					timeSurfaceRangeSet.max.times.put(v, max);
				
				int existingAvg = timeSurfaceRangeSet.avg.times.get(v);
				if (existingAvg == TimeSurface.UNREACHABLE || avg < existingAvg)
					timeSurfaceRangeSet.avg.times.put(v, avg);
				
				return true;
				
			}
		});
	    
	    LOG.info("Done with propagation.");
	}
}
