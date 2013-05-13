/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.graph_builder.impl.map;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.extra_graph.EdgesForRoute;
import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.TransitIndexService;
import org.opentripplanner.routing.transit_index.RouteVariant;
import org.opentripplanner.util.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;

public class MapBuilder implements GraphBuilder {
    private static final Logger log = LoggerFactory.getLogger(MapBuilder.class);

    public List<String> provides() {
        return Arrays.asList("edge matching");
    }

    public List<String> getPrerequisites() {
        return Arrays.asList("streets", "transit", "transitIndex");
    }

    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
        TransitIndexService transit = graph.getService(TransitIndexService.class);

        StreetMatcher matcher = new StreetMatcher(graph);

        EdgesForRoute edgesForRoute = new EdgesForRoute();
        extra.put(EdgesForRoute.class, edgesForRoute);
        log.info("matching route variants to street edges...");
        for (AgencyAndId route : transit.getAllRouteIds()) {
            for (RouteVariant variant : transit.getVariantsForRoute(route)) {
                Geometry geometry = variant.getGeometry();
                if (variant.getTraverseMode() == TraverseMode.BUS) {
                    /* we can only match geometry to streets on bus routes */
                    log.debug("Matching: " + variant + " ncoords = " + geometry.getNumPoints());

                    List<Edge> edges = matcher.match(geometry);
                    if (edges == null) {
                        log.warn("Could not match " + variant.getName() + " to street network");
                        continue;
                    }

                    List<Coordinate> coordinates = new ArrayList<Coordinate>();
                    for (Edge e : edges) {
                        coordinates.addAll(Arrays.asList(e.getGeometry().getCoordinates()));
                        MapUtils.addToMapList(edgesForRoute.edgesForRoute, route, e);
                    }
                    Coordinate[] coordinateArray = new Coordinate[coordinates.size()];
                    LineString ls = GeometryUtils.getGeometryFactory().createLineString(coordinates.toArray(coordinateArray));
                    variant.setGeometry(ls);
                }
            }
        }
    }

    @Override
    public void checkInputs() {
        //no file inputs
    }
}