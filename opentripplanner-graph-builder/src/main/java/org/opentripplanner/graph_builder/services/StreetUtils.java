package org.opentripplanner.graph_builder.services;

import java.util.Collection;

import org.opentripplanner.routing.core.DeadEnd;
import org.opentripplanner.routing.core.GenericStreetIntersectionVertex;
import org.opentripplanner.routing.core.GenericVertex;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.Intersection;
import org.opentripplanner.routing.core.IntersectionVertex;

public class StreetUtils {


    public static void unify(Graph graph, Collection<Intersection> intersections) {
        for (Intersection intersection: intersections) {
            if (intersection.getDegree() == 1) {
                //a dead end can be represented more simply as a special case.
                IntersectionVertex v = intersection.vertices.get(0);
                DeadEnd d = new DeadEnd(v);
                graph.addVertex(d);
                graph.removeVertex(v);
            } else if (intersection.getDegree() == 2) {
                //this is an "intersection" of only two streets, and so can be simplified down
                //to a single vertex.
                IntersectionVertex v1 = intersection.vertices.get(0);
                GenericVertex generic = new GenericStreetIntersectionVertex(v1.getLabel(), v1.getX(), v1.getY(), v1.getName());
                for (IntersectionVertex v : intersection.vertices) {
                    if (v.inStreet != null) {
                        generic.addIncoming(v.inStreet);
                        v.inStreet.setToVertex(generic);
                    }
                    if (v.outStreet != null) {
                        generic.addOutgoing(v.outStreet);
                        v.outStreet.setFromVertex(generic);
                    }
                    graph.removeVertex(v);
                }
                graph.addVertex(generic);
            }
        }
    }

}
