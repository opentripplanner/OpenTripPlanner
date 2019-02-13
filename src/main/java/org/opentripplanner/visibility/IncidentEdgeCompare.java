package org.opentripplanner.visibility;

import java.util.Comparator;

/**
 * Ported by David Turner from Visilibity, by Karl J. Obermeyer
 *
 *
 * This port undoubtedly introduced a number of bugs (and removed some features).
 *
 * Bug reports should be directed to the OpenTripPlanner project, unless they
 * can be reproduced in the original VisiLibity.
 *
 * This is *reversed* from the original since Java PQs are min-heaps while STL PQs are max-heaps
 */
class IncidentEdgeCompare implements Comparator<PolarEdge> {
    VLPoint observer_pointer;

    PolarPointWithEdgeInfo current_vertex_pointer;

    double epsilon;

    public IncidentEdgeCompare(VLPoint observer, PolarPointWithEdgeInfo current_vertex,
            double epsilon_temp) {

        observer_pointer = observer;
        current_vertex_pointer = current_vertex;
        epsilon = epsilon_temp;
    }

    public int compare(PolarEdge e1, PolarEdge e2) {
        PolarPoint k1, k2;
        LineSegment xing1 = new Ray(observer_pointer, current_vertex_pointer.bearing).intersection(
                new LineSegment(e1.first, e1.second), epsilon);

        LineSegment xing2 = new Ray(observer_pointer, current_vertex_pointer.bearing).intersection(
                new LineSegment(e2.first, e2.second), epsilon);
        if (xing1.size() > 0 && xing2.size() > 0) {
            k1 = new PolarPoint(observer_pointer, xing1.first());
            k2 = new PolarPoint(observer_pointer, xing2.first());
            return (int) Math.signum(k1.range - k2.range);
        }
        // Otherwise infeasible edges are given lower priority, so they
        // get pushed out the top of the priority_queue's (q2's)
        // heap.
        else if (xing1.size() == 0 && xing2.size() > 0)
            return -1;
        else if (xing1.size() > 0 && xing2.size() == 0)
            return 1;
        else
            return 0;
    }
}
