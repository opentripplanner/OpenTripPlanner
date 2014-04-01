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

package org.opentripplanner.common.model;

/**
 * A starting/ending location for a trip.
 * 
 * @author novalis
 * 
 */
public class NamedPlace {
    /**
     * some human-readable text string e.g. W 34th St
     * */
    public String name;

    /**
     * "latitude,longitude", or the name of a graph vertex
     */
    public String place;

    public NamedPlace(String name, String place) {
        this.name = name;
        this.place = place;
    }

    public NamedPlace(String place) {
        this.place = place;
    }

    public String getRepresentation() {
        if (name == null) {
            return place;
        }
        return name + "::" + place;
    }

    public String toString() {
        return "NamedPlace(" + name + ", " + place + ")";
    }
}
