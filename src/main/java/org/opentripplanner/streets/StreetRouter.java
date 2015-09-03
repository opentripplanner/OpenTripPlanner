package org.opentripplanner.streets;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.pqueue.BinHeap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * This routes over the street layer of a TransitNetwork.
 * It is a throw-away calculator object that retains routing state and after the search is finished.
 * Additional functions are called to retrieve the routing results from that state.
 */
public class StreetRouter {

    private static final Logger LOG = LoggerFactory.getLogger(StreetRouter.class);

    private static final boolean DEBUG_OUTPUT = false;

    public static final int ALL_VERTICES = -1;

    public final StreetLayer streetLayer;

    public int distanceLimitMeters = 2_000;

    TIntObjectMap<State> bestStates = new TIntObjectHashMap<>();

    BinHeap<State> queue = new BinHeap<>();

    boolean goalDirection = false;

    double targetLat, targetLon; // for goal direction heuristic

    // If you set this to a non-negative number, the search will be directed toward that vertex .
    public int toVertex = ALL_VERTICES;

    /**
     * @return a map from transit stop indexes to their distances from the origin.
     * Note that the TransitLayer contains all the information about which street vertices are transit stops.
     */
    public TIntIntMap getReachedStops() {
        TIntIntMap result = new TIntIntHashMap();
        // Convert stop vertex indexes in street layer to transit layer stop indexes.
        bestStates.forEachEntry((vertexIndex, state) -> {
            int stopIndex = streetLayer.linkedTransitLayer.stopForStreetVertex.get(vertexIndex);
            // -1 indicates no value, this street vertex is not a transit stop
            if (stopIndex >= 0) {
                result.put(stopIndex, state.weight);
            }
            return true; // continue iteration
        });
        return result;
    }

    /** Return a map of all the reached vertices to their distances from the origin */
    public TIntIntMap getReachedVertices () {
        TIntIntMap result = new TIntIntHashMap();
        bestStates.forEachEntry((vidx, state) -> {
            result.put(vidx, state.weight);
            return true; // continue iteration
        });
        return result;
    }

    /**
     * Get a distance table to all street vertices touched by the last search operation on this StreetRouter.
     * @return A packed list of (vertex, distance) for every reachable street vertex.
     * This is currently returning the weight, which is the distance in meters.
     */
    public int[] getStopTree () {
        TIntList result = new TIntArrayList(bestStates.size() * 2);
        // Convert stop vertex indexes in street layer to transit layer stop indexes.
        bestStates.forEachEntry((vertexIndex, state) -> {
            result.add(vertexIndex);
            result.add(state.weight);
            return true; // continue iteration
        });
        return result.toArray();
    }

    public StreetRouter (StreetLayer streetLayer) {
        this.streetLayer = streetLayer;
    }

    /**
     * @param lat Latitude in floating point (not fixed int) degrees.
     * @param lon Longitude in flating point (not fixed int) degrees.
     */
    public void setOrigin (double lat, double lon) {
        Split split = streetLayer.findSplit(lat, lon, 300);
        if (split == null) {
            LOG.info("No street was found near the specified origin point of {}, {}.", lat, lon);
            return;
        }
        bestStates.clear();
        queue.reset();
        State startState0 = new State(split.vertex0, -1, null);
        State startState1 = new State(split.vertex1, -1, null);
        // TODO walk speed, assuming 1 m/sec currently.
        startState0.weight = split.distance0_mm / 1000;
        startState1.weight = split.distance1_mm / 1000;
        bestStates.put(split.vertex0, startState0);
        bestStates.put(split.vertex1, startState1);
        queue.insert(startState0, startState0.weight);
        queue.insert(startState1, startState1.weight);
    }

    public void setOrigin (int fromVertex) {
        bestStates.clear();
        queue.reset();
        State startState = new State(fromVertex, -1, null);
        bestStates.put(fromVertex, startState);
        queue.insert(startState, 0);
    }

    /**
     * Call one of the setOrigin functions first.
     */
    public void route () {

        if (bestStates.size() == 0 || queue.size() == 0) {
            LOG.warn("Routing without first setting an origin, no search will happen.");
        }

        PrintStream printStream; // for debug output
        if (DEBUG_OUTPUT) {
            File debugFile = new File(String.format("street-router-debug.csv"));
            OutputStream outputStream;
            try {
                outputStream = new BufferedOutputStream(new FileOutputStream(debugFile));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            printStream = new PrintStream(outputStream);
            printStream.println("lat,lon,weight");
        }

        // Set up goal direction if a to vertex was supplied.
        if (toVertex > 0) {
            goalDirection = true;
            VertexStore.Vertex vertex = streetLayer.vertexStore.getCursor(toVertex);
            targetLat = vertex.getLat();
            targetLon = vertex.getLon();
        }

        EdgeStore.Edge edge = streetLayer.edgeStore.getCursor();
        while (!queue.empty()) {
            State s0 = queue.extract_min();
            if (bestStates.get(s0.vertex) != s0) {
                continue; // state was dominated after being enqueued
            }
            int v0 = s0.vertex;
            if (goalDirection && v0 == toVertex) {
                LOG.debug("Found destination vertex. Tree size is {}.", bestStates.size());
                break;
            }
            if (DEBUG_OUTPUT) {
                VertexStore.Vertex vertex = streetLayer.vertexStore.getCursor(v0);
                printStream.printf("%f,%f,%d\n", vertex.getLat(), vertex.getLon(), s0.weight);
            }
            TIntList edgeList = streetLayer.outgoingEdges.get(v0);
            edgeList.forEach(edgeIndex -> {
                edge.seek(edgeIndex);
                State s1 = edge.traverse(s0);
                if (!goalDirection && s1.weight > distanceLimitMeters) {
                    return true; // Iteration over edges should continue.
                }
                State existingBest = bestStates.get(s1.vertex);
                if (existingBest == null || existingBest.weight > s1.weight) {
                    bestStates.put(s1.vertex, s1);
                }
                int remainingWeight = goalDirection ? heuristic(s1) : 0;
                queue.insert(s1, s1.weight + remainingWeight);
                return true; // Iteration over edges should continue.
            });
        }
        if (DEBUG_OUTPUT) {
            printStream.close();
        }
    }

    /**
     * Estimate remaining weight to destination. Must be an underestimate.
     */
    private int heuristic (State s) {
        VertexStore.Vertex vertex = streetLayer.vertexStore.getCursor(s.vertex);
        double lat = vertex.getLat();
        double lon = vertex.getLon();
        return (int)SphericalDistanceLibrary.fastDistance(lat, lon, targetLat, targetLon);
    }

    public int getTravelTimeToVertex (int vertexIndex) {
        State state = bestStates.get(vertexIndex);
        if (state == null) {
            return Integer.MAX_VALUE; // Unreachable
        }
        return state.weight; // TODO true walk speed
    }

    public static class State implements Cloneable {
        public int vertex;
        public int weight;
        public int backEdge;
        public State backState; // previous state in the path chain
        public State nextState; // next state at the same location (for turn restrictions and other cases with co-dominant states)
        public State (int atVertex, int viaEdge, State backState) {
            this.vertex = atVertex;
            this.backEdge = viaEdge;
            this.backState = backState;
        }
    }

}
