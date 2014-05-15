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

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.onebusaway.gtfs.model.AgencyAndId;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@XmlRootElement(name = "trip")
public class TripsModelInfo implements Serializable {

    private static final long serialVersionUID = -4853941297409355512L;

    public TripsModelInfo(String headsign, Integer number, String calendarId, AgencyAndId tripId) {
        this.headsign = headsign;
        this.numberOfTrips = number;
        this.calendarId = calendarId;
        this.id = tripId.getId();
        this.agency = tripId.getAgencyId();
    }

    public TripsModelInfo() {
    }

    public String getId() {
        return id;
    }

    @XmlAttribute
    @JsonSerialize
    String headsign;

    @XmlAttribute
    @JsonSerialize
    Integer numberOfTrips;

    @XmlAttribute
    @JsonSerialize
    String calendarId;

    @XmlAttribute
    @JsonSerialize
    String id;

    @XmlAttribute
    @JsonSerialize
    String agency;
}
