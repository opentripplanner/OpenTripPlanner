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
package org.opentripplanner.routing.core;

import org.onebusaway.gtfs.model.Stop;

public class TransitStop extends GenericVertex {
    private static final long serialVersionUID = 1L;
    private boolean wheelchairEntrance;
    private boolean isEntrance;

    public TransitStop(String id, double lon, double lat, String name, String stopId, Stop stop) {
        super(id, lon, lat, name, stopId);
        if (stop != null) {
            this.wheelchairEntrance = stop.getWheelchairBoarding() == 1;
        }
        isEntrance = stop == null ? false : stop.getLocationType() == 2;
    }

    public boolean hasWheelchairEntrance() {
        return wheelchairEntrance;
    }

    public boolean isEntrance() {
        return isEntrance;
    }
}
