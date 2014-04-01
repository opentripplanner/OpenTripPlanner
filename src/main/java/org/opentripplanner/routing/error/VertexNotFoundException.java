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

package org.opentripplanner.routing.error;

import java.util.List;

/**
 * Indicates that a vertex requested by name or lat/long could not be located.
 * This might be thrown if a user enters a location outside the street/transit network.
 */
public class VertexNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    List<String> notFound;
    
    public VertexNotFoundException(List<String> notFound) {
        super("vertices not found: " + notFound.toString());
        this.notFound = notFound;
    }

    public List<String> getMissing() {
        return notFound;
    }
    
}
