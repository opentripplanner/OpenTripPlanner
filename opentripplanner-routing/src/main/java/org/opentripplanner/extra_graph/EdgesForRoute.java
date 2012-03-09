package org.opentripplanner.extra_graph;

import java.util.HashMap;
import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.graph.Edge;

public class EdgesForRoute {
    public HashMap<AgencyAndId, List<Edge>> edgesForRoute = new HashMap<AgencyAndId, List<Edge>>();

    public List<Edge> get(AgencyAndId route) {
        return edgesForRoute.get(route);
    }
}
