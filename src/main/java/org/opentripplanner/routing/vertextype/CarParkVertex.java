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

package org.opentripplanner.routing.vertextype;

import org.opentripplanner.common.MavenVersion;
import org.opentripplanner.routing.car_park.CarPark;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.util.NonLocalizedString;

/**
 * A vertex for a car park.
 * 
 * Connected to streets by StreetCarParkLink. Transition for parking the car is handled by
 * CarParkEdge.
 * 
 * @author Evan Siroky
 * @author Conveyal
 * 
 */
public class CarParkVertex extends Vertex {

    private static final long serialVersionUID = MavenVersion.VERSION.getUID();

    private int spacesAvailable;

    private String id;

    public CarParkVertex(Graph g, CarPark carPark) {
        //TODO: localize carpark
        super(g, "car park " + carPark.id, carPark.x, carPark.y, new NonLocalizedString(carPark.name));
        this.setId(carPark.id);
        this.setSpacesAvailable(carPark.spacesAvailable);
    }

    public int getSpacesAvailable() {
        return spacesAvailable;
    }

    public void setSpacesAvailable(int spaces) {
        this.spacesAvailable = spaces;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
