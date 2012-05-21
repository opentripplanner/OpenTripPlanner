/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (props, at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.core;

import java.util.HashMap;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.graph.Vertex;

/**
 * StateData contains the components of search state that are unlikely to be changed as often as
 * time or weight. This avoids frequent duplication, which should have a positive impact on both
 * time and space use during searches.
 */
public class StateData implements Cloneable {

    // the time at which the search started
    protected long startTime;

    // which trip index inside a pattern
    protected int trip;

    protected AgencyAndId tripId;

    protected double lastTransitWalk = 0;

    protected String zone;

    protected AgencyAndId route;

    protected int numBoardings;

    protected boolean alightedLocal;

    protected boolean everBoarded;

    protected boolean usingRentedBike;

    protected Vertex previousStop;

    protected long lastAlightedTime;

    protected NoThruTrafficState noThruTrafficState = NoThruTrafficState.INIT;

    protected AgencyAndId[] routeSequence;

    protected HashMap<Object, Object> extensions;

    protected RoutingRequest opt;

    protected StateData clone() {
        try {
            return (StateData) super.clone();
        } catch (CloneNotSupportedException e1) {
            throw new IllegalStateException("This is not happening");
        }
    }

}
