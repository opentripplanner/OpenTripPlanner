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

package org.opentripplanner.routing.vehicle_rental;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.locationtech.jts.geom.Geometry;

import javax.xml.bind.annotation.XmlAttribute;
import java.io.Serializable;
import java.util.Locale;

/**
 * VehicleRentalRegion defines the region in which a vehicle rental operates.
 * Vehicles are only allowed to be dropped off within the operation region and not outside.
 */
public class VehicleRentalRegion implements Serializable, Cloneable {
    private static final long serialVersionUID = -6832065607507589167L;
    /**
     * The vehicle rental network name.
     */
    @XmlAttribute
    @JsonSerialize
    public String network;

    @XmlAttribute
    @JsonSerialize
    public Geometry geometry;

    public VehicleRentalRegion(String network, Geometry geometry) {
        this.network = network;
        this.geometry = geometry;
    }

    public VehicleRentalRegion() {
    }

    public boolean equals(Object o) {
        if (!(o instanceof VehicleRentalRegion)) {
            return false;
        }
        VehicleRentalRegion other = (VehicleRentalRegion) o;
        return other.network.equals(network);
    }

    public int hashCode() {
        return network.hashCode() + 1;
    }

    public String toString() {
        return String.format(Locale.US, "Vehicle rental region for network %s", network);
    }

    @Override
    public VehicleRentalRegion clone() {
        try {
            return (VehicleRentalRegion) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(); //can't happen
        }
    }
}