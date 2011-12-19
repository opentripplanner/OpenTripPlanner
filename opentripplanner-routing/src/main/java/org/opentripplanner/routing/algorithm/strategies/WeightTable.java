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
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.edgetype.PatternBoard;
import org.opentripplanner.routing.edgetype.PreBoardEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.pqueue.BinHeap;
import org.opentripplanner.routing.spt.BasicShortestPathTree;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * WeightTable stores a table of lower bounds on shortest path weight between
 * all pairs of transit stops in a graph
 */
public class WeightTable implements Serializable {
	private static final long serialVersionUID = 20110506L; // YYYYMMDD
	private static final Logger LOG = LoggerFactory
			.getLogger(WeightTable.class);
	private float[][] table;
	private Graph g;
	Map<Vertex, Integer> stopIndices;
	private double maxWalkSpeed;
        private double maxWalkDistance;
	private transient int count;

	public WeightTable(Graph g) {
		this.g = g;
		// default max walk speed is biking speed
		//maxWalkSpeed = new TraverseOptions(TraverseMode.BICYCLE).speed;
		maxWalkSpeed = new TraverseOptions().speed;
	}

	public double getWeight(Vertex from, Vertex to) {
		int fi = stopIndices.get(from);
		int ti = stopIndices.get(to);
		return table[fi][ti];
	}

	public boolean includes(Vertex v) {
		return stopIndices.containsKey(v);
	}

	public synchronized void incrementCount() {
		count += 1;
		if (count % 1000 == 0)
			LOG.debug("TransitStop " + count + "/" + table.length);
	}

	// assignindices(Graph g)
	// update(origin, dest, newval)

	static class PoolableBinHeapFactory<T> implements PoolableObjectFactory {
		private int size;

		public PoolableBinHeapFactory(int size) {
			this.size = size;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void activateObject(Object heap) throws Exception {
			((BinHeap<T>) heap).reset();
		}

		@Override
		public void destroyObject(Object arg0) throws Exception {
		}

		@Override
		public Object makeObject() throws Exception {
			return new BinHeap<T>(size);
		}

		@Override
		public void passivateObject(Object arg0) throws Exception {
		}

		@Override
		public boolean validateObject(Object arg0) {
			return true;
		}

	}

