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

package org.opentripplanner.routing.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.opentripplanner.common.geometry.DistanceLibrary;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.edgetype.Hop;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.common.pqueue.IntBinHeap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.strtree.STRtree;

/**
 * decimate
 * @author andrewbyrd
 */
public class SimplifiedLowerBoundGraph {

    private static Logger LOG = LoggerFactory.getLogger(SimplifiedLowerBoundGraph.class);
    
    Graph originalGraph;
    int   [][] vertex;
    double[][] weight;
    private List<List<Vertex>> vertex_by_gindex = new ArrayList<List<Vertex>>();
    int max_gindex = 0;
    final double GROUP_RADIUS = 5.0; // meters

    private void groupVertices() {
        LOG.info("grouping vertices by location...");
        max_gindex = 0;
        LOG.info("building spatial index...");
        STRtree vertexIndex = new STRtree();
        for (Vertex v : originalGraph.getVertices()) {
            Envelope env = new Envelope(v.getCoordinate());
            vertexIndex.insert(env, v);
            v.setGroupIndex(-1);
        }
        vertexIndex.build();
        for (Vertex v : originalGraph.getVertices()) {
            if (v.getGroupIndex() != -1)
                continue;
            Envelope env = new Envelope(v.getCoordinate());
            env.expandBy(DistanceLibrary.metersToDegrees(GROUP_RADIUS));
            @SuppressWarnings("unchecked")
            List<Vertex> nearby = vertexIndex.query(env);
            ArrayList<Vertex> group = new ArrayList<Vertex>();
            // group will contain at least v and possibly other vertices
            for (Vertex n : nearby) {
                if (n.getGroupIndex() == -1 && n.distance(v) <= GROUP_RADIUS) {
                    n.setGroupIndex(max_gindex);
                    group.add(n);
                }
            }
            group.trimToSize();
            vertex_by_gindex.add(group);
            max_gindex += 1;
            if (max_gindex % 10000 == 0)
                LOG.info("    group {}", max_gindex);
        }
    }

    @SuppressWarnings("unchecked")
    public SimplifiedLowerBoundGraph(Graph original) {
        this.originalGraph = original;
        groupVertices();
        Map<Integer, Double>[] timeEdges = (Map<Integer, Double>[]) new Map[max_gindex];
        Map<Integer, Double>[] distEdges = (Map<Integer, Double>[]) new Map[max_gindex];
        for (int group = 0; group < max_gindex; group++){
            timeEdges[group] = new HashMap<Integer, Double>();
            distEdges[group] = new HashMap<Integer, Double>();
        }
        LOG.info("finding border edges...");
        TraverseOptions dummyOptions = new TraverseOptions();
        for (int from_gindex = 0; from_gindex < max_gindex; from_gindex++) {
            if (from_gindex % 10000 == 0)
                LOG.info("    group {}", from_gindex);
            List<Vertex> group = vertex_by_gindex.get(from_gindex);
            for (Vertex u : group) {
                for (Edge e : u.getOutgoing()) {
                    Vertex v = e.getToVertex();
                    int to_gindex = v.getGroupIndex();
                    if (to_gindex == from_gindex)
                        continue;
                    if (e instanceof PatternHop || e instanceof Hop)
                        putIfBetter(distEdges, from_gindex, to_gindex, e.timeLowerBound(dummyOptions));
                    else 
                        putIfBetter(distEdges, from_gindex, to_gindex, e.getDistance());
                }
            }
        }
        LOG.info("saving outgoing edges for each group vertex...");
        vertex = new int   [max_gindex][];
        weight = new double[max_gindex][];
        for (int group = 0; group < max_gindex; group++){
            if (group % 10000 == 0)
                LOG.info("    group {}", group);
            Map<Integer, Double> te = timeEdges[group]; 
            Map<Integer, Double> de = distEdges[group];
            int nOutgoing = te.size() + de.size();
            vertex[group] = new int   [nOutgoing];
            weight[group] = new double[nOutgoing];
            int ei = 0;
            for (Entry<Integer, Double> edge : te.entrySet()) {
                vertex[group][ei] = edge.getKey();
                weight[group][ei] = edge.getValue();
                ei += 1;
            }
            for (Entry<Integer, Double> edge : de.entrySet()) {
                vertex[group][ei] = edge.getKey();
                weight[group][ei] = - (edge.getValue()); // sign is used to indicate speed-scalable edge
                ei += 1;
            }
        }
    }
            
    private boolean putIfBetter(Map<Integer, Double>[] mapArray, int fg, int tg, double value) {
        Map<Integer, Double> map = mapArray[fg];
        Double old_value = map.get(tg);
        if (old_value == null || old_value > value) {
            map.put(tg, value);
            return true;
        }
        return false;
    }
    
    // single-source shortest path (weight to all reachable destinations)
    public double[] sssp(StreetLocation origin, TraverseOptions options) {
        double[] result = new double[max_gindex];
        Arrays.fill(result, Double.POSITIVE_INFINITY);
        IntBinHeap q = new IntBinHeap(max_gindex / 2);
        for (Edge de : origin.getExtra()) {
            Vertex toVertex = de.getToVertex();
            int toGroup = toVertex.getGroupIndex();
            if (toVertex == origin)
                continue;
            if (toGroup >= max_gindex || toGroup < 0)
                continue;
            result[toGroup] = 0;
            q.insert(toGroup, 0);
        }
        double walkScale = -1 / options.speed * options.walkReluctance;
        LOG.info("Performing SSSP");
        long t0 = System.currentTimeMillis();
        while (!q.empty()) {
            double   uw = q.peek_min_key();
            int      ui = q.p_extract_min();
            int[]    vs = vertex[ui];
            double[] ws = weight[ui];
            LOG.trace("extract {}", uw);
            if (vs == null)
                continue;
            int ne = vs.length;
            for (int ei = 0; ei < ne; ei++) {
                int vi = vs[ei];
                double weight = ws[ei];
                if (weight < 0) // sign is used to indicate speed-scalable edges
                    weight = weight * walkScale; 
                double vw = uw + weight;
                if (result[vi] > vw) {
                    result[vi] = vw;
                    q.insert(vi, vw);
                }
            }
        }
        LOG.info("End SSSP ({} msec)", System.currentTimeMillis() - t0);
        return result;
    }
}
