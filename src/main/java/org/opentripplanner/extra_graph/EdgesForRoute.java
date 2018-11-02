package org.opentripplanner.extra_graph;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.opentripplanner.model.Route;
import org.opentripplanner.routing.graph.Edge;

import java.util.Collection;

public class EdgesForRoute {
    public Multimap<Route, Edge> edgesForRoute = ArrayListMultimap.create();

    public Collection<Edge> get(Route route) {
        return edgesForRoute.get(route);
    }
}
