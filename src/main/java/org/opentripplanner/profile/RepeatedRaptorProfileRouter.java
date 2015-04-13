package org.opentripplanner.profile;

import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.map.hash.TObjectLongHashMap;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.lucene.document.Field.Store;
import org.joda.time.DateTimeZone;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.analyst.TimeSurface;
import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.algorithm.PathDiscardingRaptorStateStore;
import org.opentripplanner.routing.algorithm.Raptor;
import org.opentripplanner.routing.algorithm.RaptorStateStore;
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

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

/**
 * Perform profile routing using repeated RAPTOR searches.
 * 
 * This is conceptually very similar to the work of the Minnesota Accessibility Observatory (http://www.its.umn.edu/Publications/ResearchReports/pdfdownloadl.pl?id=2504)
 * They run repeated searches. We take advantage of the fact that the street network is static
 * (for the most part, assuming time-dependent turn restrictions and traffic are consistent across the time window)
 * and only run a fast transit search for each minute in the window.
 */
public class RepeatedRaptorProfileRouter {
    private Logger LOG = LoggerFactory.getLogger(RepeatedRaptorProfileRouter.class);
    
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
        
        Collection<State> states = findInitialStops(false);
        
        LOG.info("Found {} initial stops", states.size());
        
        final RoutingRequest rr = new RoutingRequest();
        rr.batch = true;
        rr.maxTransfers = 3;
        rr.arriveBy = false;
        rr.modes = new TraverseModeSet("WALK,TRANSIT");
        rr.from = new GenericLocation(request.fromLat, request.fromLon);
        rr.to = request.analyst ? new GenericLocation(request.fromLat, request.fromLon) : new GenericLocation(request.toLat, request.toLon);
        // we need to set the time here even though we change it later, to ensure the graph index picks up the right day.
        rr.dateTime = request.date.toDateMidnight(DateTimeZone.forTimeZone(graph.getTimeZone())).getMillis() / 1000 + request.fromTime;
        rr.setRoutingContext(graph);
        
        Map<TripPattern, TripTimeSubset> timetables =
        		TripTimeSubset.indexGraph(graph, request.date, request.fromTime, request.toTime + 120 * 60); 
        
        int i = 1;
        
        // + 2 is because we have one additional round because there is one more ride than transfer
        // (fencepost problem) and one additional round for the initial walk.
    	PathDiscardingRaptorStateStore rss = new PathDiscardingRaptorStateStore(rr.maxTransfers + 2, request.toTime + 120 * 60);
        
        // We assume the times are aligned to minutes, and we don't do a depart-after search starting
        // at the end of the window.
        for (int startTime = request.toTime - 60; startTime >= request.fromTime; startTime -= 60) {
        	// note that we do not have to change any times (except the times at the access stops)
        	// because this stores clock times not elapsed times.
        	rss.restart();
        	
        	// add the initial stops, or move back the times at them if not on the first search
        	for (State state : states) {
        		rss.put((TransitStop) state.getVertex(), (int) (state.getElapsedTimeSeconds() + startTime));
        	}
        	
        	if (++i % 30 == 0)
        		LOG.info("Completed {} RAPTOR searches", i);
            
        	//LOG.info("Filtering RAPTOR states");
            
            Raptor raptor = new Raptor(rss, rr, timetables);

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
    private Collection<State> findInitialStops(boolean dest) {
        double lat = dest ? request.toLat : request.fromLat;
        double lon = dest ? request.toLon : request.fromLon;
        QualifiedModeSet modes = dest ? request.accessModes : request.egressModes;
        
        List<State> stops = Lists.newArrayList();
        
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
        for (TransitStop tstop : graph.index.stopVertexForStop.values()) {
            State s = spt.getState(tstop);
            if (s != null) {
                stops.add(s);
            }
        }
        
        rr.cleanup();
        
        return stops;
    }
    
    private void makeSurfaces () {
	    LOG.info("Propagating from transit stops to the street network...");
	    // A map to store the travel time to each vertex
	    TimeSurface minSurface = new TimeSurface(this);
	    TimeSurface avgSurface = new TimeSurface(this);
	    TimeSurface maxSurface = new TimeSurface(this);
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
                int existing_min = minSurface.times.get(vertex);
                int existing_max = maxSurface.times.get(vertex);
                int existing_avg = avgSurface.times.get(vertex);
                // FIXME this is taking the least lower bound and the least upper bound
                // which is not necessarily wrong but it's a crude way to perform the combination
                if (existing_min == TimeSurface.UNREACHABLE || existing_min > propagated_min) {
                    minSurface.times.put(vertex, propagated_min);
                }
                if (existing_max == TimeSurface.UNREACHABLE || existing_max > propagated_max) {
                    maxSurface.times.put(vertex, propagated_max);
                }
                if (existing_avg == TimeSurface.UNREACHABLE || existing_avg > propagated_avg) {
                    avgSurface.times.put(vertex, propagated_avg);
                }
	        }
	    }
	    LOG.info("Done with propagation.");
	    /* Store the results in a field in the router object. */
	    timeSurfaceRangeSet = new TimeSurface.RangeSet();
	    timeSurfaceRangeSet.min = minSurface;
	    timeSurfaceRangeSet.max = maxSurface;
	    timeSurfaceRangeSet.avg = avgSurface;
	}

}
