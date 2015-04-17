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

import org.opentripplanner.common.model.T2;
import org.opentripplanner.routing.carspeed.CarSpeedSnapshot.StreetEdgeCarSpeedProvider;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.updater.JsonConfigurable;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * TODO Remove this
 * 
 */
public class DummyCarSpeedDataSource implements CarSpeedDataSource, JsonConfigurable {

    private List<T2<StreetEdge, StreetEdgeCarSpeedProvider>> dummy = new ArrayList<>();

    private static class ConstantCarSpeedProvider implements StreetEdgeCarSpeedProvider {

        private final float constantSpeed;

        public ConstantCarSpeedProvider(float constantSpeed) {
            this.constantSpeed = constantSpeed;
        }

        @Override
        public float getCarSpeed(StreetEdge streetEdge, long timestamp, float defaultSpeed) {
            return constantSpeed;
        }
    }

    @Override
    public List<T2<StreetEdge, StreetEdgeCarSpeedProvider>> getUpdatedEntries() {
        List<T2<StreetEdge, StreetEdgeCarSpeedProvider>> retval = dummy;
        dummy = null;
        return retval;
    }

    @Override
    public void configure(Graph graph, JsonNode jsonNode) throws Exception {
        ConstantCarSpeedProvider zeroSpeed = new ConstantCarSpeedProvider(0.001f);
        for (StreetEdge edge : graph.getStreetEdges()) {
            if (edge.getName().length() > 20) {
                dummy.add(new T2<StreetEdge, StreetEdgeCarSpeedProvider>(edge, zeroSpeed));
            } else
            if (edge.getDistance() > 100) {
                float dummySpeed = edge.getName().length();
                ConstantCarSpeedProvider provider = new ConstantCarSpeedProvider(dummySpeed);
                dummy.add(new T2<StreetEdge, StreetEdgeCarSpeedProvider>(edge, provider));
            }
        }
    }

}
