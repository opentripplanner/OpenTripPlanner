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

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@XmlRootElement(name = "Headsign")
public class HeadsignsInfo implements Serializable {

    private static final long serialVersionUID = -4853941297409355512L;

    public HeadsignsInfo(String headsign, Integer number, String calendarId) {
        this.headsign = headsign;
        this.numberOfTrips = number;
        this.calendarId = calendarId;
    }

    public HeadsignsInfo(String headsign) {
        this.headsign = headsign;
        this.numberOfTrips = 0;
    }

    public HeadsignsInfo() {
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
}