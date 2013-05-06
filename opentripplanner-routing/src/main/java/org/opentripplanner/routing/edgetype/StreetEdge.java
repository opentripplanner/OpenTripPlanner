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

package org.opentripplanner.routing.edgetype;

import java.util.List;
import java.util.Set;

import org.opentripplanner.common.TurnRestriction;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.patch.Alert;
import org.opentripplanner.routing.vertextype.StreetVertex;

/**
 * Abstract base class for edges in the (open)streetmap layer (might be paths,
 * stairs, etc. as well as streets). This can be used as a marker to detect
 * edges in the street layer.
 */

/* package-private ? */
// EdgeWithElevation extends Edge
public abstract class StreetEdge extends EdgeWithElevation {

	private static final long serialVersionUID = 1L;
	public static final int CLASS_STREET = 3;
	public static final int CLASS_CROSSING = 4;
	public static final int CLASS_OTHERPATH = 5;
	public static final int CLASS_OTHER_PLATFORM = 8;
	public static final int CLASS_TRAIN_PLATFORM = 16;
	public static final int ANY_PLATFORM_MASK = 24;
	public static final int CROSSING_CLASS_MASK = 7; // ignore platform
	public static final int CLASS_LINK = 32; // on/offramps; OSM calls them
												// "links"

	public StreetEdge(StreetVertex v1, StreetVertex v2) {
		super(v1, v2);
	}

	/**
	 * Returns true if this RoutingRequest can traverse this edge.
	 */
	public abstract boolean canTraverse(RoutingRequest options);
	
	public abstract boolean canTraverse(TraverseModeSet modes);
		
	public abstract double getLength();
	
	public abstract float getCarSpeed();
	
	public abstract void setCarSpeed(float carSpeed);
	
	public abstract int getInAngle();
	
	public abstract int getOutAngle();

	public abstract StreetTraversalPermission getPermission();

	public abstract boolean isNoThruTraffic();

	public abstract int getStreetClass();

	public abstract boolean isWheelchairAccessible();

	public abstract Set<Alert> getNotes();

	public abstract Set<Alert> getWheelchairNotes();

	public abstract List<TurnRestriction> getTurnRestrictions();

}
