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

import java.util.Arrays;
import java.util.List;

import org.opentripplanner.common.geometry.DistanceLibrary;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.pqueue.BinHeap;
import org.opentripplanner.common.pqueue.OTPPriorityQueue;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.graph.AbstractVertex;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.DefaultRemainingWeightHeuristicFactoryImpl;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.services.RemainingWeightHeuristicFactory;
import org.opentripplanner.routing.spt.BasicShortestPathTree;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class ThreadedBidirectionalHeuristic implements RemainingWeightHeuristic {

    private static final long serialVersionUID = 20111002L;

    private static Logger LOG = LoggerFactory.getLogger(LBGRemainingWeightHeuristic.class);

    private boolean aborted = false;
    
    Vertex target;

    // it is important that whenever a thread sees a higher maxFound, the preceding writes to the 
    // node table are also visible. Since Java 1.5 (JSR133) the memory model specifies that 
    // accessing a volatile will flush caches. Word tearing is also avoided by volatile.
    volatile double maxFound = 0; 

    double[] weights;

    Graph g;

    public ThreadedBidirectionalHeuristic(Graph graph) {
        this.g = graph;
    }

    @Override
    public double computeInitialWeight(State s, Vertex target) {
        if (target == this.target) {
            LOG.debug("reusing existing heuristic");
        } else {
            this.target = target;
            new Thread(new Worker(s)).start();
            //singlethreaded debug
            //new Worker(s).run();
        }
        return 0;
    }

    @Override
    public double computeForwardWeight(State s, Vertex target) {
        return computeReverseWeight(s, target);
    }

    @Override
    public double computeReverseWeight(State s, Vertex target) {
        final Vertex v = s.getVertex();
//        if (v instanceof StreetLocation)
//            return 0;
//        if (s.getWeight() < 10 * 60)
//            return 0;
        int index = v.getIndex();
        if (index < weights.length) {
            double h = weights[index];
            // all valid street vertices should be explored before the main search starts
            if (v instanceof StreetVertex)
                return h;
            if (v instanceof StreetLocation) // temp locations might not be found in walk search
                return 0;
            // but many transit vertices may not yet be explored when the search starts
            else
                return Double.isInfinite(h) ? maxFound : h;
        } else // this vertex was created after this heuristic was calculated
            return 0;
    }

    @Override
    public void reset() {
    }

    private /* inner */ class Worker implements Runnable {

        Vertex origin;
        
        RoutingRequest options;

        BinHeap<Vertex> q;

        // constructor runs in main thread so sequential semantics are guaranteed
        Worker (State s) {
            this.options = s.getOptions();
            this.origin = s.getVertex();
            // make sure distance table is initialized before starting thread
            LOG.debug("initializing heuristic computation thread");
            int nVertices = AbstractVertex.getMaxIndex();
            weights = new double[nVertices];
            Arrays.fill(weights, Double.POSITIVE_INFINITY);
            // make sure street distances are known before starting thread
            LOG.debug("street searches");
            this.q = new BinHeap<Vertex>();
            streetSearch(options, false); // forward
            for (State stopState : streetSearch(options, true)) { // backward
                q.insert(stopState.getVertex(), stopState.getWeight());
            }
        }
        
        @Override
        public void run() {
            LOG.debug("start SSSP");
            long t0 = System.currentTimeMillis();
            while (!q.empty()) {
                if (aborted) {
                    LOG.debug("aborted threaded heuristic calculation.");
                    break;
                }
                double uw = q.peek_min_key();
                Vertex u = q.extract_min();
                maxFound = uw;
                if (u == origin) { // searching backward from target to origin
                    LOG.debug("hit origin.");
                }
                int ui = u.getIndex();
                if (uw > weights[ui])
                    continue;
                // OUTgoing for heuristic search when main search is arriveBy 
                for (Edge e : options.isArriveBy() ? u.getOutgoing() : u.getIncoming()) {
                    if (e instanceof StreetTransitLink) // no streets in this phase
                        continue;
                    Vertex v = options.isArriveBy() ? e.getToVertex() : e.getFromVertex();
                    int vi = v.getIndex();
                    // check for case where the edge's to vertex has been created after this worker
                    if (vi < weights.length) {
                        double ew = e.weightLowerBound(options);
                        if (ew < 0) {
                            LOG.error("negative edge weight {} qt {}", ew, e);
                            continue;
                        }
                        double vw = uw + ew;
                        if (weights[vi] > vw) {
                            weights[vi] = vw;
                            // selectively rekeying did not seem to offer any speed advantage
                            q.insert(v, vw);
                            // System.out.println("Insert " + v + " weight " + vw);
                        }
                    }
                }
            }            
            LOG.info("End SSSP ({} msec)", System.currentTimeMillis() - t0);
            
        }
        
    }

    public synchronized void abort () {
        this.aborted = true;
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
     */

    private List<State> streetSearch (RoutingRequest rr, boolean fromTarget) {
        rr = rr.clone();
        rr.setMaxWalkDistance(5000);
        if (fromTarget)
            rr.setArriveBy( ! rr.isArriveBy());
        List<State> stopStates = Lists.newArrayList();
        ShortestPathTree spt = new BasicShortestPathTree(rr);
        OTPPriorityQueue<State> pq = new BinHeap<State>();
        Vertex initVertex = fromTarget ? rr.rctx.target : rr.rctx.origin;
        State initState = new State(initVertex, rr);
        pq.insert(initState, 0);
        while ( ! pq.empty()) {
            double w = pq.peek_min_key();
            State s = pq.extract_min();
            Vertex v = s.getVertex();
            int vi = v.getIndex();
            
            if (!fromTarget) // only save distances on reverse search
                w = 0;
            if (vi < weights.length)
                if (weights[vi] > w)
                    weights[vi] = w;
            //LOG.debug("{} at v={}", w, v);
            
            if (v instanceof TransitStop) {
                stopStates.add(s);
                continue;
            }
            // here, arriveBy has been set to match actual directional behavior in this subsearch
            for (Edge e : rr.arriveBy ? v.getIncoming() : v.getOutgoing()) {
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
                return new ThreadedBidirectionalHeuristic (opt.rctx.graph);
            } else {
                LOG.debug("Non-transit itinerary requested.");
                return new DefaultRemainingWeightHeuristic();
            }
        }
    }
    
}
