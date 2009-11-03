package org.opentripplanner.jags.algorithm.kao;

import java.util.ArrayList;
import java.util.Date;

import org.opentripplanner.jags.core.Edge;
import org.opentripplanner.jags.core.Vertex;
import org.opentripplanner.jags.edgetype.Hop;

public class Kao {
    public static Tree find(KaoGraph graph, Date startTime, Vertex startVertex, long window) {
        Tree tree = new Tree();
        tree.setParent(startVertex, null);

        ArrayList<EdgeOption> edgeoptions = graph.sortedEdges(startTime, window);

        for (EdgeOption eo : edgeoptions) {
            Edge segment = eo.edge;

            int segmentStartTime = ((Hop) segment.payload).getStartStopTime().getDepartureTime();
            Vertex segmentOrig = segment.fromv;
            Vertex segmentDest = segment.tov;

            if (tree.containsVertex(segmentOrig) && !tree.containsVertex(segmentDest)) {
                Edge parentSegment = tree.getParent(segment.fromv);

                if (parentSegment == null) {
                    tree.setParent(segmentDest, segment);
                    continue;
                }

                int parentSegmentEndTime = ((Hop) parentSegment.payload).getEndStopTime()
                        .getArrivalTime();

                if (segmentStartTime >= parentSegmentEndTime) {
                    tree.setParent(segmentDest, segment);
                }
            }
        }
        return tree;
    }
}
