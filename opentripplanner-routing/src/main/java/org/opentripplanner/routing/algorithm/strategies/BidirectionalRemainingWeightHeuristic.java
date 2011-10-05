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
import org.opentripplanner.routing.core.GenericVertex;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.GraphVertex;
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
public class BidirectionalRemainingWeightHeuristic implements RemainingWeightHeuristic {

    private static final long serialVersionUID = 20111002L;

    private static Logger LOG = LoggerFactory.getLogger(LBGRemainingWeightHeuristic.class);

    Vertex target;

    double[] weights;

    int nVertices = 0;

    Graph g;

    // TraverseOptions opt;

    public BidirectionalRemainingWeightHeuristic(Graph g) {
        this.g = g;
    }

    @Override
    public double computeInitialWeight(State s, Vertex target) {
        recalculate(target, s.getOptions());
        return computeForwardWeight(s, target);
    }

    @Override
    public double computeForwardWeight(State s, Vertex target) {
        return computeReverseWeight(s, target);
    }

    @Override
    public double computeReverseWeight(State s, Vertex target) {
        int index = ((GenericVertex) s.getVertex()).getIndex();
        if (index < weights.length) {
            double h = weights[index];
            // System.out.printf("h=%f at %s\n", h, s.getVertex());
            return h == Double.POSITIVE_INFINITY ? 0 : h;
        } else
            return 0;
    }

    private void recalculate(Vertex target, TraverseOptions options) {
        if (target != this.target) {
            this.target = target;
            nVertices = GenericVertex.getMaxIndex();
            weights = new double[nVertices];
            Arrays.fill(weights, Double.POSITIVE_INFINITY);
            BinHeap<Vertex> q = new BinHeap<Vertex>();
            long t0 = System.currentTimeMillis();

            if (target instanceof StreetLocation) {
                for (DirectEdge de : ((StreetLocation) target).getExtra()) {
                    GenericVertex toVertex = (GenericVertex) (de.getToVertex());
                    int toIndex = toVertex.getIndex();
                    if (toVertex == target)
                        continue;
                    if (toIndex >= nVertices)
                        continue;
                    weights[toIndex] = 0;
                    q.insert(toVertex, 0);
                }
            } else {
                int i = ((GenericVertex) target).getIndex();
                weights[i] = 0;
                q.insert(target, 0);
            }

            while (!q.empty()) {
                double uw = q.peek_min_key();
                Vertex u = q.extract_min();
                int ui = ((GenericVertex) u).getIndex();
                if (uw > weights[ui])
                    continue;
                // System.out.println("Extract " + u + " weight " + uw);
                GraphVertex gv = g.getGraphVertex(u);
                if (gv == null)
                    continue;
                Iterable<Edge> edges;
                if (options.isArriveBy())
                    edges = gv.getOutgoing();
                else
                    edges = gv.getIncoming();
                for (Edge e : edges) {
                    if (e instanceof DirectEdge) {
                        GenericVertex v = (GenericVertex) ((DirectEdge) e).getToVertex();
                        double vw = uw + e.weightLowerBound(options);
                        int vi = v.getIndex();
                        if (weights[vi] > vw) {
                            weights[vi] = vw;
                            // System.out.println("Insert " + v + " weight " + vw);
                            q.insert(v, vw);
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

}