	/**
	 * Build the weight table, parallelized according to the number of processors 
	 */
	public void buildTable() {
		ArrayList<TransitStop> stopVertices;

		LOG.debug("Number of vertices: " + g.getVertices().size());
		stopVertices = new ArrayList<TransitStop>();
		for (Vertex gv : g.getVertices())
			if (gv instanceof TransitStop)
				stopVertices.add((TransitStop) gv);
		int nStops = stopVertices.size();

		stopIndices = new IdentityHashMap<Vertex, Integer>(nStops);
		for (int i = 0; i < nStops; i++)
			stopIndices.put(stopVertices.get(i), i);
		LOG.debug("Number of stops: " + nStops);

		table = new float[nStops][nStops];
		for (float[] row : table)
			Arrays.fill(row, Float.POSITIVE_INFINITY);

		LOG.debug("Performing search at each transit stop.");

		int nThreads = Runtime.getRuntime().availableProcessors();
		LOG.debug("number of threads: " + nThreads);
		ArrayBlockingQueue<Runnable> taskQueue = new ArrayBlockingQueue<Runnable>(
				nStops);
		ThreadPoolExecutor threadPool = new ThreadPoolExecutor(nThreads,
				nThreads, 10, TimeUnit.SECONDS, taskQueue);
		GenericObjectPool heapPool = new GenericObjectPool(
				new PoolableBinHeapFactory<State>(g.getVertices().size()),
				nThreads);

		// make one heap and recycle it
		TraverseOptions options = new TraverseOptions();
		options.speed = maxWalkSpeed;
		final double MAX_WEIGHT = 60 * 60 * options.walkReluctance;
		final double OPTIMISTIC_BOARD_COST = options.boardCost;

		// create a task for each transit stop in the graph
		ArrayList<Callable<Void>> tasks = new ArrayList<Callable<Void>>();
		for (TransitStop origin : stopVertices) {
			SPTComputer task = new SPTComputer(heapPool, options, MAX_WEIGHT,
						OPTIMISTIC_BOARD_COST, origin);
			tasks.add(task);
		}
		try {
			//invoke all of tasks.
			threadPool.invokeAll(tasks);
			threadPool.shutdown();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		floyd();
	}

	/** 
	 * A callable that computes the shortest path tree out to MAX_WEIGHT for one vertex, updating the weight
	 * table as it goes.
	 * @author novalis
	 *
	 */
	class SPTComputer implements Callable<Void> {

		private GenericObjectPool heapPool;
		private TraverseOptions options;
		private double OPTIMISTIC_BOARD_COST;
		private double MAX_WEIGHT;		
		private TransitStop origin;

		SPTComputer(GenericObjectPool heapPool, TraverseOptions options,
				final double MAX_WEIGHT, final double OPTIMISTIC_BOARD_COST,
				TransitStop origin) {
			this.heapPool = heapPool;
			this.options = options;
			this.MAX_WEIGHT = MAX_WEIGHT;
			this.OPTIMISTIC_BOARD_COST = OPTIMISTIC_BOARD_COST;
			this.origin = origin;
		}
		
		@SuppressWarnings("unchecked")
		public Void call() throws Exception {
			// LOG.debug("ORIGIN " + origin);
			int oi = stopIndices.get(origin); // origin index
			// first check for walking transfers
			// LOG.debug("    Walk");

			BinHeap<State> heap;
			try {
				heap = (BinHeap<State>) heapPool.borrowObject();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

			BasicShortestPathTree spt = new BasicShortestPathTree(500000);
			State s0 = new State(origin, options);
			spt.add(s0);
			heap.insert(s0, s0.getWeight());
			while (!heap.empty()) {
				double w = heap.peek_min_key();
				State u = heap.extract_min();
				if (!spt.visit(u))
					continue;
				Vertex uVertex = u.getVertex();
				// LOG.debug("heap extract " + u + " weight " + w);
				if (w > MAX_WEIGHT)
					break;
				if (uVertex instanceof TransitStop) {
					int di = stopIndices.get(uVertex); // dest index
					table[oi][di] = (float) w;
					// LOG.debug("    Dest " + u + " w=" + w);
				}
				for (Edge e : uVertex.getOutgoing()) {
					if (!(e instanceof PreBoardEdge)) {
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
			while (!q.isEmpty()) {
				Vertex u = q.poll();
				for (Edge e : u.getOutgoing()) {
					if (e instanceof PatternBoard) {
						Vertex v = ((PatternBoard) e).getToVertex();
						// give onboard vertices same index as their
						// corresponding station
						stopIndices.put(v, oi);
						StateEditor se = (new State(u, options)).edit(e);
						se.incrementWeight(OPTIMISTIC_BOARD_COST);
						s0 = se.makeState();
						spt.add(s0);
						heap.insert(s0, s0.getWeight());
						// _log.debug("    board " + tov);
					} else if (e instanceof FreeEdge) { // handle preboard
						Vertex v = ((FreeEdge) e).getToVertex();
						// give onboard vertices same index as their
						// corresponding station
						stopIndices.put(v, oi);
						q.add(v);
					}
				}
			}
			// all boarding edges for this stop have now been traversed
			// LOG.debug("    Transit");
			while (!heap.empty()) {
				// check for transit stops when pulling off of heap
				// and continue when one is found
				// this is enough to prevent reboarding
				// need to mark closed vertices because otherwise cycles may
				// appear (interlining...)
				double w = heap.peek_min_key();
				State u = heap.extract_min();
				if (!spt.visit(u))
					continue;
				// LOG.debug("    Extract " + u + " w=" + w);
				Vertex uVertex = u.getVertex();
				if (uVertex instanceof TransitStop) {
					int di = stopIndices.get(uVertex); // dest index
					if (table[oi][di] > w) {
						table[oi][di] = (float) w;
						// LOG.debug("    Dest " + u + "w=" + w);
					}
					continue;
				}
				for (Edge e : uVertex.getOutgoing()) {
					// LOG.debug("        Edge " + e);
					State v = e.optimisticTraverse(u);
					if (v != null && spt.add(v))
						heap.insert(v, v.getWeight());
					// else LOG.debug("        (skip)");
				}
			}
			heapPool.returnObject(heap);
			incrementCount();
			return null;
		}
	}

	/* Find all pairs shortest paths */
	private void floyd() {
		LOG.debug("Floyd");
		int n = table.length;
		for (int k = 0; k < n; k++) {
			for (int i = 0; i < n; i++) {
				double ik = table[i][k];
				if (ik == Float.POSITIVE_INFINITY)
					continue;
				for (int j = 0; j < n; j++) {
					double kj = table[k][j];
					if (kj == Float.POSITIVE_INFINITY)
						continue;
					double ikj = ik + kj;
					double ij = table[i][j];
					if (ikj < ij)
						table[i][j] = (float) ikj;
				}
			}
			if (k % 50 == 0)
				LOG.debug("k=" + k + "/" + n);
		}
	}

	public void setMaxWalkSpeed(double maxWalkSpeed) {
		this.maxWalkSpeed = maxWalkSpeed;
	}

	public double getMaxWalkSpeed() {
		return maxWalkSpeed;
	}
	
	public void setMaxWalkDistance(double maxWalkDistance) {
	    this.maxWalkDistance = maxWalkDistance;
	}

        public double getMaxWalkDistance(double maxWalkDistance) {
            return this.maxWalkDistance;
        }
}
