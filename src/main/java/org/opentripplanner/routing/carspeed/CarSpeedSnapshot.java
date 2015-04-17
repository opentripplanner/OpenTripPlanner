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

package org.opentripplanner.routing.carspeed;

import java.util.Map;

import org.opentripplanner.routing.edgetype.StreetEdge;

public class CarSpeedSnapshot {

    public interface StreetEdgeCarSpeedProvider {

        /**
         * Note that we pass the edge as a parameter to the provider, as some implementation could
         * be shared for multiple edges (for example a provider for an exclusion zone).
         */
        public float getCarSpeed(StreetEdge streetEdge, long timestamp, float defaultSpeed);
    }

    /**
     * An ultra simple car speed provider that returns always the same constant speed. Used for unit
     * testing and some simple cases (zone restricting for example).
     */
    public static class StreetEdgeConstantCarSpeedProvider implements StreetEdgeCarSpeedProvider {

        private final float constantCarSpeed;

        public StreetEdgeConstantCarSpeedProvider(float constantSpeed) {
            this.constantCarSpeed = constantSpeed;
        }

        @Override
        public float getCarSpeed(StreetEdge streetEdge, long timestamp, float defaultSpeed) {
            return constantCarSpeed;
        }
    }

    private Map<StreetEdge, StreetEdgeCarSpeedProvider> providers;

    protected CarSpeedSnapshot(Map<StreetEdge, StreetEdgeCarSpeedProvider> providers) {
        this.providers = providers;
    }

    public float getCarSpeed(StreetEdge streetEdge, long timestamp, float defaultSpeed) {
        StreetEdgeCarSpeedProvider carSpeedProvider = providers.get(streetEdge);
        if (carSpeedProvider == null)
            return defaultSpeed;
        return carSpeedProvider.getCarSpeed(streetEdge, timestamp, defaultSpeed);
    }
}
