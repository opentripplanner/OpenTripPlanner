/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.opentripplanner.routing.vertextype;

import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.edgetype.PathwayEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;

public class TransitStop extends OffboardVertex {
    private static final long serialVersionUID = 1L;

    private boolean wheelchairEntrance;

    private boolean isEntrance;

    /**
     * For stops that are deep underground, there is a time cost to entering and exiting the stop;
     * all stops are assumed to be at street level unless we have configuration to the contrary
     */
    private int streetToStopTime = 0;

    /** A stop is local iff, for each of its possible transfers to another trip pattern,
     * the same transfers can be made at the previous stop or the next stop.  Once
     * one boards at a local stop, one can never board at another local stop; once 
     * one alights from a local stop, one can never board again at all. 
     */
    private boolean local = false;

    public TransitStop(Graph graph, Stop stop) {
        super(graph, GtfsLibrary.convertIdToString(stop.getId()), stop);
            this.wheelchairEntrance = stop.getWheelchairBoarding() == 1;
        isEntrance = stop.getLocationType() == 2;
    }

    public boolean hasWheelchairEntrance() {
        return wheelchairEntrance;
    }

    public boolean isEntrance() {
        return isEntrance;
    }

    public void setLocal(boolean local) {
        this.local  = local;
    }
    public boolean isLocal() {
        return local;
    }

    public boolean hasEntrances() {
        for (Edge e : this.getOutgoing()) {
            if (e instanceof PathwayEdge) {
                return true;
            }
        }
        return false;
    }

    public int getStreetToStopTime() {
        return streetToStopTime;
    }

    public void setStreetToStopTime(int streetToStopTime) {
        this.streetToStopTime = streetToStopTime;
    }
}
