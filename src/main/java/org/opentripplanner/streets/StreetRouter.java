package org.opentripplanner.streets;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.pqueue.BinHeap;
import org.opentripplanner.streets.structs.StreetIntersection;
import org.opentripplanner.streets.structs.StreetSegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 *
 */
public class StreetRouter {

    private static final Logger LOG = LoggerFactory.getLogger(StreetRouter.class);

    private static final boolean DEBUG_OUTPUT = false;

    public static final int ALL_VERTICES = -1;

    private StreetLayer streetLayer;

    public int distanceLimitMeters = 5_000;

    TIntObjectMap<State> bestStates = new TIntObjectHashMap<>();

    BinHeap<State> queue = new BinHeap<>();

    public StreetRouter (StreetLayer streetLayer) {
        this.streetLayer = streetLayer;
    }

    boolean goalDirection = false;

    double targetLat, targetLon; // for goal direction heuristic

    public void route (int fromVertex, int toVertex) {
        long startTime = System.currentTimeMillis();
        bestStates.clear();
        queue.reset();
        State startState = new State(fromVertex);
        bestStates.put(fromVertex, startState);
        queue.insert(startState, 0);

        PrintStream printStream; // for debug output
        if (DEBUG_OUTPUT) {
            File debugFile = new File(String.format("%d-%d.csv", fromVertex, toVertex));
            OutputStream outputStream;
            try {
                outputStream = new BufferedOutputStream(new FileOutputStream(debugFile));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            printStream = new PrintStream(outputStream);
            printStream.println("lat,lon,weight");
        }

        if (toVertex > 0) {
            goalDirection = true;
            StreetIntersection intersection = streetLayer.vertices.get(toVertex);
            targetLat = intersection.getLat();
            targetLon = intersection.getLon();
        }
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
                StreetIntersection intersection = streetLayer.vertices.get(v0);
                printStream.printf("%f,%f,%d\n", intersection.getLat(), intersection.getLon(), s0.weight);
            }
            TIntList edgeList = streetLayer.outgoingEdges.get(v0);
            TIntIterator edgeIterator = edgeList.iterator();
            while (edgeIterator.hasNext()) {
                int edgeIndex = edgeIterator.next();
                StreetSegment segment = streetLayer.edges.get(edgeIndex);
                State s1 = segment.traverse(s0);
                if (!goalDirection && s1.weight > distanceLimitMeters) {
                    continue;
                }
                State existingBest = bestStates.get(s1.vertex);
                if (existingBest == null || existingBest.weight > s1.weight) {
                    bestStates.put(s1.vertex, s1);
                    int remainingWeight = goalDirection ? heuristic(s1) : 0;
                    queue.insert(s1, s1.weight + remainingWeight);
                }
            }
        }
        if (DEBUG_OUTPUT) {
            printStream.close();
        }
        //LOG.info("Routing produced {} states.", bestStates.size());
    }

    /**
     * Estimate remaining weight to destination. Must be an underestimate.
     */
    private int heuristic (State s) {
        StreetIntersection intersection = streetLayer.vertices.get(s.vertex);
        double lat = intersection.getLat();
        double lon = intersection.getLon();
        return (int)SphericalDistanceLibrary.fastDistance(lat, lon, targetLat, targetLon);
    }

    public static class State implements Cloneable {
        public int vertex;
        public int weight;
        public State backState; // previous state in the path chain
        public State nextState; // for multiple states at the same location
        public State (int vertex) {
            this.vertex = vertex;
        }
    }

}
