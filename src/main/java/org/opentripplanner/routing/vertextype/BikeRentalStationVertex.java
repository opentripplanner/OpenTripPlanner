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
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.updater.bike_rental.BikeRentalHours;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A vertex for a bike rental station.
 * It is connected to the streets by a StreetBikeRentalLink.
 * To allow transitions on and off a bike, it has RentABike* loop edges.
 *
 * @author laurent
 *
 * TODO if we continue using this for car rental and flex systems, change name to VehicleRentalStationVertex
 */
public class BikeRentalStationVertex extends Vertex {

    private static final long serialVersionUID = MavenVersion.VERSION.getUID();

    private int bikesAvailable;

    private int spacesAvailable;

    private String id;

    /** Some car rental systems and flex transit systems work exactly like bike rental, but with cars. */
    private boolean isCarStation;

    /**
     * The hours that this station is in operation.
     * GBFS defines hours of operation at the system level, but we store information on a per-station level.
     * When null or empty no hours of operation have been defined, and we assume the station is always active.
     */
    private List<BikeRentalHours> rentalHoursList = null;

    public BikeRentalStationVertex(Graph g, BikeRentalStation station) {
        //FIXME: raw_name can be null if bike station is made from graph updater
        super(g, "bike rental station " + station.id, station.x, station.y, station.name);
        this.setId(station.id);
        this.setBikesAvailable(station.bikesAvailable);
        this.setSpacesAvailable(station.spacesAvailable);
        this.isCarStation = station.isCarStation;
        this.rentalHoursList = station.rentalHoursList;
    }

    public int getBikesAvailable() {
        return bikesAvailable;
    }

    public int getSpacesAvailable() {
        return spacesAvailable;
    }

    public void setBikesAvailable(int bikes) {
        this.bikesAvailable = bikes;
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

    /**
     * Tell the routing algorithm what kind of vehicle is being rented or dropped off here.
     * Some car rental systems and flex transit systems work exactly like bike rental, but with cars.
     * We can model them as bike rental systems by changing only this one detail.
     */
    public TraverseMode getVehicleMode () {
         return isCarStation ? TraverseMode.CAR : TraverseMode.BICYCLE;
    }

    /**
     * Determines whether this station can be used by a member or non-member at the given date and time.
     */
    public boolean isSystemActive(LocalDateTime dateTime, boolean isSystemMember) {
        // If no specific hours of operation were set, we assume the system is always active.
        if (rentalHoursList == null || rentalHoursList.isEmpty()) {
            return true;
        }
        for (BikeRentalHours rentalHours : rentalHoursList) {
            if (rentalHours.matches(dateTime, isSystemMember)) return true;
        }
        return false;
    }

}
