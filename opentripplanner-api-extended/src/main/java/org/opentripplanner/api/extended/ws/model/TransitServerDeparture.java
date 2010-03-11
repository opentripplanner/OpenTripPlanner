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

import org.onebusaway.gtfs.model.Route;

public class TransitServerDeparture implements Comparable<TransitServerDeparture> {
    private String routeId;
    private String headsign;
    private Date date;

    public TransitServerDeparture() {        
    }
    
    public TransitServerDeparture(Route route, String headsign, Date date) {
        String routeId = route.getId().toString();
        this.setRouteId(routeId);
        this.setHeadsign(headsign);
        this.setDate(date);
    }

    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    public String getRouteId() {
        return routeId;
    }

    public void setHeadsign(String headsign) {
        this.headsign = headsign;
    }

    public String getHeadsign() {
        return headsign;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Date getDate() {
        return date;
    }

    @Override
    public int compareTo(TransitServerDeparture other) {
        return this.getDate().compareTo(other.getDate());
    }
}
