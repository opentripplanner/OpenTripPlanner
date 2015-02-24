/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.*/
package org.opentripplanner.routing.bike_rental;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Locale;
import java.util.Set;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;

/**
 * Used for Json/XML serialized BikeRentalStation with wanted Locale
 *
 * @author mabu
 */
public class TranslatedBikeRentalStation {
    @XmlTransient
    @JsonIgnore
    public BikeRentalStation bikeRentalStation;
    
    @XmlTransient
    @JsonIgnore
    private Locale locale;

    public TranslatedBikeRentalStation(BikeRentalStation bikeRentalStation, Locale locale) {
        this.bikeRentalStation = bikeRentalStation;
        this.locale = locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    @XmlAttribute
    @JsonSerialize
    public String getId() {
        return this.bikeRentalStation.id;
    }

    @XmlAttribute
    @JsonSerialize
    public double getX() {
        return this.bikeRentalStation.x;
    }

    @XmlAttribute
    @JsonSerialize
    public double getY() {
        return this.bikeRentalStation.y;
    }

    @XmlAttribute
    @JsonSerialize
    public String getName() {
        return this.bikeRentalStation.name.toString(locale);
    }

    @XmlAttribute
    @JsonSerialize
    public int getBikesAvailable() {
        return this.bikeRentalStation.bikesAvailable;
    }

    @XmlAttribute
    @JsonSerialize
    public int getSpacesAvailable() {
        return this.bikeRentalStation.spacesAvailable;
    }

    @XmlAttribute
    @JsonSerialize
    public boolean getAllowDropoff() {
        return this.bikeRentalStation.allowDropoff;
    }

    @XmlAttribute
    @JsonSerialize
    public Set<String> getNetworks() {
        return this.bikeRentalStation.networks;
    }

    @XmlAttribute
    @JsonSerialize
    public boolean getRealTimeData() {
        return this.bikeRentalStation.realTimeData;
    }
}
