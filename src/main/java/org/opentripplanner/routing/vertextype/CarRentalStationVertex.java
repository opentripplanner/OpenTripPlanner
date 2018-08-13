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
import org.opentripplanner.routing.car_rental.CarRentalStation;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;

import java.util.Set;

/**
 * A vertex for a car rental station.
 * It is connected to the streets by a StreetCarRentalLink.
 * To allow transitions on and off a car, it has RentACar* loop edges.
 *
 * @author Evan Siroky
 * @author Conveyal
 * 
 */
public class CarRentalStationVertex extends Vertex {

    private static final long serialVersionUID = MavenVersion.VERSION.getUID();

    private int carsAvailable;

    private int spacesAvailable;

    private String id;

    private String address;

    private Set<String> networks;
    
    public CarRentalStationVertex(Graph g, CarRentalStation station) {
        //FIXME: raw_name can be null if car station is made from graph updater
        super(g, "car rental station " + station.id, station.x, station.y,
                station.name);
        this.setId(station.id);
        this.setCarsAvailable(station.carsAvailable);
        this.setSpacesAvailable(station.spacesAvailable);
        this.setAddress(station.address);
        this.setNetworks(station.networks);
    }

    public int getCarsAvailable() {
        return carsAvailable;
    }

    public int getSpacesAvailable() {
        return spacesAvailable;
    }

    public void setCarsAvailable(int cars) {
        this.carsAvailable = cars;
    }

    public void setSpacesAvailable(int spaces) {
        this.spacesAvailable = spaces;
    }

    public String getAddress() { return address; }

    public void setAddress(String address) { this.address = address; }

    public Set<String> getNetworks() { return networks; }

    public void setNetworks(Set<String> networks) { this.networks = networks; }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
