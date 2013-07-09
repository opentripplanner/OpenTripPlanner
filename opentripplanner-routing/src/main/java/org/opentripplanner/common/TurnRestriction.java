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

package org.opentripplanner.common;

import java.io.Serializable;

import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.graph.Edge;

public class TurnRestriction implements Serializable {
    private static final long serialVersionUID = 6072427988268244536L;
    public TurnRestrictionType type;
    public Edge from;
    public Edge to;
    public RepeatingTimePeriod time;
    public TraverseModeSet modes;

    public String toString() {
        return type.name() + " from " + from + " to " + to + "(" + modes + ")";
    }
    
    public TurnRestriction () {
        time = null;
    }
    
    /**
     * Convenience constructor.
     * 
     * @param from
     * @param to
     * @param type
     */
    public TurnRestriction(Edge from, Edge to, TurnRestrictionType type,
            TraverseModeSet modes) {
        this();
        this.from = from;
        this.to = to;
        this.type = type;
        this.modes = modes;
    }
    
    /**
     * Return true if the turn restriction is in force at the time described by the long.
     * @param time
     * @return
     */
    public boolean active(long time) {
        if (this.time != null)
            return this.time.active(time);
        return true;
    }
}
