/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.*/
package org.opentripplanner.visualizer;

import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.vertextype.TransitStop;

/**
 * Saves {@link TransitStop} or {@link StreetEdge} for current Stop to
 * Street connection in {@link CurStationConModel} in Graph Visualizer
 * 
 * It is used to create tests for Linker
 *
 * @author mabu
 */
public class TranstiStopOrStreetEdge {
    TransitStop transitStop = null;
    StreetEdge wantedPath = null;

    public TranstiStopOrStreetEdge(TransitStop transitStop) {
        this.transitStop = transitStop;
    }

    public TranstiStopOrStreetEdge(StreetEdge wantedPath) {
        this.wantedPath = wantedPath;
    }

    @Override
    public String toString() {
        if (transitStop != null) {
            return "V: " + transitStop.getStop().getName() + " (" + transitStop.getStopId().getId() + ")";
        } else {
            return "E: " + wantedPath.getName() + 
                    " [" + wantedPath.getPermission() + "]";// +
                    //" (" + wantedPath.getLabel() +")";
        }
    }
    
    public boolean isTransitStop() {
        return transitStop != null;
    }
    
    public boolean isStreetEdge() {
        return wantedPath != null;
    }
    
    
}
