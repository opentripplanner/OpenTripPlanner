/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.algorithm.strategies;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.opentripplanner.routing.core.DirectEdge;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.GraphVertex;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TransitStop;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.core.GenericVertex;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.edgetype.PatternAlight;
import org.opentripplanner.routing.edgetype.PatternBoard;
import org.opentripplanner.routing.edgetype.PatternDwell;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.PatternInterlineDwell;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.edgetype.SimpleEdge;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.edgetype.TurnEdge;
import org.opentripplanner.routing.pqueue.BinHeap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * WeightTable stores a table of lower bounds on shortest path weight between
 * all pairs of transit stops in a graph
 */
public class WeightTable implements Serializable {
	private static final long serialVersionUID = 20110506L; //YYYYMMDD
	private static final Logger LOG = LoggerFactory.getLogger(WeightTable.class);
    private float[][] table;
    private Graph g;
    Map<GenericVertex,Integer> stopIndices;
    
	public WeightTable(Graph g) {
		this.g = g;
		buildTable();
	}
	
	public double getWeight(Vertex from, Vertex to) {
		int fi = stopIndices.get(from);
		int ti = stopIndices.get(to);
		return table[fi][ti];
	}

	public boolean includes(Vertex v) {
		return stopIndices.containsKey(v);
	}

	//assignindices(Graph g)
	//update(origin, dest, newval)

