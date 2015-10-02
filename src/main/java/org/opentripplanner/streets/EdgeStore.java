package org.opentripplanner.streets;

import com.conveyal.osmlib.Node;
import com.vividsolutions.jts.geom.Envelope;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Column store is better than struct simulation because 1. it is less fancy, 2. it is auto-resizing (not fixed size),
 * 3. I've tried both and they're the same speed.
 *
 * Edges come in pairs that have the same origin and destination vertices and the same geometries, but reversed.
 * Therefore many of the arrays are only half as big as the number of edges. All even numbered edges are forward, all
 * odd numbered edges are reversed.
 *
 * Typically, somewhat more than half of street segment edges have intermediate points (other than the two intersection
 * endpoints). Therefore it's more efficient to add a complete dense column for the intermediate point arrays, instead
 * of using a sparse hashmap to store values only for edges with intermediate points.
 *
 * For geometry storage I tried several methods. Full Netherlands load in 4GB of heap:
 * Build time is around 8 minutes. 2.5-3GB was actually in use.
 * List of int arrays: 246MB serialized, 5.2 sec write, 6.3 sec read.
 * List of int arrays, full lists (not only intermediates): 261MB, 6.1 sec write, 6.2 sec read.
 * Indexes into single contiguous int array: 259MB, 5 sec write, 7.5 sec read.
 * Currently I am using the first option as it is both readable and efficient.
 */
public class EdgeStore implements Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(EdgeStore.class);
    private static final int DEFAULT_SPEED_KPH = 50;
    private static final int[] EMPTY_INT_ARRAY = new int[0];

    // The vertices that are referred to in these edges
    private VertexStore vertexStore;

    int nEdges = 0;

    /** Flags for this edge.  One entry for each forward and each backward edge. */
    protected TIntList flags;

    /** Speed for this edge.  One entry for each forward and each backward edge. */
    protected TIntList speeds;

    /** From vertices. One entry for each edge pair */
    protected TIntList fromVertices;

    /** To vertices. One entry for each edge pair */
    protected TIntList toVertices;

    /** Length (millimeters). One entry for each edge pair */
    protected TIntList lengths_mm;

    /** Geometries. One entry for each edge pair */
    protected List<int[]> geometries; // intermediate points along the edge, other than the intersection endpoints

    public EdgeStore (VertexStore vertexStore, int initialSize) {
        this.vertexStore = vertexStore;
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

    /** Remove the specified edges from this edge store */
    public void remove (int[] edgesToOmit) {
        // be clever: sort the list descending, because removing an edge only affects the indices of
        // edges that appear later in the graph.
        Arrays.sort(edgesToOmit);

        int prevEdge = -1;
        for (int cursor = edgesToOmit.length - 1; cursor >= 0; cursor--) {
            int edge = edgesToOmit[cursor] / 2;

            if (edge == prevEdge)
                continue; // ignore duplicates

            prevEdge = edge;
            // note using 2 int version because it is an offset not a value that we want to remove
            // flags and speeds have separate entries for forward and backwards edges
            flags.remove(edge * 2, 2);
            speeds.remove(edge * 2, 2);

            // everything else has a single entry for forward and backward edges
            fromVertices.remove(edge, 1);
            toVertices.remove(edge, 1);
            lengths_mm.remove(edge, 1);
            // this is not a TIntList
            geometries.remove(edge);
            nEdges -= 2;
        }
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
            if (pos < 0)
                throw new ArrayIndexOutOfBoundsException("Attempt to seek to negative edge number");
            if (pos >= nEdges)
                throw new ArrayIndexOutOfBoundsException("Attempt to seek beyond end of edge store");

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

        /**
         * NOTE that this will have an effect on both edges in the bidirectional edge pair.
         */
        public void setToVertex(int toVertexIndex) {
            if (isBackward) {
                fromVertices.set(pairIndex, toVertexIndex);
            } else {
                toVertices.set(pairIndex, toVertexIndex);
            }
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

        /**
         * Set the length for the current edge pair (always the same in both directions).
         */
        public void setLengthMm (int millimeters) {
            lengths_mm.set(pairIndex, millimeters);
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

        /**
         * Call a function on every segment in this edges's geometry.
         * Always iterates forward over the geometry, whether we are on a forward or backward edge.
         */
        public void forEachSegment (SegmentConsumer segmentConsumer) {
            VertexStore.Vertex vertex = vertexStore.getCursor(fromVertices.get(pairIndex));
            int prevFixedLat = vertex.getFixedLat();
            int prevFixedLon = vertex.getFixedLon();
            int[] intermediates = geometries.get(pairIndex);
            int s = 0;
            int i = 0;
            while (i < intermediates.length) {
                int fixedLat = intermediates[i++];
                int fixedLon = intermediates[i++];
                segmentConsumer.consumeSegment(s, prevFixedLat, prevFixedLon, fixedLat, fixedLon);
                prevFixedLat = fixedLat;
                prevFixedLon = fixedLon;
                s++;
            }
            vertex.seek(toVertices.get(pairIndex));
            segmentConsumer.consumeSegment(s, prevFixedLat, prevFixedLon, vertex.getFixedLat(), vertex.getFixedLon());
        }


        /**
         * Call a function for every point on this edge's geometry, including the beginning end end points.
         * Always iterates forward over the geometry, whether we are on a forward or backward edge.
         */
        public void forEachPoint (PointConsumer pointConsumer) {
            VertexStore.Vertex vertex = vertexStore.getCursor(fromVertices.get(pairIndex));
            int p = 0;
            pointConsumer.consumePoint(p++, vertex.getFixedLat(), vertex.getFixedLon());
            int[] intermediates = geometries.get(pairIndex);
            int i = 0;
            while (i < intermediates.length) {
                pointConsumer.consumePoint(p++, intermediates[i++], intermediates[i++]);
            }
            vertex.seek(toVertices.get(pairIndex));
            pointConsumer.consumePoint(p, vertex.getFixedLat(), vertex.getFixedLon());
        }

        /** @return an envelope around the whole edge geometry. */
        public Envelope getEnvelope() {
            Envelope envelope = new Envelope();
            forEachPoint((p, fixedLat, fixedLon) -> {
                envelope.expandToInclude(fixedLon, fixedLat);
            });
            return envelope;
        }

        /**
         * @return the number of segments in the geometry of the current edge.
         */
        public int nSegments () {
            int[] geom = geometries.get(pairIndex);
            if (geom != null) {
                // Number of packed lat-lon pairs plus the final segment.
                return (geom.length / 2) + 1;
            } else {
                // A single segment from the initial vertex to the final vertex.
                return 1;
            }
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

    /** A functional interface that consumes segments in a street geometry one by one. */
    @FunctionalInterface
    public static interface SegmentConsumer {
        public void consumeSegment (int index, int fixedLat0, int fixedLon0, int fixedLat1, int fixedLon1);
    }

    /** A functional interface that consumes the points in a street geometry one by one. */
    @FunctionalInterface
    public static interface PointConsumer {
        public void consumePoint (int index, int fixedLat, int fixedLon);
    }

    public void dump () {
        Edge edge = getCursor();
        for (int e = 0; e < nEdges; e++) {
            edge.seek(e);
            System.out.println(edge);
        }
    }


}
