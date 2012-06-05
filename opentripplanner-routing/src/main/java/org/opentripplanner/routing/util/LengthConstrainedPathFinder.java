package org.opentripplanner.routing.util;

import java.security.InvalidParameterException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import org.opentripplanner.common.pqueue.BinHeap;
import org.opentripplanner.routing.graph.Edge;

/**
 * Path finder for use in debugging and testing the vehicle location inference
 * engine. 
 * 
 * Specs from Brandon Willard: Given an origin and destination edge,
 * find all paths of a given length that could connect the two. The path length is to
 * be interpreted as being exact, but some degree of tolerance is provided by the
 * fact that edges have length. If cycles are allowed in the solutions, the
 * complexity is exponential in path length. However, these methods will only be used over 
 * timesteps of around 30 seconds, so search space should still be limited in practice.
 * 
 * The paths are not necessarily needed - only the edges that make up those
 * paths are essential. This fact can be exploited to reduce complexity by not storing back
 * pointers and just keeping a set of edges touched. Rather than finding all
 * paths with some exact length, we can also just place an upper bound on shortest path
 * length, and include all edges that are in any such shortest path. 
 * 
 * We may actually want to index turn *vertices* and use them to begin at all turn edges in a bundle.
 * 
 * @author abyrd
 */
public class LengthConstrainedPathFinder {

    // search parameters
    private final Edge startEdge, targetEdge;
    private final double targetLength, epsilon;
    private Set<State> solutions;
    // cached derived values
    private final boolean reverse; 
    public final Map<Edge, Double> bounds;
    
    public LengthConstrainedPathFinder (Edge startEdge, Edge targetEdge, 
           double targetLength, double epsilon, boolean calculateBounds) {
        this.startEdge = startEdge;
        this.targetEdge = targetEdge;
        reverse = targetLength < 0;
        targetLength = Math.abs(targetLength);
        this.targetLength = targetLength;
        if (epsilon < 0)
            throw new InvalidParameterException();
        else
            this.epsilon = epsilon;
        this.bounds = calculateBounds ? findBounds() : null;
    }

    // use reverse search from the target edge to find lower bounds on distance for pruning
    private Map<Edge, Double> findBounds() {
        // lower bound on distance after traversing the edge
        Map<Edge, Double> ret = new HashMap<Edge, Double>();
        ret.put(targetEdge, 0.0); 
        BinHeap<Edge> pq = new BinHeap<Edge>();
        pq.insert(targetEdge, 0.0);
        while ( ! pq.empty()) {
            double length = pq.peek_min_key();
            Edge e0 = pq.extract_min();
            for (Edge e1 : getAdjacentEdges(e0, !reverse)) {
                double length1 = length + e1.getDistance();
                if (length1 < targetLength) {
                    Double existingLength = ret.get(e1);
                    if (existingLength == null || length1 < existingLength) {
                        ret.put(e1, length1);
                        pq.insert(e1, length1);
                    }
                }
            }
        }
        return ret;
    }

    /** returns cached solutions if any are present, otherwise finds them with DFS */
    public Set<State> getSolutions() {
        if (solutions == null)
            solveDepthFirst();
        return solutions;
    }
    
    /** accounts for negative target distances */
    private Iterable<Edge> getAdjacentEdges(Edge edge, boolean reverse) {
        if (reverse)
            return edge.getFromVertex().getIncoming();
        else
            return edge.getToVertex().getOutgoing();
    }
    
    /** breadth-first search */
    public Set<State> solveBreadthFirst() {
        solutions = new HashSet<State>();
        Queue<State> q = new LinkedList<State>();
        q.add(new State(startEdge));
        while ( ! q.isEmpty()) {
            State s0 = q.poll();
            //System.out.println(s0.toString());
            if (s0.isSolution())
                solutions.add(s0);
            for (Edge edge : getAdjacentEdges(s0.edge, reverse)) {
                State s1 = s0.traverse(edge);
                if (s1.isCandidate())
                    q.add(s1);
            }
        }
        return getSolutions();
    }

    /** recursive depth-first search using JVM stack */
    public Set<State> solveDepthFirst() {
        solutions = new HashSet<State>();
        depthFirst(new State(startEdge));
        return getSolutions();
    }

    private void depthFirst(State s0) {
        if (s0.isSolution())
            solutions.add(s0);
        for (Edge edge : getAdjacentEdges(s0.edge, reverse)) {
            State s1 = s0.traverse(edge);
            if (s1.isCandidate())
                depthFirst(s1);
        }        
    }
    
    public class State {
        
        final Edge edge; 
        final double minLength; // before traversing the edge, without initial/final edge fragments
        final State back;
        
        public State(Edge edge) { 
            // min distance should be 0 on 2-length paths
            // neg is harmless because it is only used as a lower bound 
            this(edge, 0 - edge.getDistance(), null);
        }
        
        private State(Edge edge, double minLength, State back) {
            this.edge = edge;
            this.minLength = minLength;
            this.back = back;
        }

        public List<Edge> toEdgeList() {
            List<Edge> ret = new LinkedList<Edge>();
            for (State s = this; s != null; s = s.back)
                ret.add(0, s.edge);
            return ret;
        }

        private boolean isCandidate() {
            Double lBound = 0.0;
            if (bounds != null)
                lBound = bounds.get(this.edge);
                if (lBound == null)
                    lBound = Double.POSITIVE_INFINITY;
            return this.minLength - epsilon + lBound < targetLength;
        }

        private double getMaxLength() {
            return this.minLength + startEdge.getDistance() + targetEdge.getDistance(); 
        }

        private boolean isSolution() {
            return edge == targetEdge && 
                   this.minLength - epsilon <= targetLength && 
                   this.getMaxLength() + epsilon >= targetLength;
        }
        
        private State traverse(Edge edge) {
            return new State(edge, this.minLength + this.edge.getDistance(), this);
        }
        
        public String toString() {
            return String.format("%5.0f %s", minLength, edge);
        }

        public String toStringVerbose() {
            List<Edge> edges = this.toEdgeList();
            return String.format("%5.0f %d %s", this.minLength, edges.size(), edges);
        }
    }

}

