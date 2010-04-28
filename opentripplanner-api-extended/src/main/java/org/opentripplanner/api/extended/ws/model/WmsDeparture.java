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

package org.opentripplanner.api.extended.ws.model;

import java.util.Date;

import javax.xml.bind.annotation.XmlElement;

import org.onebusaway.gtfs.model.Route;

public class WmsDeparture implements Comparable<WmsDeparture> {

    @SuppressWarnings("unused")
    @XmlElement(name="route")
    private WmsRoute route;
    
    @SuppressWarnings("unused")
    @XmlElement(name="headsign")
    private String headsign;
    
    @XmlElement(name="date")
    private Date date;

    public WmsDeparture() {        
    }
    
    public WmsDeparture(Route route, String headsign, Date date) {
        this.route = new WmsRoute(route);
        this.headsign = headsign;
        this.date = date;
    }

    @Override
    public int compareTo(WmsDeparture other) {
        return this.date.compareTo(other.date);
    }
}
