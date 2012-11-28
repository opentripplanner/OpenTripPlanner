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

import org.opentripplanner.common.geometry.DistanceLibrary;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.pqueue.BinHeap;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.AbstractVertex;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.location.StreetLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadedBidirectionalHeuristic implements RemainingWeightHeuristic {

    private static final long serialVersionUID = 20111002L;

    private static Logger LOG = LoggerFactory.getLogger(LBGRemainingWeightHeuristic.class);

    Vertex target;

    double cutoff;
    
    // it is important that whenever a thread sees a higher maxFound, the preceding writes to the 
    // node table are also visible. Since Java 1.5 (JSR133) the memory model specifies that 
    // accessing a volatile will flush caches. Word tearing is also avoided by volatile.
    volatile double maxFound = 0; 

    double[] weights;

    Graph g;

    private DistanceLibrary distanceLibrary = SphericalDistanceLibrary.getInstance();

    /**
     * RemainingWeightHeuristic interface
     */

    public ThreadedBidirectionalHeuristic(Graph graph) {
        this.g = graph;
    }

    @Override
    public double computeInitialWeight(State s, Vertex target) {
        if (target == this.target && s.getOptions().maxWeight <= this.cutoff) {
            LOG.debug("reusing existing heuristic");
        } else {
            // make sure array is initialized before starting thread
            synchronized (this) {
                int nVertices = AbstractVertex.getMaxIndex();
                weights = new double[nVertices];
                Arrays.fill(weights, Double.POSITIVE_INFINITY);
                this.target = target;
                LOG.debug("spawning heuristic computation thread");
            }
            new Thread(new Worker(s)).start();
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
        if (v instanceof StreetLocation)
            return 0;
        if (s.getWeight() < 10 * 60)
            return 0;
        int index = v.getIndex();
        if (index < weights.length) {
            double h = weights[index];
            return Double.isInfinite(h) ? maxFound : h;
        } else
            return 0;
    }

    @Override
    public void reset() {
    }

    private /* inner */ class Worker implements Runnable {

        Vertex origin;
        
        RoutingRequest options;
        
        Worker (State s) {
            this.options = s.getOptions();
            this.origin = s.getVertex();
        }
        
        @Override
        public void run() {
            LOG.debug("recalc");
            cutoff = options.maxWeight;
            BinHeap<Vertex> q = new BinHeap<Vertex>();
            long t0 = System.currentTimeMillis();
            if (target instanceof StreetLocation) {
                for (Edge de : ((StreetLocation) target).getExtra()) {
                    Vertex gv;
                    if (options.isArriveBy()) {
                        gv = de.getToVertex();
                    } else {
                        gv = de.getFromVertex();
                    }
                    int gvi = gv.getIndex();
                    if (gv == target)
                        continue;
                    if (gvi >= weights.length)
                        continue;
                    weights[gvi] = 0;
                    q.insert(gv, 0);
                }
            } else {
                int i = target.getIndex();
                weights[i] = 0;
                q.insert(target, 0);
            }
            while (!q.empty()) {
                double uw = q.peek_min_key();
                Vertex u = q.extract_min();
                maxFound = uw;
                if (uw > cutoff)
                    break;
                if (u == origin) { // searching backward from target to origin
                    LOG.debug("hit origin.");
                    //break;
                }
                int ui = u.getIndex();
                if (uw > weights[ui])
                    continue;
                Iterable<Edge> edges;
                if (options.isArriveBy())
                    edges = u.getOutgoing();
                else
                    edges = u.getIncoming();
                for (Edge e : edges) {
                    Vertex v = options.isArriveBy() ? 
                        e.getToVertex() : e.getFromVertex();
                    double ew = e.weightLowerBound(options);
                    if (ew < 0) {
                        LOG.error("negative edge weight {} qt {}", ew, e);
                        continue;
                    }
                    double vw = uw + ew;
                    int vi = v.getIndex();
                    if (weights[vi] > vw) {
                        weights[vi] = vw;
                        // selectively rekeying did not seem to offer any speed advantage
                        q.insert(v, vw);
                        // System.out.println("Insert " + v + " weight " + vw);
                    }
                }
            }            
            LOG.info("End SSSP ({} msec)", System.currentTimeMillis() - t0);
            
        }
        
    }
    
}