	public void buildTable() {
		ArrayList<TransitStop> stopVertices;
	    
	    LOG.debug("Number of vertices: " + g.getVertices().size());        
	    stopVertices = new ArrayList<TransitStop>(); 
	    for (GraphVertex gv : g.getVertices())
	    	if (gv.vertex instanceof TransitStop) 
	    		stopVertices.add((TransitStop)gv.vertex);
	    int nStops = stopVertices.size();

	    stopIndices = new IdentityHashMap<GenericVertex, Integer>(nStops);
	    for (int i = 0; i < nStops; i++)
	    	stopIndices.put(stopVertices.get(i), i);
	    LOG.debug("Number of stops: " + nStops);        

	    table = new float[nStops][nStops];

	    LOG.debug("Performing search at each transit stop.");        
	    // make one heap and recycle it
	    BinHeap<Vertex> heap = new BinHeap<Vertex>(g.getVertices().size());
	    TraverseOptions options = new TraverseOptions();
	    final double MAX_WEIGHT = 60 * 60 * options.walkReluctance;
	    final double OPTIMISTIC_BOARD_COST = options.boardCost;  
	    for (float[] row : table)
	    	Arrays.fill(row, Float.POSITIVE_INFINITY);

	    List<DirectEdge> transferEdges = new ArrayList<DirectEdge>();
	    int count = 0;
	    // for each transit stop in the graph
	    for (TransitStop origin : stopVertices) {
	    	count += 1;
	    	if (count % 100 == 0) LOG.debug("TransitStop " + count + "/" + nStops);
    		//LOG.debug("ORIGIN " + origin);
	    	int oi = stopIndices.get(origin); // origin index
	    	// first check for walking transfers
	    	//LOG.debug("    Walk");
	    	heap.reset();
		    HashSet<Vertex> closed = new HashSet<Vertex>();
	    	heap.insert(origin, 0);
	    	while (! heap.empty()) {
	    		double w = heap.peek_min_key();
	    		Vertex u = heap.extract_min();
	    		//LOG.debug("heap extract " + u + " weight " + w);
	    		if (w > MAX_WEIGHT) break; // search FOR EVAR
	    		if (closed.contains(u)) continue; 
	    		closed.add(u);
	    		if (u instanceof TransitStop) {
	    			int di = stopIndices.get(u); // dest index
	    			table[oi][di] = (float)w;
	    			// add edge for transfer to graph
	    			SimpleEdge se = new SimpleEdge(origin, u, w, (int)(w/options.speed));
	    			transferEdges.add(se);
	    			//LOG.debug("    Dest " + u + " w=" + w);        
	    		}
	    		GraphVertex gu = g.getGraphVertex(u.getLabel());
	    		for (Edge e : gu.getOutgoing()) {
	    			if (e instanceof TurnEdge ||
    					e instanceof PlainStreetEdge ||
    					e instanceof StreetTransitLink ||
    					e instanceof FreeEdge ) {
	    				State s0 = new State();
	    				TraverseResult tr = e.traverse(s0, options);
	    				if (tr == null) continue;
	    				Vertex tov = tr.getEdgeNarrative().getToVertex();
	    				if (! closed.contains(tov)) 
	    					heap.insert(tov, w + tr.weight);
	    			}
	    		}
	    	}

	    	// then check what is accessible in one transit trip
	    	heap.reset(); // recycle heap
	    	// first handle preboard edges 
	    	Queue<Vertex> q = new ArrayDeque<Vertex>(100);
	    	q.add(origin);
	    	while (! q.isEmpty()) { 
	    		Vertex u = q.poll();               
	    		GraphVertex gu = g.getGraphVertex(u.getLabel());
	    		for (Edge e : gu.getOutgoing()) {
	    			if (e instanceof PatternBoard ) {
	    				Vertex tov = ((PatternBoard)e).getToVertex();
	    				// give onboard vertices same index as their corresponding station
	    				stopIndices.put((GenericVertex)tov, oi);
	    				heap.insert(tov, OPTIMISTIC_BOARD_COST);
	    				//_log.debug("    board " + tov);
	    			} else if (e instanceof FreeEdge) { // handle preboard
	    				Vertex tov = ((FreeEdge)e).getToVertex();
	    				// give onboard vertices same index as their corresponding station
	    	    		stopIndices.put((GenericVertex)tov, oi);
	    				q.add(tov);
	    			}
	    		}
	    	}
	    	// all boarding edges for this stop have now been traversed
	    	closed.clear();
	    	//LOG.debug("    Transit");
	    	while (! heap.empty()) {
	    		// check for transit stops when pulling off of heap 
	    		// and continue when one is found
	    		// this is enough to prevent reboarding
	    		// need to mark closed vertices because otherwise cycles may appear (interlining...)
	    		double w = heap.peek_min_key();
	    		Vertex u = heap.extract_min();                
	    		if (closed.contains(u)) continue;
		    	//LOG.debug("    Extract " + u + " w=" + w);
	    		closed.add(u);
	    		if (u instanceof TransitStop) {
	    			int di = stopIndices.get(u); // dest index
	    			if (table[oi][di] > w) {
	    				table[oi][di] = (float)w;                                
	    				//LOG.debug("    Dest " + u + "w=" + w);
	    			}
	    			continue;
	    		}
	    		GraphVertex gu = g.getGraphVertex(u.getLabel());
	    		for (Edge e : gu.getOutgoing()) {
			    	//LOG.debug("        Edge " + e);
	    			Vertex tov = null;
	    			double tw = 0;
	    			if (e instanceof PatternHop) {
	    				State s0 = new State();
	    				TraverseResult tr = ((PatternHop)e).optimisticTraverse(s0, options);
	    				if (tr != null) {
	    					tov = tr.getEdgeNarrative().getToVertex();
	    					tw = tr.weight;
	    				}
	    			} else if (e instanceof PatternDwell) {
	    				//State s0 = new State();
	    				//TraverseResult tr = ((PatternDwell)e).traverse(s0, walkOptions);
	    				//if (tr == null) continue;
	    				tov = ((PatternDwell)e).getToVertex();
	    				// optomisticTraverse not implemented
	    				// and traverse does not work because we are on no particular trip 
	    				// use 1
	    				tw = 1;
	    			} else if (e instanceof PatternInterlineDwell) {
	    				// not coherent with other optimistic traverse
	    				tov = ((PatternInterlineDwell)e).getToVertex();
	    				tw = ((PatternInterlineDwell)e).optimisticTraverse(options);
	    			} else if (e instanceof PatternAlight || e instanceof FreeEdge) {
	    				tov = ((DirectEdge)e).getToVertex();
	    				tw = 1;
	    			}
	    			if (tov != null && !closed.contains(tov)) heap.insert(tov, w + tw);
	    			//else LOG.debug("        (skip)");

	    		}
	    	}
	    }
	    floyd();
	}

	/* Find all pairs shortest paths */
    private void floyd() {
        LOG.debug("Floyd");
        int n = table.length;
        for (int k=0; k<n; k++) {
            for (int i=0; i<n; i++) {
                double ik = table[i][k];
                if (ik == Double.POSITIVE_INFINITY) continue;
                for (int j=0; j<n; j++) {
                    double kj  = table[k][j];
                    if (kj == Integer.MAX_VALUE) continue;
                    double ikj = ik + kj;
                    double ij  = table[i][j];
                    if (ikj < ij) table[i][j] = (float)ikj;
                }
            }                    
            if (k % 20 == 0) LOG.debug("k=" + k + "/" + n);
        }
    }
}
