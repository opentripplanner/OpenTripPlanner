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

import java.io.Serializable;
import java.util.Set;

import javax.xml.bind.annotation.XmlAttribute;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class BikeRentalStation implements Serializable {
    private static final long serialVersionUID = 8311460609708089384L;

    @XmlAttribute
    @JsonSerialize
    public String id;
    @XmlAttribute
    @JsonSerialize
    public String name;
    @XmlAttribute
    @JsonSerialize
    public double x, y; //longitude, latitude
    @XmlAttribute
    @JsonSerialize
    public int bikesAvailable = Integer.MAX_VALUE;
    @XmlAttribute
    @JsonSerialize
    public int spacesAvailable = Integer.MAX_VALUE;
    @XmlAttribute
    @JsonSerialize
    public boolean allowDropoff = true;

    /**
     * List of compatible network names. Null (default) to be compatible with all.
     */
    @XmlAttribute
    @JsonSerialize
    public Set<String> networks = null;
    
    /**
     * Whether this station is static (usually coming from OSM data) or a real-time source. If no real-time data, users should take
     * bikesAvailable/spacesAvailable with a pinch of salt, as they are always the total capacity divided by two. Only the total is meaningful.
     */
    @XmlAttribute
    @JsonSerialize
    public boolean realTimeData = true;

    public boolean equals(Object o) {
        if (!(o instanceof BikeRentalStation)) {
            return false;
        }
        BikeRentalStation other = (BikeRentalStation) o;
        return other.id.equals(id);
    }
    
    public int hashCode() {
        return id.hashCode() + 1;
    }
}
