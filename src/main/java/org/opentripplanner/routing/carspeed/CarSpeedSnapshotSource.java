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

import java.util.HashMap;
import java.util.Map;

import org.opentripplanner.routing.carspeed.CarSpeedSnapshot.StreetEdgeCarSpeedProvider;
import org.opentripplanner.routing.edgetype.StreetEdge;

public class CarSpeedSnapshotSource {

    private Map<StreetEdge, StreetEdgeCarSpeedProvider> currentProviders = new HashMap<>();

    private CarSpeedSnapshot currentSnapshot;

    public CarSpeedSnapshotSource() {
        commit();
    }

    public CarSpeedSnapshot getCarSpeedSnapshot() {
        return currentSnapshot;
    }

    public synchronized void updateCarSpeedProvider(StreetEdge streetEdge, StreetEdgeCarSpeedProvider provider) {
        /* If a previous provider exists, it will be kept as long as a CarSpeedSnapshot use it. */
        if (provider == null) {
            /* This special case ensure we do not store unneeded null values. */
            currentProviders.remove(streetEdge);
        } else {
            currentProviders.put(streetEdge, provider);
        }
    }

    public synchronized void commit() {
        // Make a copy of the map
        Map<StreetEdge, StreetEdgeCarSpeedProvider> newProviders = new HashMap<>(currentProviders);
        // Assignment of references are atomic in java, no need to synchronize
        currentSnapshot = new CarSpeedSnapshot(newProviders);
    }
}
