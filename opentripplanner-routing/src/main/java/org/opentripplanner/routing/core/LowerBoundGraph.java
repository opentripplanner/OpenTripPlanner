package org.opentripplanner.routing.core;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.opentripplanner.routing.impl.GraphServiceImpl;
import org.opentripplanner.routing.pqueue.BinHeap;
import org.opentripplanner.routing.pqueue.IntBinHeap;
import org.opentripplanner.routing.spt.BasicShortestPathTree;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A directed, non-time-dependent graph
 * intended for calculating embeddings and other heuristic-related stuff
 * @author andrewbyrd
 */
public class LowerBoundGraph {
    private static Logger LOG = LoggerFactory.getLogger(LowerBoundGraph.class);
	Graph originalGraph;
	int   [][] vertex;
	double[][] weight;
	int nVertices = 0;
    Vertex[] vertexByIndex;

	public LowerBoundGraph(Graph original) {
		originalGraph = original;
		nVertices = GenericVertex.maxIndex;
		vertex = new int   [nVertices][];
		weight = new double[nVertices][];
		vertexByIndex = new Vertex[nVertices];
		TraverseOptions opt = new TraverseOptions();
		LOG.info("Loading origial graph into compact representation...");
		ArrayList<State> svs = new ArrayList<State>();
		for (GraphVertex gv : original.getVertices()) {
			GenericVertex u = (GenericVertex) (gv.vertex); 
			State su = new State(u, opt);
			svs.clear();
			// avoid empty edgelist entries by traversing all edges first 
			for (Edge e : original.getOutgoing(u)) {
				State sv = e.optimisticTraverse(su);
				if (sv != null) 
					svs.add(sv);
			}
			int ui = u.index;
			int ne = svs.size(); 
			vertex[ui] = new int[ne];
			weight[ui] = new double[ne];
			int ei = 0;
			for (State sv : svs) {
				vertex[ui][ei] = ((GenericVertex)(sv.getVertex())).index;
				weight[ui][ei] = sv.getWeight();
				ei++;
			}
			vertexByIndex[ui] = u;
		}
	}
	
	// turn discrete normed (quasi-metric) space 
	// into a discrete metric space 
	// while maintaining lower bound property
	private void symmetricize() {
		LOG.info("Making distance metric commutative");
		for (int ui = 0; ui < nVertices; ui++) {
			int[]    u_vs = vertex[ui];
			double[] u_ws = weight[ui];
			int      u_ne = u_vs.length;
			for (int u_ei = 0; u_ei < u_ne; u_ei++) {
				int    vi = u_vs[u_ei]; 
				double vw = u_ws[u_ei];
				int[]    v_vs = vertex[vi];
				double[] v_ws = weight[vi];
				int      v_ne = v_vs.length;
				boolean found = false;
				for (int v_ei = 0; v_ei < v_ne; ++v_ei) {
					if (v_vs[v_ei] == ui) {
	  					if (v_ws[v_ei] > vw)
							v_ws[v_ei] = vw;
	  					else 
	  						u_ws[u_ei] = v_ws[v_ei];
	  					found = true;
	  					break;
					}
				}
				if (! found) {
					vertex[vi] = Arrays.copyOf(v_vs, v_ne + 1);
					weight[vi] = Arrays.copyOf(v_ws, v_ne + 1);
					vertex[vi][v_ne] = ui;
					weight[vi][v_ne] = vw;
				}
			}			
		}
	}
	
	// single-source shortest path (weight to all reachable destinations)
	private double[] sssp(int originIndex) {
		LOG.info("Initializing SSSP");
		double[] result = new double[nVertices];
		Arrays.fill(result, Double.POSITIVE_INFINITY);
		result[originIndex] = 0;
		BinHeap<Integer> q = new BinHeap<Integer>();
		//boolean[] closed = new boolean[nVertices];
		q.insert(originIndex, 0);
		LOG.info("Performing SSSP");
		while ( ! q.empty()) {
			double   uw = q.peek_min_key();
			int      ui = q.extract_min();
			int[]    vs = vertex[ui];
			double[] ws = weight[ui];
			int      ne = vs.length;
			//closed[ui]  = true;
			for (int ei = 0; ei < ne; ei++) {
				int    vi = vs[ei]; 
				double vw = ws[ei] + uw;
				//if (closed[vi])
				//	continue;
				if (result[vi] > vw) {
					result[vi] = vw;
					q.insert(vi, vw);
				}
			}
		}
		LOG.info("End SSSP");
		return result;
	}
	
