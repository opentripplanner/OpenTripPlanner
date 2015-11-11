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

package org.opentripplanner.graph_builder.module.osm;

import java.util.ArrayList;
import java.util.List;

import org.opentripplanner.common.RepeatingTimePeriod;
import org.opentripplanner.common.TurnRestrictionType;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.StreetEdge;

/**
 * A temporary holder for turn restrictions while we have only way/node ids but not yet edge objects
 */
class TurnRestrictionTag {

    enum Direction {
        LEFT, RIGHT, U, STRAIGHT;
    }

    long via;

    //Used only for graph annotations so that it can be visualized which annotations are problematic
    long relationOSMID;

    TurnRestrictionType type;

    Direction direction;

    RepeatingTimePeriod time;

    public List<StreetEdge> possibleFrom = new ArrayList<StreetEdge>();

    public List<StreetEdge> possibleTo = new ArrayList<StreetEdge>();

    public TraverseModeSet modes;

    TurnRestrictionTag(long via, TurnRestrictionType type, Direction direction, long relationOSMID) {
        this.via = via;
        this.type = type;
        this.direction = direction;
        this.relationOSMID = relationOSMID;
    }

    @Override
    public String toString() {
        return String.format("%s turn restriction via node %d", direction, via);
    }
}
