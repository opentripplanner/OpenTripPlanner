package org.opentripplanner.streets.structs;

import org.nustaq.offheap.structs.FSTStruct;
import org.opentripplanner.streets.StreetRouter.State;

/**
 * https://github.com/RuedigerMoeller/fast-serialization/wiki/Structs
 */
public class StreetSegment extends FSTStruct {

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

    protected int fromVertex;
    protected int toVertex;
    protected int flags;
    protected int length_mm;
    protected int speed;

    public int getFromVertex() {
        return fromVertex;
    }

    public void setFromVertex(int fromVertex) {
        this.fromVertex = fromVertex;
    }

    public int getToVertex() {
        return toVertex;
    }

    public void setToVertex(int toVertex) {
        this.toVertex = toVertex;
    }

    public boolean getFlag(Flag flag) {
        return (flags & flag.flag) != 0;
    }

    public void setFlag(Flag flag) {
        this.flags |= flag.flag;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public void setLength(double meters) {
        this.length_mm = (int)(meters * 1000);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Edge from %d to %d. Length %f meters, speed %d kph.",
                fromVertex, toVertex, length_mm / 1000D, speed));
        for (Flag flag : Flag.values()) {
            if (getFlag(flag)) {
                sb.append(" ");
                sb.append(flag.toString());
            }
        }
        return sb.toString();
    }

    public State traverse (State s0) {
        State s1 = new State(toVertex);
        s1.backState = s0;
        s1.nextState = null;
        s1.weight = s0.weight + length_mm / 1000;
        return s1;
    }

}
