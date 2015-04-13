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

import org.joda.time.DateTimeZone;
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
    
    TObjectIntMap<Vertex> mins = new TObjectIntHashMap<Vertex>(3000, 0.75f, Integer.MAX_VALUE); 
    TObjectIntMap<Vertex> maxs = new TObjectIntHashMap<Vertex>(3000, 0.75f, Integer.MIN_VALUE);
    
    // accumulate the travel times to create an average
    TObjectLongMap<Vertex> accumulator = new TObjectLongHashMap<Vertex>();
    TObjectIntMap<Vertex> counts = new TObjectIntHashMap<Vertex>();
    
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
        
        // loop backwards with intention of eventually implementing dynamic programming/rRAPTOR based approach
        int i = 1;
        
        // We assume the times are aligned to minutes, and we don't do a depart-after search starting
        // at the end of the window.
        for (int startTime = request.toTime - 60; startTime >= request.fromTime; startTime -= 60) {
        	RaptorStateStore rss = new PathDiscardingRaptorStateStore();
        	
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
                Vertex v = it.key();
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
        
        rr.cleanup();
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
    
    private void makeSurfaces() {
        LOG.info("Propagating from transit stops to the street network...");
        List<State> lower = Lists.newArrayList();
        List<State> upper = Lists.newArrayList();
        List<State> avg = Lists.newArrayList();
        
        RoutingRequest rr = new RoutingRequest(TraverseMode.WALK);
        rr.batch = (true);
        rr.from = new GenericLocation(request.fromLat, request.fromLon);
        rr.setRoutingContext(graph);
        rr.longDistance = true;
        rr.dominanceFunction = new DominanceFunction.EarliestArrival();
        rr.setNumItineraries(1);
        rr.worstTime = rr.dateTime + 2 * 60 * 60;
       
        long startTime = rr.dateTime;
        
        State origin = new State(rr);
        
        // Iterate over all rides at all clusters
        // Note that some may be dominated, but it doesn't matter
        // Multi-origin Dijkstra search; preinitialize the queue with states at each transit stop
        for (Vertex v : mins.keySet()) {
        	if (maxs.get(v) > 60 * 999)
        		continue;
        	
            lower.add(new State(v, null, mins.get(v) + startTime, startTime, rr));
            upper.add(new State(v, null, maxs.get(v) + startTime, startTime, rr));
            
            // just use a long, avoid repeated casting
            long average = accumulator.get(v) / counts.get(v);
            avg.add(new State(v, null, average + startTime, startTime, rr));
        }
        
        // get direct trips as well
        lower.add(origin);
        upper.add(origin);
        avg.add(origin);
        
        // create timesurfaces
        timeSurfaceRangeSet = new TimeSurface.RangeSet();

        AStar astar = new AStar();
        timeSurfaceRangeSet.min = new TimeSurface(astar.getShortestPathTree(rr, 20, null, lower), false);
        astar = new AStar();
        timeSurfaceRangeSet.max = new TimeSurface(astar.getShortestPathTree(rr, 20, null, upper), false);
        astar = new AStar();
        timeSurfaceRangeSet.avg = new TimeSurface(astar.getShortestPathTree(rr, 20, null, avg), false);
        
        rr.cleanup();
        
        LOG.info("Done with propagation.");
        /* Store the results in a field in the router object. */
    }
}
