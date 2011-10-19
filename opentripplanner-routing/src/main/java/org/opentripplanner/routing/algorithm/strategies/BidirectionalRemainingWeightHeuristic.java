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
import org.opentripplanner.routing.core.DirectEdge;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.pqueue.BinHeap;
import org.opentripplanner.routing.services.RemainingWeightHeuristicFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A heuristic that performs a single-source / all destinations shortest path search backward from
 * the target of the main search, using lower bounds on the weight of each edge.
 * 
 * @author andrewbyrd
 */
public class BidirectionalRemainingWeightHeuristic implements 
    RemainingWeightHeuristic, RemainingTimeHeuristic {

    private static final long serialVersionUID = 20111002L;

    private static Logger LOG = LoggerFactory.getLogger(LBGRemainingWeightHeuristic.class);

    Vertex target;

    double[] weights;

    int nVertices = 0;

    Graph g;

    /**
     * RemainingWeightHeuristic interface
     */

    public BidirectionalRemainingWeightHeuristic(Graph g) {
        this.g = g;
    }

    @Override
    public double computeInitialWeight(State s, Vertex target) {
        recalculate(target, s.getOptions(), false);
        return computeForwardWeight(s, target);
    }

    @Override
    public double computeForwardWeight(State s, Vertex target) {
        return computeReverseWeight(s, target);
    }

    @Override
    public double computeReverseWeight(State s, Vertex target) {
        if (s.getVertex() instanceof StreetLocation)
            return 0;
        if (s.getWeight() < 10 * 60)
            return 0;
        int index = ((Vertex) s.getVertex()).getIndex();
        if (index < weights.length) {
            double h = weights[index];
            // System.out.printf("h=%f at %s\n", h, s.getVertex());
            // return infinite heuristic values 
            // so transit boarding is not even attempted useless patterns
            return h;
            //return h == Double.POSITIVE_INFINITY ? 0 : h;
            
        } else
            return 0;
    }

    private void recalculate(Vertex target, TraverseOptions options, boolean timeNotWeight) {
        if (target != this.target) {
            this.target = target;
            this.nVertices = Vertex.getMaxIndex();
            weights = new double[nVertices];
            Arrays.fill(weights, Double.POSITIVE_INFINITY);
            BinHeap<Vertex> q = new BinHeap<Vertex>();
            long t0 = System.currentTimeMillis();

            if (target instanceof StreetLocation) {
                for (DirectEdge de : ((StreetLocation) target).getExtra()) {
                    Vertex gv;
                    if (options.isArriveBy()) {
                        gv = (Vertex) (de.getToVertex());
                    } else {
                        gv = (Vertex) (de.getFromVertex());
                    }
                    int gvi = gv.getIndex();
                    if (gv == target)
                        continue;
                    if (gvi >= nVertices)
                        continue;
                    weights[gvi] = 0;
                    q.insert(gv, 0);
                }
            } else {
                int i = ((Vertex) target).getIndex();
                weights[i] = 0;
                q.insert(target, 0);
            }
            while (!q.empty()) {
                double uw = q.peek_min_key();
                Vertex u = q.extract_min();
                int ui = ((Vertex) u).getIndex();
                if (uw > weights[ui])
                    continue;
                Iterable<Edge> edges;
                if (options.isArriveBy())
                    edges = u.getOutgoing();
                else
                    edges = u.getIncoming();
                for (Edge e : edges) {
                    if (e instanceof DirectEdge) {
                        Vertex v = (Vertex) (options.isArriveBy() ? 
                            ((DirectEdge) e).getToVertex() : ((DirectEdge) e).getFromVertex());
                        double vw = uw + (timeNotWeight ? 
                                e.timeLowerBound(options) : e.weightLowerBound(options));
                        int vi = v.getIndex();
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

    @Override
    public void reset() {
    }

    /**
     * RemainingTimeHeuristic interface
     */
    @Override
    public void timeInitialize(State s, Vertex target) {
        recalculate(target, s.getOptions(), true);
    }

    @Override
    public double timeLowerBound(State s) {
        return computeReverseWeight(s, null);
    }

}
