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

package org.opentripplanner.routing.location;

import com.vividsolutions.jts.geom.Coordinate;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.NonLocalizedString;

/**
 * Represents a location on a street, somewhere between the two corners. This is used when computing the first and last segments of a trip, for trips
 * that start or end between two intersections. Also for situating bus stops in the middle of street segments.
 */
public class StreetLocation extends StreetVertex {
    private boolean wheelchairAccessible;

    // maybe name should just be pulled from street being split
    public StreetLocation(String id, Coordinate nearestPoint, I18NString name) {
        // calling constructor with null graph means this vertex is temporary
        super(null, id, nearestPoint.x, nearestPoint.y, name);
    }

    //For tests only
    public StreetLocation(String id, Coordinate nearestPoint, String name) {
        // calling constructor with null graph means this vertex is temporary
        super(null, id, nearestPoint.x, nearestPoint.y, new NonLocalizedString(name));
    }

    private static final long serialVersionUID = 1L;

    public void setWheelchairAccessible(boolean wheelchairAccessible) {
        this.wheelchairAccessible = wheelchairAccessible;
    }

    public boolean isWheelchairAccessible() {
        return wheelchairAccessible;
    }

    public boolean equals(Object o) {
        if (o instanceof StreetLocation) {
            StreetLocation other = (StreetLocation) o;
            return other.getCoordinate().equals(getCoordinate());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getCoordinate().hashCode();
    }
}