	private double[] astar(int originIndex, int destIndex) {
		LOG.info("Performing Astar");
		double[] result = new double[nVertices];
		Arrays.fill(result, Double.POSITIVE_INFINITY);
		double[] heuristic = sssp(destIndex);
		IntBinHeap q = new IntBinHeap();
		//boolean[] closed = new boolean[nVertices];
		q.insert(originIndex, 0);
    	long t0 = System.currentTimeMillis();
		while ( ! q.empty()) {
			if (q.peek_min() == destIndex) break;
			double   uw = q.peek_min_key();
			int      ui = q.extract_min();
			int[]    vs = vertex[ui];
			double[] ws = weight[ui];
			int      ne = vs.length;
			//closed[ui]  = true;
			for (int ei = 0; ei < ne; ++ei) {
				int    vi = vs[ei]; 
				double vw = ws[ei] + uw;
				//if (closed[vi])
				//	continue;
				if (result[vi] > vw) {
					result[vi] = vw;
					q.insert(vi, vw + heuristic[vi]);
				}
			}
		}
    	long t1 = System.currentTimeMillis();
    	LOG.info("search time was {} msec", (t1 - t0));
		LOG.info("End Astar");
		return result;
	}
	
	public static void main(String args[]) {
		File f = new File("/home/syncopate/otp_data/pdx/Graph.obj");
        GraphServiceImpl graphService = new GraphServiceImpl();
        graphService.setGraphPath(f);
        graphService.refreshGraph();
        Graph g = graphService.getGraph();
        LowerBoundGraph lbg = new LowerBoundGraph(g);
        for (int i = 1000; i < 4000; i += 500) {
        	long t0 = System.currentTimeMillis();
        	double[] result1 = lbg.sssp(i);
        	long t1 = System.currentTimeMillis();
        	LOG.info("search time was {} msec", (t1 - t0));
        	int nFound = 0;
        	int nFails = 0;
        	for (double w : result1)
        		if (w == Double.POSITIVE_INFINITY ) nFails++;
        		else nFound++;
        	LOG.info("number of unreached destinations {}/{}", nFails, result1.length);
        	
        	// also good for checking that optimisticTraverse is not path-dependent
        	ShortestPathTree result2 = lbg.originalSSSP(lbg.vertexByIndex[i]);
        	int nMatch = 0;
        	int nWrong = 0;
        	for (int vi = 0; vi < lbg.nVertices; vi++) {
        		double w1 = result1[vi];
        		double w2 = Double.POSITIVE_INFINITY;
        		State  s2 = result2.getState(lbg.vertexByIndex[vi]);
        		if (s2 != null)
        			w2 = s2.getWeight();
        		if (w1 != w2) {
        			LOG.trace("Mismatch : {} vs {}", w1, w2);
        			nWrong++;
        		} else {
        			nMatch++;
        		}
        	}
    		LOG.debug("Matches {} mismatches {}", nMatch, nWrong);
        }

        lbg.symmetricize();
        for (int i = 1000; i < 4000; i += 500) {
        	double[] result = lbg.astar(i, i+10000);
        }        
	}

	// testing search function that does an optimistic search in the original graph
	private ShortestPathTree originalSSSP(Vertex o){
		LOG.info("Initializing original SSSP");
		BasicShortestPathTree spt = new BasicShortestPathTree();
		BinHeap<State> q = new BinHeap<State>();
		TraverseOptions opt = new TraverseOptions();
		opt.maxWalkDistance = Double.MAX_VALUE;
		State initialState = new State(o, opt);
		q.insert(initialState, 0);
		spt.add(initialState);
		LOG.info("Performing original SSSP");
    	long t0 = System.currentTimeMillis();
		while ( ! q.empty()) {
			State  su = q.extract_min();
			if ( ! spt.visit(su)) continue;
			for (Edge e : originalGraph.getOutgoing(su.getVertex())) {
				State sv = e.optimisticTraverse(su);
				if (sv != null && spt.add(sv))
					q.insert(sv, sv.getWeight());
			}
		}
		LOG.info("End original SSSP");
    	long t1 = System.currentTimeMillis();
    	LOG.info("search time was {} msec", (t1 - t0));
		return spt;
	}
	

}
