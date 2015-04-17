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

package org.opentripplanner.updater.carspeed;

import java.util.ArrayList;
import java.util.List;

import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.routing.carspeed.CarSpeedSnapshot.StreetEdgeCarSpeedProvider;
import org.opentripplanner.routing.carspeed.CarSpeedSnapshot.StreetEdgeConstantCarSpeedProvider;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.JsonConfigurable;

import com.fasterxml.jackson.databind.JsonNode;
import com.vividsolutions.jts.geom.Coordinate;

/**
 * This is a demo for a really simple car speed source: it just set the
 * speed to zero in some "exclusion zone".
 */
public class ExclusionZoneCarSpeedDataSource implements CarSpeedDataSource, JsonConfigurable {

    private List<T2<StreetEdge, StreetEdgeCarSpeedProvider>> dummy = new ArrayList<>();

    @Override
    public List<T2<StreetEdge, StreetEdgeCarSpeedProvider>> getUpdatedEntries() {
        List<T2<StreetEdge, StreetEdgeCarSpeedProvider>> retval = dummy;
        dummy = null;
        return retval;
    }

    @Override
    public void configure(Graph graph, JsonNode jsonNode) throws Exception {

        // TODO We should be really reading the exclusion zone from somewhere
        // and use a generic "exclusion zone source". But this is a demo.

        double lat = jsonNode.path("center").path("lat").asDouble();
        double lon = jsonNode.path("center").path("lon").asDouble();
        double radius = jsonNode.path("radiusMeters").asDouble();
        Coordinate center = new Coordinate(lon, lat);

        StreetEdgeCarSpeedProvider noTraffic = new StreetEdgeConstantCarSpeedProvider(0f);

        // TODO This is not really efficient, use spatial index to query only edges in the zone
        for (StreetEdge streetEdge : graph.getStreetEdges()) {
            double d1 = SphericalDistanceLibrary.fastDistance(center, streetEdge.getFromVertex()
                    .getCoordinate());
            double d2 = SphericalDistanceLibrary.fastDistance(center, streetEdge.getToVertex()
                    .getCoordinate());
            if (d1 < radius || d2 < radius) {
                dummy.add(new T2<>(streetEdge, noTraffic));
            }
        }
    }

}
