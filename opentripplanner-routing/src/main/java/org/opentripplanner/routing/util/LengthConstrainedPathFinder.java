package org.opentripplanner.routing.util;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.opentripplanner.common.IterableLibrary;
import org.opentripplanner.common.pqueue.BinHeap;
import org.opentripplanner.routing.edgetype.TurnEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TurnVertex;

/**
 * Path finder for use in debugging and testing the vehicle location inference
 * engine. Specs from Brandon Willard: Given an origin and destination edge,
 * find all paths of a given length that could connect the two. The path length
 * is to be interpreted as being exact, but some degree of tolerance is provided
 * by the fact that edges have length. The complexity is exponential in path
 * length. However, these methods will only be used over timesteps of around 30
 * seconds, so search space should still be limited in practice. The paths are
 * not necessarily needed - only the edges that make up those paths are
 * essential. This fact can be exploited to reduce complexity by not storing
 * back pointers and just keeping a set of edges touched. Rather than finding
 * all paths with some exact length, we can also just place an upper bound on
 * shortest path length, and include all edges that are in any such shortest
 * path. We may actually want to index turn *vertices* and use them to begin at
 * all turn edges in a bundle. 
 * 
 * Apparently skipping paths containing cycles is in fact not a win. Tracking which 
 * edges have been visited with a multiset doubles the run time, and runtime remains
 * almost unchanged when using a plain old hashset. There are just not that many 
 * solution paths with repeated loops to eliminate! 
 * 
 * This is likely because the length of loops must be "tuned" just right to hit the 
 * target edge given the distance constraint. In a naive recursive enumeration, these
 * useless loops would still be followed in circles until the target length is
 * exceeded. Our pre-computed lower bound on distance to the destination helps
 * prune paths containing repeated occurrences of "out of tune" loops before
 * they branch out of control.
 * 
 * @author abyrd
 */
public class LengthConstrainedPathFinder {

    //these actually represent being somewhere on the linestring, not points
    private final TurnVertex startVertex, targetVertex; 
    private final double targetLength, epsilon;
    private Set<State> solutions;
    // cached derived values
    private final boolean reverse; 
    public final Map<Vertex, Double> bounds;

    public LengthConstrainedPathFinder (Edge startEdge, Edge targetEdge, 
           double targetLength, double epsilon, boolean calculateBounds) {
        this((TurnVertex)(startEdge.getFromVertex()), (TurnVertex)(targetEdge.getFromVertex()), 
           targetLength, epsilon, calculateBounds);
        }

    public LengthConstrainedPathFinder (TurnVertex startVertex, TurnVertex targetVertex, 
           double targetLength, double epsilon, boolean calculateBounds) {
        this.startVertex = startVertex;
        this.targetVertex = targetVertex;
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
    private Map<Vertex, Double> findBounds() {
        Map<Vertex, Double> ret = new HashMap<Vertex, Double>();
        ret.put(targetVertex, 0.0); 
        BinHeap<Vertex> pq = new BinHeap<Vertex>();
        pq.insert(targetVertex, 0.0);
        while ( ! pq.empty()) {
            double length0 = pq.peek_min_key();
            Vertex v0 = pq.extract_min();
            // TODO handle PlainStreetEdges (or not?) they may already work right (before edge...)
            for (Edge edge : getTurns(v0, !reverse)) {
                double length1 = length0 + edge.getDistance();
                // sense intentionally swapped because this is a search backward from target
                Vertex v1 = reverse ? edge.getToVertex() : edge.getFromVertex();
                if (length1 < targetLength) {
                    Double existingLength = ret.get(v1);
                    if (existingLength == null || length1 < existingLength) {
                        ret.put(v1, length1);
                        pq.insert(v1, length1);
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
    private Iterable<Edge> getTurns(Vertex vertex, boolean reverse) {
        if (reverse)
            return vertex.getIncoming();
        else
            return vertex.getOutgoing();
    }
    
//    /** breadth-first search */
//    public Set<State> solveBreadthFirst() {
//        solutions = new HashSet<State>();
//        Queue<State> q = new LinkedList<State>();
//        q.add(new State(startEdge));
//        while ( ! q.isEmpty()) {
//            State s0 = q.poll();
//            //System.out.println(s0.toString());
//            if (s0.isSolution())
//                solutions.add(s0);
//            for (Edge edge : getAdjacentEdges(s0.edge, reverse)) {
//                State s1 = s0.traverse(edge);
//                if (s1.isCandidate())
//                    q.add(s1);
//            }
//        }
//        return getSolutions();
//    }

    /** recursive depth-first search using JVM stack */
    public Set<State> solveDepthFirst() {
        solutions = new HashSet<State>();
        depthFirst(new State(startVertex));
        return getSolutions();
    }

    private void depthFirst(State s0) {
        if (s0.isSolution())
            solutions.add(s0);
        for (Edge edge : getTurns(s0.vertex, reverse)) {
            State s1 = s0.traverse(edge);
            if (s1.isCandidate())
                depthFirst(s1);
        }        
    }
    
    public class State {
        
        final Vertex vertex; 
        final double minLength; // before traversing the edge, without initial/final edge fragments
        final State back;
        
        public State(TurnVertex tvertex) { 
            // make initial state. min distance should be 0 paths of length 2. 
            // neg is harmless because it is only used as a lower bound 
            this(tvertex, 0 - tvertex.getLength(), null);
        }
        
        private State(Vertex vertex, double minLength, State back) {
            this.vertex = vertex;
            this.minLength = minLength;
            this.back = back;
        }

        public List<Vertex> toVertexList() {
            List<Vertex> ret = new LinkedList<Vertex>();
            for (State s = this; s != null; s = s.back)
                ret.add(0, s.vertex);
            return ret;
        }

        private boolean isCandidate() {
            Double lBound = 0.0;
            if (bounds != null)
                lBound = bounds.get(this.vertex);
                if (lBound == null)
                    lBound = Double.POSITIVE_INFINITY;
            return this.minLength - epsilon + lBound < targetLength;
        }

        private double getMaxLength() {
            // TurnVertex has length because it actually represents being on a segment
            return this.minLength + startVertex.getLength() + targetVertex.getLength(); 
        }

        private boolean isSolution() {
            return vertex == targetVertex && 
                   this.minLength - epsilon <= targetLength && 
                   this.getMaxLength() + epsilon >= targetLength;
        }
        
        private State traverse(Edge edge) {
            return new State(edge.getToVertex(), this.minLength + edge.getDistance(), this);
        }
        
        public String toString() {
            return String.format("%5.0f %s", minLength, vertex);
        }

        public String toStringVerbose() {
            List<Vertex> vertices = this.toVertexList();
            return String.format("%5.0f %d %s", this.minLength, vertices.size(), vertices);
        }
    }
    
    public Map<Vertex, Double> pathProportions() {
        Map<Vertex, Double> vertexCounts = new HashMap<Vertex, Double>();
        for (State path : getSolutions()) {
            for (Vertex vertex : path.toVertexList()) {
                Double count = vertexCounts.get(vertex);
                if (count == null)
                    count = 0.0;
                vertexCounts.put(vertex, count + 1);
            }
        }
        int nPaths = getSolutions().size();
        List<Vertex> vs = new ArrayList<Vertex>(vertexCounts.keySet());
        for (Vertex v : vs) {
            Double count = vertexCounts.get(v);
            vertexCounts.put(v, count / nPaths);
        }
        return vertexCounts;
    }

}

