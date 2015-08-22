package org.opentripplanner.streets;

import com.conveyal.osmlib.Node;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Column store might be better because 1. it is less fancy 2. it is auto-resizing (not fixed size).
 *
 * Edges come in pairs that have the same origin and destination vertices and the same geometries, but reversed.
 * Therefore many of the arrays are only half as big as the number of edges. All even numbered edges are forward, all
 * odd numbered edges are reversed.
 *
 * Typically, somewhat more than half of street segment edges have intermediate points (other than the two intersection
 * endpoints). Therefore it's more efficient to add a complete dense column for the intermediate point arrays, instead
 * of using a sparse hashmap to store values only for edges with intermediate points.
 */
public class EdgeStore implements Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(EdgeStore.class);
    private static final int DEFAULT_SPEED_KPH = 50;
    private static final int[] EMPTY_INT_ARRAY = new int[0];

    int nEdges = 0;
    protected TIntList flags;
    protected TIntList speeds;
    protected TIntList fromVertices;
    protected TIntList toVertices;
    protected TIntList lengths_mm;
    protected List<int[]> geometries; // intermediate points along the edge, other than the intersection endpoints

    public EdgeStore (int initialSize) {
        // There is one flags and speeds entry per edge.
        flags = new TIntArrayList(initialSize);
        speeds = new TIntArrayList(initialSize);
        // Vertex indices, geometries, and lengths are shared between pairs of forward and backward edges.
        int initialEdgePairs = initialSize / 2;
        fromVertices = new TIntArrayList(initialEdgePairs);
        toVertices = new TIntArrayList(initialEdgePairs);
        geometries = new ArrayList<>(initialEdgePairs);
        lengths_mm = new TIntArrayList(initialEdgePairs);
    }

    // Maybe reserve the first 4-5 bits (or a whole byte, and 16 bits for flags) for mutually exclusive edge types.
    // Maybe we should have trunk, secondary, tertiary, residential etc. as types 0...6
    // SIDEWALK(1),     CROSSING(2),     ROUNDABOUT(3),     ELEVATOR(4),     STAIRS(5),     PLATFORM(6),

    public static enum Flag {
        UNUSED(0),
        BIKE_PATH(1),
        SIDEWALK(2),
        CROSSING(3),
        ROUNDABOUT(4),
        ELEVATOR(5),
        STAIRS(6),
        PLATFORM(7),
        BOGUS_NAME(8),
        NO_THRU_TRAFFIC(9),
        SLOPE_OVERRIDE(10),
        TRANSIT_LINK(11), // This edge is a one-way connection from a street to a transit stop. Target is a transit stop index, not an intersection index.
        // Permissions
        ALLOWS_PEDESTRIAN(16),
        ALLOWS_BIKE(17),
        ALLOWS_CAR(18),
        ALLOWS_WHEELCHAIR(19);
        public final int flag;
        private Flag (int bitNumber) {
            flag = 1 << bitNumber;
        }
    }

    /**
     * This creates the bare topological edge pair with a length.
     * Flags, detailed geometry, etc. must be set using an edge cursor.
     * This avoids having a tangle of different edge creator functions for different circumstances.
     * @return a cursor pointing to the forward edge in the pair, which always has an even index.
     */
    public Edge addStreetPair(int beginVertexIndex, int endVertexIndex, int edgeLengthMillimeters) {

        // Store only one length, set of endpoints, and intermediate geometry per pair of edges.
        lengths_mm.add(edgeLengthMillimeters);
        fromVertices.add(beginVertexIndex);
        toVertices.add(endVertexIndex);
        geometries.add(EMPTY_INT_ARRAY);

        // Forward edge
        speeds.add(DEFAULT_SPEED_KPH);
        flags.add(0);

        // Backward edge
        speeds.add(DEFAULT_SPEED_KPH);
        flags.add(0);

        // Increment total number of edges created so far, and return the index of the first new edge.
        int forwardEdgeIndex = nEdges;
        nEdges += 2;
        return getCursor(forwardEdgeIndex);

    }

    @FunctionalInterface
    public static interface SegmentConsumer {
        public void consumeSegment (int index, int fixedLat0, int fixedLon0, int fixedLat1, int fixedLon1);
    }

    public void consumeGeometry (SegmentConsumer geometryConsumer) {
        int fixedLat;
        int fixedLon;
    }

    /** Inner class that serves as a cursor: points to a single edge in this store, and can be moved to other indexes. */
    public class Edge {

        int edgeIndex = -1;
        int pairIndex = -1;
        boolean isBackward = true;

        /**
         * Move the cursor forward one edge.
         * @return true if we have not advanced past the end of the list (there is an edge at the new position).
         */
        public boolean advance() {
            edgeIndex += 1;
            pairIndex = edgeIndex / 2;
            isBackward = !isBackward;
            return edgeIndex < nEdges;
        }

        /** Jump to a specific edge number. */
        public void seek(int pos) {
            edgeIndex = pos;
            // divide and multiply by two are fast bit shifts
            pairIndex = edgeIndex / 2;
            isBackward = (pairIndex * 2) != edgeIndex;
        }

        public int getFromVertex() {
            return isBackward ? toVertices.get(pairIndex) : fromVertices.get(pairIndex);
        }

        public int getToVertex() {
            return isBackward ? fromVertices.get(pairIndex) : toVertices.get(pairIndex);
        }

        public boolean getFlag(Flag flag) {
            return (flags.get(edgeIndex) & flag.flag) != 0;
        }

        public void setFlag(Flag flag) {
            flags.set(edgeIndex, flags.get(edgeIndex) | flag.flag);
        }

        public int getSpeed() {
            return speeds.get(edgeIndex);
        }

        public void setSpeed(int speed) {
            speeds.set(edgeIndex, speed);
        }

        public int getLengthMm () {
            return lengths_mm.get(pairIndex);
        }

        public boolean isBackward () {
            return isBackward;
        }

        public boolean isForward () {
            return !isBackward;
        }

        public StreetRouter.State traverse (StreetRouter.State s0) {
            StreetRouter.State s1 = new StreetRouter.State(getToVertex(), edgeIndex, s0);
            s1.nextState = null;
            s1.weight = s0.weight + getLengthMm() / 1000;
            return s1;
        }

        public void setGeometry (List<Node> nodes) {
            // The same empty int array represents all straight-line edges.
            if (nodes.size() <= 2) {
                geometries.set(pairIndex, EMPTY_INT_ARRAY);
                return;
            }
            if (isBackward) {
                LOG.warn("Setting a forward geometry on a back edge.");
            }
            // Create a geometry, which will be used for both forward and backward edge.
            int nIntermediatePoints = nodes.size() - 2;
            // Make a packed list of all coordinates between the endpoint intersections.
            int[] intermediateCoords = new int[nIntermediatePoints * 2];
            int i = 0;
            for (Node node : nodes.subList(1, nodes.size() - 1)) {
                intermediateCoords[i++] = node.fixedLat;
                intermediateCoords[i++] = node.fixedLon;
            }
            geometries.set(pairIndex, intermediateCoords);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Edge from %d to %d. Length %f meters, speed %d kph.",
                    getFromVertex(), getToVertex(), getLengthMm() / 1000D, getSpeed()));
            for (Flag flag : Flag.values()) {
                if (getFlag(flag)) {
                    sb.append(" ");
                    sb.append(flag.toString());
                }
            }
            return sb.toString();
        }
    }

    public Edge getCursor() {
        return new Edge();
    }

    public Edge getCursor(int pos) {
        Edge edge = new Edge();
        edge.seek(pos);
        return edge;
    }

    public void dump () {
        Edge edge = getCursor();
        for (int e = 0; e < nEdges; e++) {
            edge.seek(e);
            System.out.println(edge);
        }
    }


}
