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

import org.opentripplanner.routing.core.GenericVertex;

/** Represents an ordinary location in space, typically an intersection */

public class EndpointVertex extends GenericVertex {

    public EndpointVertex(String label, double x, double y, String name) {
        super(label, x, y, name);
    }
    
    public EndpointVertex(String label, double x, double y) {
        super(label, x, y, label);
    }

    private static final long serialVersionUID = 1L;

    public StreetTraversalPermission getPermission() {
        throw new UnsupportedOperationException();
    }

    public boolean isWheelchairAccessible() {
        throw new UnsupportedOperationException();
    }

}
