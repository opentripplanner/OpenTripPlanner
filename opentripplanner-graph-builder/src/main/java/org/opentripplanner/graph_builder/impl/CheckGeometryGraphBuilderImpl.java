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

package org.opentripplanner.graph_builder.impl;

import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.ShapePoint;
import org.onebusaway.gtfs.serialization.GtfsReader;
import org.opentripplanner.graph_builder.services.EntityReplacementStrategy;
import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.GraphVertex;
import org.opentripplanner.routing.core.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

/**
 * {@link GraphBuilder} plugin that supports adding transit network data from a GTFS feed to the
 * routing {@link Graph}.
 * 
 * Supports reading from multiple {@link GtfsReader} instances sequentially with respect to GTFS
 * entity classes. That is to say, given three feeds A, B, and C, all {@link Agency} entities will
 * be read from A, B, and C in turn, and then all {@link ShapePoint} entities will be read from A,
 * B, and C in turn, and so forth. This sequential reading scheme allows for cases where two
 * separate feeds may have cross-feed references (ex. StopTime => Stop) as facilitated by the use of
 * an {@link EntityReplacementStrategy}.
 * 
 * @author bdferris
 * 
 */
public class CheckGeometryGraphBuilderImpl implements GraphBuilder {

    private final Logger _log = LoggerFactory.getLogger(CheckGeometryGraphBuilderImpl.class);

    @Override
    public void buildGraph(Graph graph) {
        for (GraphVertex gv : graph.getVertices()) {
            Vertex v = gv.vertex;
            if (Double.isNaN(v.getCoordinate().x) || Double.isNaN(v.getCoordinate().y)) {
                _log.warn("Vertex " + v + " has NaN location; this will cause doom.");
            }
            
            for (Edge e : gv.getOutgoing()) {
                Geometry g = e.getGeometry();
                if (g == null) {
                    continue;
                }
                for (Coordinate c : g.getCoordinates()) {
                    if (Double.isNaN(c.x) || Double.isNaN(c.y)) {
                        _log.warn("Edge " + e + " has bogus geometry");
                    }
                }
            }
        }

    }
}
