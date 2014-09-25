package org.opentripplanner.osm;

public class Edge {
    long way;
    long from;
    long to;
    int mask;

    /** Overridden to return true in edges running the opposite direction. */
    // Arrange edges in pairs: forward and back, so you always know where the companion edge is.
//    public boolean isBack() {
//        return false;
//    }
}
