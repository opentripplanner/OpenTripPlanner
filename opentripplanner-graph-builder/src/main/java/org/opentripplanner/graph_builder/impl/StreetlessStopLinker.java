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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import lombok.Setter;

import org.opentripplanner.common.IterableLibrary;
import org.opentripplanner.common.geometry.DistanceLibrary;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.routing.edgetype.SimpleTransfer;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.StreetVertexIndexServiceImpl;
import org.opentripplanner.routing.services.StreetVertexIndexService;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * {@link GraphBuilder} plugin that links up the stops of a transit network among themselves,
 * without using the street network at all. For now this just considers distance, but it should
 * also consider parent station specifications, which are present in the Dutch KV7 data.
 */
public class StreetlessStopLinker implements GraphBuilder {

    @Setter private double radius = 250; 

    private static Logger LOG = LoggerFactory.getLogger(StreetlessStopLinker.class); 

    DistanceLibrary distanceLibrary = SphericalDistanceLibrary.getInstance();
    
    public List<String> provides() {
        return Arrays.asList("linking");
    }

    public List<String> getPrerequisites() {
        return Arrays.asList("transit");
    }

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
        StreetVertexIndexService index = new StreetVertexIndexServiceImpl(graph);
        
        for (TransitStop ts : IterableLibrary.filter(graph.getVertices(), TransitStop.class)) {
            Coordinate c = ts.getCoordinate();
            LOG.trace("linking stop {}", ts);
            int n = 0;
            for (TransitStop other : index.getNearbyTransitStops(c, 250)) {
                int distance = (int) distanceLibrary.distance(c, other.getCoordinate());
                LOG.trace("  to stop: {} ({}m)", other, distance);
                new SimpleTransfer(ts, other, distance);
                n += 1;
            }
            LOG.trace("linked to {} others.", n);
        }
        
    }

    @Override
    public void checkInputs() {
        //no inputs
    }

}
