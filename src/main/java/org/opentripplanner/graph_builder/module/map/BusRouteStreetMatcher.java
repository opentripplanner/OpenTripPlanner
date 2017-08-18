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

package org.opentripplanner.graph_builder.module.map;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import org.onebusaway.gtfs.model.Route;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;

/**
 * Uses the shapes from GTFS to determine which streets buses drive on. This is used to improve the quality of
 * the stop-to-street linkage. It encourages the linker to link to streets where transit actually travels.
 *
 * GTFS provides a mapping from trips->shapes. This module provides a mapping from stops->trips and shapes->edges.
 * Then transitively we get a mapping from stop->edges.
 * The edges that "belong" to a stop are favored when linking that stop to the street network.
 */
public class BusRouteStreetMatcher implements GraphBuilderModule {
    private static final Logger log = LoggerFactory.getLogger(BusRouteStreetMatcher.class);

    public List<String> provides() {
        return Arrays.asList("edge matching");
    }

    public List<String> getPrerequisites() {
        return Arrays.asList("streets", "transit");
    }

    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {

        //Mapbuilder needs transit index
        graph.index(new DefaultStreetVertexIndexFactory());

        StreetMatcher matcher = new StreetMatcher(graph);
        log.info("Finding corresponding street edges for trip patterns...");
        // Why do we need to iterate over the routes? Why not just patterns?
        int nPatterns = 0;
        int nHops = 0;
        for (Route route : graph.index.routeForId.values()) {
            for (TripPattern pattern : graph.index.patternsForRoute.get(route)) {
                //If there are no shapes in GTFS pattern geometry is generated
                //generated geometry is useless for street matching
                //that is why pattern.geometry is null in that case
                if (pattern.mode == TraverseMode.BUS && pattern.geometry != null) {
                    /* we can only match geometry to streets on bus routes */
                    log.debug("Matching {}", pattern);
                    // Replace the pattern's geometry from GTFS with that of the equivalent OSM edges.
                    LineString ls = getMatchedGeometry(matcher, pattern.geometry, pattern.toString());
                    if (ls != null) {
                        pattern.geometry = ls;
                        nPatterns++;
                    }
                    for (PatternHop hop : pattern.getPatternHops()) {
                        ls = getMatchedGeometry(matcher, hop.getGeometry(), hop.toString());
                        if (ls != null) {
                            hop.setGeometry(ls);
                            nHops++;
                        }
                    }
                }
            }
        }
        log.info("Replaced {} pattern geometries and {} hop geometries", nPatterns, nHops);
    }

    private static LineString getMatchedGeometry(StreetMatcher matcher, LineString geometry, String id) {
        List<Edge> edges = matcher.match(geometry);
        if (edges == null || edges.isEmpty()) {
            log.warn("Could not match to street network: {}", id);
            return null;
        }
        List<Coordinate> coordinates = new ArrayList<Coordinate>();
        for (Edge e : edges) {
            coordinates.addAll(Arrays.asList(e.getGeometry().getCoordinates()));
        }
        Coordinate[] coordinateArray = new Coordinate[coordinates.size()];
        return GeometryUtils.getGeometryFactory().createLineString(coordinates.toArray(coordinateArray));
    }

    @Override
    public void checkInputs() {
        //no file inputs
    }
}
