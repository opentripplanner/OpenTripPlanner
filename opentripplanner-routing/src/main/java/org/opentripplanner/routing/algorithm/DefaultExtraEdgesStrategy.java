package org.opentripplanner.routing.algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opentripplanner.routing.core.DirectEdge;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.location.StreetLocation;

public class DefaultExtraEdgesStrategy implements ExtraEdgesStrategy {

    @Override
    public Map<Vertex, List<Edge>> getIncomingExtraEdges(Vertex origin, Vertex target) {

        /* generate extra edges for StreetLocations */
        Map<Vertex, List<Edge>> extraEdges;
        if (origin instanceof StreetLocation) {
            extraEdges = new HashMap<Vertex, List<Edge>>();
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
        } else {
            extraEdges = Collections.emptyMap();
        }
        if (target instanceof StreetLocation) {
            if (extraEdges.isEmpty()) {
                extraEdges = new HashMap<Vertex, List<Edge>>();
            }
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

        return extraEdges;
    }

    @Override
    public Map<Vertex, List<Edge>> getOutgoingExtraEdges(Vertex origin, Vertex target) {

        /* generate extra edges for StreetLocations */
        Map<Vertex, List<Edge>> extraEdges;

        if (origin instanceof StreetLocation) {
            extraEdges = new HashMap<Vertex, List<Edge>>();
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
        } else {
            extraEdges = Collections.emptyMap();
        }

        if (target instanceof StreetLocation) {
            if (extraEdges.isEmpty()) {
                extraEdges = new HashMap<Vertex, List<Edge>>();
            }
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

        return extraEdges;
    }

}
