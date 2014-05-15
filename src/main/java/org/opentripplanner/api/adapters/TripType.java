/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (props, at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.api.adapters;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Trip;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@XmlRootElement(name = "trip")
public class TripType {

    @SuppressWarnings("deprecation")
    public TripType(Trip obj) {
        this.id = obj.getId();
        this.serviceId = obj.getServiceId();
        this.tripShortName = obj.getTripShortName();
        this.tripHeadsign = obj.getTripHeadsign();
        this.routeId = obj.getRoute().getId();
        this.directionId = obj.getDirectionId();
        this.blockId = obj.getBlockId();
        this.shapeId = obj.getShapeId();
        this.wheelchairAccessible = obj.getWheelchairAccessible();
        this.tripBikesAllowed = obj.getTripBikesAllowed();
        this.bikesAllowed = obj.getBikesAllowed();
        this.route = obj.getRoute();
    }

    @SuppressWarnings("deprecation")
    public TripType(Trip obj, Boolean extended) {
        this.id = obj.getId();
        this.tripShortName = obj.getTripShortName();
        this.tripHeadsign = obj.getTripHeadsign();
        if (extended != null && extended.equals(true)) {
            this.route = obj.getRoute();
            this.serviceId = obj.getServiceId();
            this.routeId = obj.getRoute().getId();
            this.directionId = obj.getDirectionId();
            this.blockId = obj.getBlockId();
            this.shapeId = obj.getShapeId();
            this.wheelchairAccessible = obj.getWheelchairAccessible();
            this.tripBikesAllowed = obj.getTripBikesAllowed();
            this.bikesAllowed = obj.getBikesAllowed();
        }
    }

    public TripType() {
    }

    @XmlJavaTypeAdapter(AgencyAndIdAdapter.class)
    @JsonSerialize
    AgencyAndId id;

    @XmlJavaTypeAdapter(AgencyAndIdAdapter.class)
    @JsonSerialize
    AgencyAndId serviceId;

    @XmlAttribute
    @JsonSerialize
    String tripShortName;

    @XmlAttribute
    @JsonSerialize
    String tripHeadsign;

    @XmlJavaTypeAdapter(AgencyAndIdAdapter.class)
    @JsonSerialize
    AgencyAndId routeId;

    @XmlAttribute
    @JsonSerialize
    String directionId;

    @XmlAttribute
    @JsonSerialize
    String blockId;

    @XmlJavaTypeAdapter(AgencyAndIdAdapter.class)
    @JsonSerialize
    AgencyAndId shapeId;

    @XmlAttribute
    @JsonSerialize
    Integer wheelchairAccessible;

    @XmlAttribute
    @JsonSerialize
    Integer tripBikesAllowed;
    
    @XmlAttribute
    @JsonSerialize
    Integer bikesAllowed;

    Route route;

    public Route getRoute() {
        return route;
    }
}
