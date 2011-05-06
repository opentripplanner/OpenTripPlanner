package org.opentripplanner.routing.algorithm.strategies;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.opentripplanner.routing.core.DirectEdge;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.location.StreetLocation;

public class DefaultExtraEdgesStrategy implements ExtraEdgesStrategy {

    @Override
    public void addIncomingEdgesForOrigin(Map<Vertex, List<Edge>> extraEdges, Vertex origin) {
        if (origin instanceof StreetLocation) {
            Iterable<DirectEdge> extra = ((StreetLocation) origin).getExtra();
            for (DirectEdge edge : extra) {
                Vertex tov = edge.getToVertex();
                List<Edge> edges = extraEdges.get(tov);
                if (edges == null) {
                    edges = new ArrayList<Edge>();
                    extraEdges.put(tov, edges);
                }
                edges.add(edge);
            }
        }
    }

    @Override
    public void addIncomingEdgesForTarget(Map<Vertex, List<Edge>> extraEdges, Vertex target) {
        if (target instanceof StreetLocation) {
            Iterable<DirectEdge> extra = ((StreetLocation) target).getExtra();
            for (DirectEdge edge : extra) {
                Vertex tov = edge.getToVertex();
                List<Edge> edges = extraEdges.get(tov);
                if (edges == null) {
                    edges = new ArrayList<Edge>();
                    extraEdges.put(tov, edges);
                }
                edges.add(edge);
            }
        }
    }

    @Override
    public void addOutgoingEdgesForOrigin(Map<Vertex, List<Edge>> extraEdges, Vertex origin) {
        if (origin instanceof StreetLocation) {
            Iterable<DirectEdge> extra = ((StreetLocation) origin).getExtra();
            for (DirectEdge edge : extra) {
                Vertex fromv = edge.getFromVertex();
                List<Edge> edges = extraEdges.get(fromv);
                if (edges == null) {
                    edges = new ArrayList<Edge>();
                    extraEdges.put(fromv, edges);
                }
                edges.add(edge);
            }
        }
    }

    @Override
    public void addOutgoingEdgesForTarget(Map<Vertex, List<Edge>> extraEdges, Vertex target) {
        if (target instanceof StreetLocation) {
            Iterable<DirectEdge> extra = ((StreetLocation) target).getExtra();
            for (DirectEdge edge : extra) {
                Vertex fromv = edge.getFromVertex();
                List<Edge> edges = extraEdges.get(fromv);
                if (edges == null) {
                    edges = new ArrayList<Edge>();
                    extraEdges.put(fromv, edges);
                }
                edges.add(edge);
            }
        }
    }
}
