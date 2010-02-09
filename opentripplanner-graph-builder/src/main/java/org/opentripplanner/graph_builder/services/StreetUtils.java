package org.opentripplanner.graph_builder.services;

import java.util.ArrayList;
import java.util.HashMap;

import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.Intersection;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.Street;
import org.opentripplanner.routing.edgetype.Turn;

import com.vividsolutions.jts.geom.Coordinate;

public class StreetUtils {

    public static void createTurnEdges(Graph graph,
            HashMap<Coordinate, ArrayList<Edge>> edgesByLocation) {
        // add turns
        for (ArrayList<Edge> edges : edgesByLocation.values()) {
            for (Edge in : edges) {
                Vertex tov = in.getToVertex();
                Coordinate c = tov.getCoordinate();
                ArrayList<Edge> outEdges = edgesByLocation.get(c);
                if (outEdges != null) {
                    /* If this is not an intersection or street name change, unify the vertices */
                    boolean unified = false;

                    /*
                     * Some physical (oneway) streets may be represented by one edge only
                     * (since no parallel/coincident edge is needed) in which case the vertices should
                     * still be unified. (This also results in lone vertices at the end of ways being unified,
                     * but that doesn't hurt anyone.)
                     */
                    if (outEdges.size() == 1) {
                        Edge out = outEdges.get(0);
                        Vertex fromVertex = out.getFromVertex();
                        if (tov != fromVertex && out.getName() == in.getName()) {
                            Intersection v = (Intersection) tov;
                            v.mergeFrom(graph, (Intersection) fromVertex);
                            graph.removeVertex(fromVertex);
                            unified = true;
                        }
                    }
                    else if (outEdges.size() == 2) {
                        /*
                         * Checking only the number of edges leaving a certain point isn't enough
                         * to determine whether there is an intersection:
                         * If a (non-pedestrian) oneway street forks into two oneway streets, than there
                         * are two edges leaving the intersection, but unifying the vertices is not the
                         * right thing to do. (And the name might not even change, if the edges are
                         * iterated over in a suitable manner)
                         *
                         * So, for unification to ensue, it also has to be ensured that one of the
                         * two outgoing edges is parallel/coincident to the examined edge.
                         */
                        if(outEdges.get(0).getToVertex() == in.getFromVertex() || outEdges.get(1).getToVertex() == in.getFromVertex()) {
                            for (Edge out : outEdges) {
                                Vertex fromVertex = out.getFromVertex();
                                if (tov != fromVertex && out.getName() == in.getName()) {
                                    Intersection v = (Intersection) tov;
                                    v.mergeFrom(graph, (Intersection) fromVertex);
                                    graph.removeVertex(fromVertex);
                                    unified = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (!unified) {
                        for (Edge out : outEdges) {
                            /*
                             * Only create a turn edge if: (a) the edge is not the one we are
                             * coming from (b) the edge is a Street (c) the edge is an outgoing
                             * edge from this location
                             */
                            if (tov != out.getFromVertex() && out instanceof Street
                                    && out.getFromVertex().getCoordinate().equals(c)) {
                                graph.addEdge(new Turn(in, out));
                            }
                        }
                    }
                }
            }
        }
    }

}
