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
package org.opentripplanner.model;

import org.opentripplanner.routing.vertextype.TransitStationStop;

import java.io.Serializable;
import java.util.List;

public class Landmark implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;

    private List<TransitStationStop> stops;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<TransitStationStop> getStops() {
        return stops;
    }

    public void setStops(List<TransitStationStop> stops) {
        this.stops = stops;
    }
}
