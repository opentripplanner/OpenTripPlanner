/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (props, at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.algorithm.strategies;

import java.util.List;
import java.util.Map;

import org.opentripplanner.common.geometry.DistanceLibrary;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.pqueue.BinHeap;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.services.RemainingWeightHeuristicFactory;
import org.opentripplanner.routing.spt.BasicShortestPathTree;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TransitStationStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class InterleavedBidirectionalHeuristic implements RemainingWeightHeuristic {

    private static final long serialVersionUID = 20130813L;

    private static final int HEURISTIC_STEPS_PER_MAIN_STEP = 4;
    
    private static Logger LOG = LoggerFactory.getLogger(InterleavedBidirectionalHeuristic.class);

    private DistanceLibrary distanceLibrary = SphericalDistanceLibrary.getInstance();

    /* 
     * http://en.wikipedia.org/wiki/Train_routes_in_the_Netherlands
     * http://en.wikipedia.org/wiki/File:Baanvaksnelheden.png 
     */
    private final static double MAX_TRANSIT_SPEED = 45.0; // in meters/second
    
    /** The vertex that the main search is working towards. */
    Vertex target;

    double maxFound = 0;
    
    double minEgressWalk = 0;

    Map <Vertex, Double> weights;

    Graph graph;
    
    Vertex origin;
    
    RoutingRequest options;
    
    BinHeap<Vertex> q;

    boolean finished = false;
    
    public InterleavedBidirectionalHeuristic(Graph graph) {
        this.graph = graph;
    }

    
    /* Implementation observations:
     * 1. filling weights array with inf is expensive (~70 msec in PDX)
     * 2. keeping a separate closed bitset is ugly (and was not threadsafe)
     * 3. street movement is "almost Euclidean" in cost
     * 4. precomputing 20km origin/destination circles takes ~30 msec each
     *    1 or 2km takes only about 5msec each.
     *    20km using hashmap weights takes ~150 msec each AND search gets really slow due to unlimited walking
     *    
     * 5. we could get the and calculate origin circle on the fly using distance
     * 6. we could use a HashMap for weights
     * 0. Raising walk distance severely affects bidi heuristic performance
     * TODO: verify that everything works both arriveBy and departAfter
     * 
     * We could even create shortcut edges from the destination stops to the destination.
     */
    
    @Override
    public void initialize(State s, Vertex target, long abortTime) {
        if (target == this.target) {
            LOG.debug("reusing existing heuristic");
            return;
        }
        LOG.info("begin init heuristic        {}", System.currentTimeMillis());
        this.target = target;
        // int nVertices = AbstractVertex.getMaxIndex(); // will be ever increasing?
        int nVertices = graph.countVertices();
        weights = Maps.newHashMapWithExpectedSize(((int)Math.log(nVertices)) + 1);
        this.options = s.getOptions();
        this.origin = s.getVertex();
        // do not use soft limiting in long-distance mode
        options.setSoftWalkLimiting(false);
        options.setSoftPreTransitLimiting(false);
        // make sure distance table is initialized before starting thread
        LOG.debug("initializing heuristic computation thread");
        // forward street search first, sets values around origin to 0
        List<State> search = streetSearch(options, false, abortTime); // ~30 msec
        if (search == null) return; // Search timed out
        LOG.info("end foreward street search {}", System.currentTimeMillis());
        // create a new priority queue
        q = new BinHeap<Vertex>();
        // enqueue states for each stop within walking distance of the destination
        search = streetSearch(options, true, abortTime);
        if (search == null) return; // Search timed out
        for (State stopState : search) { // backward street search
            q.insert(stopState.getVertex(), stopState.getWeight());
        }
        LOG.info("end backward street search {}", System.currentTimeMillis());
        // once street searches are done, raise the limits to max
        // because hard walk limiting is incorrect and is observed to cause problems 
        // for trips near the cutoff
        options.setMaxWalkDistance(Double.POSITIVE_INFINITY);
        options.setMaxPreTransitTime(Integer.MAX_VALUE);
        LOG.debug("initialized SSSP");
        s.getOptions().rctx.debugOutput.finishedPrecalculating();
    }

    /** Do up to N iterations as long as the queue is not empty */
    @Override
    public void doSomeWork() {
        if (finished) return;
        for (int i = 0; i < HEURISTIC_STEPS_PER_MAIN_STEP; ++i) {
            if (q.empty()) {
                LOG.info("Emptied SSSP queue.");
                finished = true;
                break;
            }
            double uw = q.peek_min_key();
            Vertex u = q.extract_min();
            //LOG.info("dequeued weight {} at {}", uw, u);
//            // Ignore vertices that could be rekeyed (but are not rekeyed in this implementation).
//            if (uw > weights.get(u)) continue;
            // The weight of the queue head is uniformly increasing. This is the highest ever seen.
            maxFound = uw;
            
//            System.out.printf("H, %3.5f, %3.5f, %2.1f\n", u.getY(), u.getX(), 
//                    Double.isInfinite(uw) ? -1.0 : uw);

            // OUTgoing for heuristic search when main search is arriveBy 
            for (Edge e : options.isArriveBy() ? u.getOutgoing() : u.getIncoming()) {
                // Do not enter streets in this phase.
                if (e instanceof StreetTransitLink) continue;
                Vertex v = options.isArriveBy() ? e.getToVertex() : e.getFromVertex();
                double ew = e.weightLowerBound(options);
                // INF heuristic value indicates unreachable (e.g. non-running transit service)
                // this saves time by not reverse-exploring those routes and avoids maxFound of INF.
                if (Double.isInfinite(ew)) {
                    continue;  
                }
                double vw = uw + ew;
                Double old_vw = weights.get(v);
                if (old_vw == null || vw < old_vw) {
                    weights.put(v, vw);
                    q.insert(v, vw); 
                }
            }
        }
    }
    
    @Override
    public double computeForwardWeight(State s, Vertex target) {
        return computeReverseWeight(s, target);
    }

    /**
     * We must return an underestimate of the cost to reach the destination no matter how much 
     * progress has been made on the heuristic search.
     * 
     * All on-street vertices must be explored by the heuristic before the main search starts.
     * This allows us to completely skip walking outside a certain radius of the origin/destination.
     */
    @Override
    public double computeReverseWeight(State s, Vertex target) {
        final Vertex v = s.getVertex();
        // Temporary vertices (StreetLocations) might not be found in walk search.
        if (v instanceof StreetLocation) return 0;
        Double weight = weights.get(v);
        // All valid street vertices should be explored before the main search starts,
        // but many transit vertices may not yet be explored when the search starts.
        // TODO: verify that StreetVertex includes all vertices of interest.
        if (v instanceof StreetVertex) return weight == null ? Double.POSITIVE_INFINITY : weight;
        else if (weight == null) {
            double dist = distanceLibrary.fastDistance(v.getY(), v.getX(), target.getY(), target.getX());
            double time = dist / MAX_TRANSIT_SPEED;
            return Math.max(maxFound, time);
        }
        else return weight;
    }

    @Override
    public void reset() {
    }
        

    /*
    
    Main search always proceeds from the origin to the target (arriveBy or departAfter)
    heuristic search always proceeds outward from the target (arriveBy or departAfter)
    
    when main search is departAfter:
    it gets outgoing edges and traverses them with arriveBy=false
    heuristic search gets incoming edges and traverses them with arriveBy=true
    heuristic destination street search also gets incoming edges and traverses them with arriveBy=true
    heuristic origin street search gets outgoing edges and traverses them with arriveBy=false
    
    when main search is arriveBy:
    it gets incoming edges and traverses them with arriveBy=true
    heuristic search gets outgoing edges and traverses them with arriveBy=false
    heuristic destination street search also gets outgoing edges and traverses them with arriveBy=false
    heuristic origin street search gets incoming edges and traverses them with arriveBy=true
    
    We traverse using the real traverse method rather than the lower bound traverse method because
    this allows us to keep track of the distance walked.
    
    We are not boarding transit in the street search. Though walking is only allowed at the very 
    beginning and end of the trip, we have to worry about storing overestimated weights because 
    places around the origin may be walked through pre-transit.
    
    Perhaps rather than tracking walk distance, we should just check the straight-line radius and 
    only walk within that distance. This would avoid needing to call the main traversal functions.

    Really, as soon as you hit any transit stop during the destination search, you can't set any 
    weights higher than that amount unless you flood-fill with 0s from the origin.

    The other ultra-simple option is to just set the heuristic value to 0 for all on-street locations
    within walking distance of the origin and destination.
    
    Another way of achieving this is to search from the origin first, saving 0s, then search from
    the destination without overwriting any 0s.
    
    */

    private List<State> streetSearch (RoutingRequest rr, boolean fromTarget, long abortTime) {
        rr = rr.clone();
        if (fromTarget)
            rr.setArriveBy( ! rr.isArriveBy());
        List<State> stopStates = Lists.newArrayList();
        ShortestPathTree spt = new BasicShortestPathTree(rr);
        BinHeap<State> pq = new BinHeap<State>();
        Vertex initVertex = fromTarget ? rr.rctx.target : rr.rctx.origin;
        State initState = new State(initVertex, rr);
        pq.insert(initState, 0);
        while ( ! pq.empty()) {
            /**
             * Terminate the search prematurely if we've hit our computation wall.
             */
            if (abortTime < Long.MAX_VALUE  && System.currentTimeMillis() > abortTime) {
                return null;
            }

            State s = pq.extract_min();
            Double w = s.getWeight();
            Vertex v = s.getVertex();
            if (v instanceof TransitStationStop) {
                stopStates.add(s);
                // Prune street search upon reaching TransitStationStops.
                // Do not save weights at transit stops. Since they may be reached by 
                // SimpleTransfer their weights will be recorded during the main heuristic search.
                continue;
            }
            // at this point the vertex is closed (pulled off heap).
            // on reverse search save measured weights.
            // on forward search set heuristic to 0 -- we have no idea how far to the destination, 
            // the optimal path may use transit etc.
            if (!fromTarget) weights.put(v, 0.0);
            else {
                Double old_weight = weights.get(v);
                if (old_weight == null || old_weight > w) {
                    weights.put(v, w);
                }
            }

            for (Edge e : rr.arriveBy ? v.getIncoming() : v.getOutgoing()) {
                // arriveBy has been set to match actual directional behavior in this subsearch
                State s1 = e.traverse(s);
                if (s1 == null)
                    continue;
                if (spt.add(s1)) {
                    pq.insert(s1,  s1.getWeight());
                }
            }
        }
        // return a list of all stops hit
        LOG.debug("hit stops: {}", stopStates);
        return stopStates;
    }
 
    public static class Factory implements RemainingWeightHeuristicFactory {
        @Override
        public RemainingWeightHeuristic getInstanceForSearch(RoutingRequest opt) {
            if (opt.getModes().isTransit()) {
                LOG.debug("Transit itinerary requested.");
                return new InterleavedBidirectionalHeuristic (opt.rctx.graph);
            } else {
                LOG.debug("Non-transit itinerary requested.");
                return new DefaultRemainingWeightHeuristic();
            }
        }
    }

}
