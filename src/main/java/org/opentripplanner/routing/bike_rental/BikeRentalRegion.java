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

package org.opentripplanner.routing.bike_rental;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.vividsolutions.jts.geom.Geometry;

import javax.xml.bind.annotation.XmlAttribute;
import java.io.Serializable;
import java.util.Locale;

/**
 * BikeRentalRegion defines the region in which a bike rental operates.
 * For example, in case of free floating bikes (or dockless), bikes are only allowed to be dropped off within
 * the operation region and not outside.
 */
public class BikeRentalRegion implements Serializable, Cloneable {
    private static final long serialVersionUID = -6832065607507589167L;
    /**
     * The bike rental network name.
     */
    @XmlAttribute
    @JsonSerialize
    public String network;

    @XmlAttribute
    @JsonSerialize
    public Geometry geometry;

    public BikeRentalRegion(String network, Geometry geometry) {
        this.network = network;
        this.geometry = geometry;
    }

    public BikeRentalRegion() {
    }

    public boolean equals(Object o) {
        if (!(o instanceof BikeRentalRegion)) {
            return false;
        }
        BikeRentalRegion other = (BikeRentalRegion) o;
        return other.network.equals(network);
    }

    public int hashCode() {
        return network.hashCode() + 1;
    }

    public String toString() {
        return String.format(Locale.US, "Bike rental region for network %s", network);
    }

    @Override
    public BikeRentalRegion clone() {
        try {
            return (BikeRentalRegion) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(); //can't happen
        }
    }
}
