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

import java.util.List;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.api.extended.ws.TransitServerGtfs;
import org.opentripplanner.util.model.EncodedPolylineBean;

public class TransitServerRoute {
    private String shortname;
    private String longname;
    private String id;
    private String agencyId;
    private String mode;
    
    private TransitServerStops transitStops;
    
    private EncodedPolylineBean geometry;

    public TransitServerRoute() {
    }
    
    public TransitServerRoute(Route route, List<Stop> stops, EncodedPolylineBean geometry) {
        this.setId(route.getId().toString());
        this.setShortName(route.getShortName());
        this.setLongName(route.getLongName());
        this.setGeometry(geometry);
        this.setAgencyId(route.getAgency().getId());
        this.transitStops = new TransitServerStops(stops);
        
        switch (route.getType()) {
        case 0:
            this.setMode("TRAM");
            break;
        case 1:
            this.setMode("SUBWAY");
            break;
        case 2:
            this.setMode("RAIL");
            break;
        case 3:
            this.setMode("BUS");
            break;
        case 4:
            this.setMode("FERRY");
            break;
        case 5:
            this.setMode("CABLE_CAR");
            break;
        case 6:
            this.setMode("GONDOLA");
            break;
        case 7:
            this.setMode("FUNICULAR");
            break;
        default:
            throw new IllegalArgumentException("unknown gtfs route type " + route.getType());
        }
    }

    public TransitServerRoute(TransitServerGtfs transitServerGtfs, String routeId) {
        Route route = transitServerGtfs.getRoute(routeId);
        List<Stop> stops = transitServerGtfs.getStopsForRoute(routeId);
        
        this.setId(routeId);
        this.setShortName(route.getShortName());
        this.setLongName(route.getLongName());
        this.setStops(new TransitServerStops(stops));
    }

    public void setShortName(String shortName) {
        this.shortname = shortName;
    }

    public String getShortName() {
        return shortname;
    }
    
    public void setLongName(String longName) {
        this.longname = longName;
    }
    
    public String getLongName() {
        return longname;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setGeometry(EncodedPolylineBean geometry) {
        this.geometry = geometry;
    }

    public EncodedPolylineBean getGeometry() {
        return geometry;
    }
    
    public TransitServerStops getStops() {
        return transitStops;
    }
    public void setStops(TransitServerStops stops) {
        this.transitStops = stops;
    }

    public void setAgencyId(String agencyId) {
        this.agencyId = agencyId;
    }

    public String getAgencyId() {
        return agencyId;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getMode() {
        return mode;
    }

}
