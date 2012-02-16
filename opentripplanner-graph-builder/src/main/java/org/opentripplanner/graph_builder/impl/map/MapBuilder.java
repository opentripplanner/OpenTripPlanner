package org.opentripplanner.graph_builder.impl.map;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.TransitIndexService;
import org.opentripplanner.routing.transit_index.RouteVariant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

public class MapBuilder implements GraphBuilder {
    private static final Logger log = LoggerFactory.getLogger(MapBuilder.class);

    public void buildGraph(Graph graph) {
        TransitIndexService transit = graph.getService(TransitIndexService.class);

        StreetMatcher matcher = new StreetMatcher(graph);
        
        for (AgencyAndId route : transit.getAllRouteIds()) {
            for (RouteVariant variant : transit.getVariantsForRoute(route)) {
                Geometry geometry = variant.getGeometry();
                if (variant.getTraverseMode() == TraverseMode.BUS) {
                    /* we can only match geometry to streets on bus routes */
                    log.debug("Matching: " + variant + " ncoords = " + geometry.getNumPoints());

                    List<Edge> edges = matcher.match(geometry);
                    GeometryFactory gf = geometry.getFactory();
                    List<Coordinate> coordinates = new ArrayList<Coordinate>();
                    for (Edge e : edges) {
                        coordinates.addAll(Arrays.asList(e.getGeometry().getCoordinates()));
                    }
                    Coordinate[] coordinateArray = new Coordinate[coordinates.size()];
                    LineString ls = gf.createLineString(coordinates.toArray(coordinateArray));
                    variant.setGeometry(ls);
                }
            }
        }
    }
}