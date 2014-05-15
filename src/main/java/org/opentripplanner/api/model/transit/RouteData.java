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

package org.opentripplanner.api.model.transit;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.api.adapters.AgencyAndIdAdapter;
import org.opentripplanner.api.adapters.RouteType;
import org.opentripplanner.api.adapters.StopType;
import org.opentripplanner.routing.transit_index.RouteVariant;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@XmlRootElement(name = "RouteData")
public class RouteData {
    @XmlElement
    @XmlJavaTypeAdapter(AgencyAndIdAdapter.class)
    @JsonSerialize
    public AgencyAndId id;

    @XmlElementWrapper
    public List<StopType> stops;

    @XmlElementWrapper
    public List<RouteVariant> variants;

    @XmlElementWrapper
    public List<String> directions;
    
    @XmlElement(name = "route")
    public RouteType route;
}
