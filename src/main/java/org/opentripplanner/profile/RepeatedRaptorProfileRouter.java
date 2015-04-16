package org.opentripplanner.profile;

import com.google.common.collect.Lists;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import org.joda.time.DateTimeZone;
import org.opentripplanner.analyst.TimeSurface;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.algorithm.PathDiscardingRaptorStateStore;
import org.opentripplanner.routing.algorithm.Raptor;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.DominanceFunction;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.trippattern.TripTimeSubset;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
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

    public ProfileRequest request;
    
    public Graph graph;
    
    public TimeSurface.RangeSet timeSurfaceRangeSet;
    
    TObjectIntMap<TransitStop> mins = new TObjectIntHashMap<TransitStop>(3000, 0.75f, Integer.MAX_VALUE); 
    TObjectIntMap<TransitStop> maxs = new TObjectIntHashMap<TransitStop>(3000, 0.75f, Integer.MIN_VALUE);
    
    // accumulate the travel times to create an average
    TObjectLongMap<TransitStop> accumulator = new TObjectLongHashMap<TransitStop>();
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
        
        LOG.info("Found {} initial stops", accessTimes.size());
        
        Map<TripPattern, TripTimeSubset> timetables =
                TripTimeSubset.indexGraph(graph, request.date, request.fromTime, request.toTime + MAX_DURATION);

        int i = 1;
        
        // We assume the times are aligned to minutes, and we don't do a depart-after search starting
        // at the end of the window.
        for (int startTime = request.toTime - 60; startTime >= request.fromTime; startTime -= 60) {
        	if (++i % 30 == 0)
        		LOG.info("Completed {} RAPTOR searches", i);
            
        	//LOG.info("Filtering RAPTOR states");
            
            Raptor raptor = new Raptor(graph, 3, request.walkSpeed, accessTimes, startTime, request.date, timetables);

            //LOG.info("Performing RAPTOR search for minute {}", i++);
            
            raptor.run();
            
            //LOG.info("Finished RAPTOR search in {} milliseconds", System.currentTimeMillis() - roundStartTime);
            
            // loop over all states, accumulating mins, maxes, etc.
            for (TObjectIntIterator<TransitStop> it = raptor.iterator(); it.hasNext();) {
                it.advance();
                
                int et = it.value() - startTime;
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
        
        LOG.info("Profile request complete, propagating to the street network");
        
        makeSurfaces();
        
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
