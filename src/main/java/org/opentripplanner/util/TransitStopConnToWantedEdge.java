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

package org.opentripplanner.util;

import java.io.Serializable;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.vertextype.TransitStop;

/**
 *
 * @author mabu
 */


public class TransitStopConnToWantedEdge implements Serializable {
    protected final TransitStop transitStop;
    protected final StreetEdge wantedPath;
    protected StreetType streetType;
    
    private static final long serialVersionUID = 7526472295622776147L;

    public TransitStopConnToWantedEdge(TransitStop transitStop, StreetEdge wantedPath, StreetType streetType) {
        this.transitStop = transitStop;
        this.wantedPath = wantedPath;
        this.streetType = streetType;
    }

    @Override
    public String toString() {
        String transitString = "V: " + transitStop.getStop().getName() + " (" + transitStop.getStopId().getId() + ")";
        String edgeString = "E: " + wantedPath.getName()
                + " [" + wantedPath.getPermission() + "|" + streetType + "]"
                + " (" + wantedPath.getLabel() + ")";
        
        return transitString + " => " + edgeString;

    }
    
    public String getStopID() {
        return transitStop.getLabel();
    }
    
    public TransitStop getTransitStop() {
        return transitStop;
    }
    
    public StreetEdge getStreetEdge() {
        return wantedPath;
    }
     
}
