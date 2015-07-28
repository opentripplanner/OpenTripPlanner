package org.opentripplanner.streets;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.io.Serializable;

/**
 * Column store might be better because 1. it is less fancy 2. it is auto-resizing (not fixed size)
 */
public class EdgeStore implements Serializable {

    private static int DEFAULT_SPEED_KPH = 50;
    int nEdges = 0;
    protected TIntList fromVertices;
    protected TIntList toVertices;
    protected TIntList flags;
    protected TIntList lengths_mm;
    protected TIntList speeds;

    public EdgeStore (int initialSize) {
        fromVertices = new TIntArrayList(initialSize);
        toVertices = new TIntArrayList(initialSize);
        flags = new TIntArrayList(initialSize);
        lengths_mm = new TIntArrayList(initialSize);
        speeds = new TIntArrayList(initialSize);
    }

    // Maybe reserve the first 4-5 bits (or a whole byte, and 16 bits for flags) for mutually exclusive edge types.
    // Maybe we should have trunk, secondary, tertiary, residential etc. as types 0...6
    // SIDEWALK(1),     CROSSING(2),     ROUNDABOUT(3),     ELEVATOR(4),     STAIRS(5),     PLATFORM(6),

    public static enum Flag {
        BACKWARD(0), // This edge's geometry should be interpreted backward
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

    public int addStreetPair(int beginVertexIndex, int endVertexIndex, double lengthMeters) {

        // Forward edge
        int forwardEdgeId = nEdges++;
        fromVertices.add(beginVertexIndex);
        toVertices.add(endVertexIndex);
        lengths_mm.add((int)(lengthMeters * 1000));
        speeds.add(DEFAULT_SPEED_KPH);
        flags.add(0);

        // Backward edge
        int backEdgeId = nEdges++;
        fromVertices.add(endVertexIndex);
        toVertices.add(beginVertexIndex);
        lengths_mm.add((int)(lengthMeters * 1000));
        speeds.add(DEFAULT_SPEED_KPH);
        flags.add(Flag.BACKWARD.flag);

        return forwardEdgeId;
    }

    public class Edge {

        int index = -1;

        // Return true if we have not advanced past the end of the list (there is data to read).
        public boolean advance() {
            index += 1;
            return index < nEdges;
        }

        public void seek(int pos) {
            this.index = pos;
        }

        public int getFromVertex() {
            return fromVertices.get(index);
        }

        public void setFromVertex(int fromVertex) {
            fromVertices.set(index, fromVertex);
        }

        public int getToVertex() {
            return toVertices.get(index);
        }

        public void setToVertex(int toVertex) {
            toVertices.set(index, toVertex);
        }

        public boolean getFlag(Flag flag) {
            return (flags.get(index) & flag.flag) != 0;
        }

        public void setFlag(Flag flag) {
            flags.set(index, flags.get(index) | flag.flag);
        }

        public int getSpeed() {
            return speeds.get(index);
        }

        public void setSpeed(int speed) {
            speeds.set(index, speed);
        }

        public int getLengthMm () {
            return lengths_mm.get(index);
        }

        public void setLength(double meters) {
            lengths_mm.set(index, (int)(meters * 1000));
        }

        public StreetRouter.State traverse (StreetRouter.State s0) {
            StreetRouter.State s1 = new StreetRouter.State(getToVertex(), index, s0);
            s1.nextState = null;
            s1.weight = s0.weight + getLengthMm() / 1000;
            return s1;
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
