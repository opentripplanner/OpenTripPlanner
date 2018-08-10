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

package org.opentripplanner.routing.car_rental;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.ResourceBundleSingleton;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.util.Locale;
import java.util.Set;

public class CarRentalStation implements Serializable, Cloneable {
    private static final long serialVersionUID = 8311460609708089384L;

    @XmlAttribute
    @JsonSerialize
    public String id;

    @XmlTransient
    @JsonIgnore
    public String licensePlate;

    @XmlTransient
    @JsonIgnore
    public I18NString name;

    @XmlAttribute
    @JsonSerialize
    public double x, y; //longitude, latitude

    @XmlAttribute
    @JsonSerialize
    public int carsAvailable = Integer.MAX_VALUE;

    @XmlAttribute
    @JsonSerialize
    public int spacesAvailable = Integer.MAX_VALUE;

    @XmlAttribute
    @JsonSerialize
    public boolean allowDropoff = true;

    @XmlAttribute
    @JsonSerialize
    public boolean allowPickup = true;

    @XmlAttribute
    @JsonSerialize
    public boolean isFloatingCar = false;

    /**
     * List of compatible network names. Null (default) to be compatible with all.
     */
    @XmlAttribute
    @JsonSerialize
    public Set<String> networks = null;

    /**
     * Fuel type of the car.
     */
    @XmlAttribute
    @JsonSerialize
    public CarFuelType fuelType = CarFuelType.UNKNOWN;

    /**
     * Reported address of car.
     */
    @XmlAttribute
    @JsonSerialize
    public String address;

    /**
     * This is used for localization. Currently "car rental station" isn't part of the name.
     * It can be added on the client. But since it is used as Station: name, and Recommended Pick Up: name.
     * It isn't used.
     *
     * Names can be different in different languages if name tags in OSM have language tags.
     *
     * It is set in {@link org.opentripplanner.api.resource.CarRental} from URL parameter.
     *
     * Sets default locale on start
     *
     */
    @JsonIgnore
    @XmlTransient
    public Locale locale = ResourceBundleSingleton.INSTANCE.getLocale(null);

    /**
     * Whether or not this station is a border dropoff for routing purposes
     */
    @JsonIgnore
    @XmlTransient
    public boolean isBorderDropoff = false;

    public boolean equals(Object o) {
        if (!(o instanceof CarRentalStation)) {
            return false;
        }
        CarRentalStation other = (CarRentalStation) o;
        // since ID is set to be a license plate, changes to position constitute a different station
        return other.id.equals(id);
    }

    public int hashCode() {
        return id.hashCode() + 1;
    }

    public String toString () {
        return String.format(Locale.US, "Car rental station %s at %.6f, %.6f", name, y, x);
    }

    @Override
    public CarRentalStation clone() {
        try {
            return (CarRentalStation) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(); //can't happen
        }
    }

    /**
     * Gets translated name of car rental station based on locale
     */
    @XmlAttribute
    @JsonSerialize
    public String getName() {
        return name.toString(locale);
    }
}
