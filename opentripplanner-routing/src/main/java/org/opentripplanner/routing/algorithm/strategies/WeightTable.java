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
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TransitStop;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.core.GenericVertex;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.edgetype.PatternAlight;
import org.opentripplanner.routing.edgetype.PatternBoard;
import org.opentripplanner.routing.edgetype.PatternDwell;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.PatternInterlineDwell;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.edgetype.PreBoardEdge;
import org.opentripplanner.routing.edgetype.SimpleEdge;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.edgetype.TurnEdge;
import org.opentripplanner.routing.pqueue.BinHeap;
import org.opentripplanner.routing.spt.BasicShortestPathTree;
import org.opentripplanner.routing.spt.ShortestPathTree;
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
	    BinHeap<State> heap = new BinHeap<State>(g.getVertices().size());
	    ShortestPathTree spt;
	    TraverseOptions options = new TraverseOptions();
	    final double MAX_WEIGHT = 60 * 60 * options.walkReluctance;
	    final double OPTIMISTIC_BOARD_COST = options.boardCost;  
	    for (float[] row : table)
	    	Arrays.fill(row, Float.POSITIVE_INFINITY);

	    int count = 0;
	    // for each transit stop in the graph
	    for (TransitStop origin : stopVertices) {
	    	count += 1;
	    	if (count % 1000 == 0) LOG.debug("TransitStop " + count + "/" + nStops);
    		//LOG.debug("ORIGIN " + origin);
	    	int oi = stopIndices.get(origin); // origin index
	    	// first check for walking transfers
	    	//LOG.debug("    Walk");
	    	heap.reset();
	    	spt = new BasicShortestPathTree(500000);
	    	State s0 = new State(origin, options);
	    	spt.add(s0);
	    	heap.insert(s0, s0.getWeight());
	    	while (! heap.empty()) {
	    		double w = heap.peek_min_key();
	    		State u = heap.extract_min();
	    		if (!spt.visit(u)) continue;
	    		Vertex uVertex = u.getVertex();
	    		//LOG.debug("heap extract " + u + " weight " + w);
	    		if (w > MAX_WEIGHT) break; 
	    		if (uVertex instanceof TransitStop) {
	    			int di = stopIndices.get(uVertex); // dest index
	    			table[oi][di] = (float)w;
	    			//LOG.debug("    Dest " + u + " w=" + w);        
	    		}
	    		GraphVertex gu = g.getGraphVertex(uVertex.getLabel());
	    		for (Edge e : gu.getOutgoing()) {
	    			if (! (e instanceof PreBoardEdge)) {
	    				State v = e.optimisticTraverse(u);
	    				if (v != null && spt.add(v))  
	    					heap.insert(v, v.getWeight());
	    			}
	    		}
	    	}

	    	// then check what is accessible in one transit trip
	    	heap.reset(); // recycle heap
	    	spt = new BasicShortestPathTree(50000);
	    	// first handle preboard edges 
	    	Queue<Vertex> q = new ArrayDeque<Vertex>(100);
	    	q.add(origin);
	    	while (! q.isEmpty()) { 
	    		Vertex u = q.poll();               
	    		GraphVertex gu = g.getGraphVertex(u.getLabel());
	    		for (Edge e : gu.getOutgoing()) {
	    			if (e instanceof PatternBoard ) {
	    				Vertex v   = ((PatternBoard)e).getToVertex();
	    				// give onboard vertices same index as their corresponding station
	    				stopIndices.put((GenericVertex)v, oi);
	    				StateEditor se = (new State(u, options)).edit(e);
	    				se.incrementWeight(OPTIMISTIC_BOARD_COST);
	    				s0 = se.makeState();
	    				spt.add(s0);
	    				heap.insert(s0, s0.getWeight());
	    				//_log.debug("    board " + tov);
	    			} else if (e instanceof FreeEdge) { // handle preboard
	    				Vertex v = ((FreeEdge)e).getToVertex();
	    				// give onboard vertices same index as their corresponding station
	    	    		stopIndices.put((GenericVertex)v, oi);
	    				q.add(v);
	    			}
	    		}
	    	}
	    	// all boarding edges for this stop have now been traversed
	    	//LOG.debug("    Transit");
	    	while (! heap.empty()) {
	    		// check for transit stops when pulling off of heap 
	    		// and continue when one is found
	    		// this is enough to prevent reboarding
	    		// need to mark closed vertices because otherwise cycles may appear (interlining...)
	    		double w = heap.peek_min_key();
	    		State  u = heap.extract_min();                
	    		if (! spt.visit(u)) continue;
		    	//LOG.debug("    Extract " + u + " w=" + w);
	    		Vertex uVertex = u.getVertex();
	    		if (uVertex instanceof TransitStop) {
	    			int di = stopIndices.get(uVertex); // dest index
	    			if (table[oi][di] > w) {
	    				table[oi][di] = (float)w;                                
	    				//LOG.debug("    Dest " + u + "w=" + w);
	    			}
	    			continue;
	    		}
	    		GraphVertex gu = g.getGraphVertex(uVertex.getLabel());
	    		for (Edge e : gu.getOutgoing()) {
			    	//LOG.debug("        Edge " + e);
	    			State v = e.optimisticTraverse(u);
	    			if (v!=null && spt.add(v))
	    				heap.insert(v, v.getWeight());
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
                if (ik == Float.POSITIVE_INFINITY) continue;
                for (int j=0; j<n; j++) {
                    double kj  = table[k][j];
                    if (kj == Float.POSITIVE_INFINITY) continue;
                    double ikj = ik + kj;
                    double ij  = table[i][j];
                    if (ikj < ij) table[i][j] = (float)ikj;
                }
            }                    
            if (k % 50 == 0) LOG.debug("k=" + k + "/" + n);
        }
    }
}
