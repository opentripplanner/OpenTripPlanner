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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.services.GtfsRelationalDao;
import org.opentripplanner.api.extended.ws.TransitServerGtfs;

@XmlRootElement(name="stop")
public class WmsStop {
    @SuppressWarnings("unused")
    @XmlElement(name="name")
    private String name;
    
    @SuppressWarnings("unused")
    @XmlElement(name="stopId")
    private String stopId;
    
    @SuppressWarnings("unused")
    @XmlElement(name="agencyId")
    private String agencyId;
    
    @XmlElement(name="routes")
    private List<WmsRoute> routes;
    
    @SuppressWarnings("unused")
    @XmlElement(name="departures")
    private List<WmsDeparture> departures;
    
    private Calendar calendar;
    
    public WmsStop() {
    }
    
    public WmsStop(TransitServerGtfs gtfs, AgencyAndId stopId) {
        calendar = Calendar.getInstance();
        
        GtfsRelationalDao dao = gtfs.getGtfsContext().getDao();
        Stop stop = dao.getStopForId(stopId);
        if (stop == null) {
            return;
        }
        this.name = stop.getName();
        this.stopId = stopId.getId();
        this.routes = new ArrayList<WmsRoute>();
        Set<String> routeIds = gtfs.getRouteIdsForStopId(stopId.toString());
        this.agencyId = stopId.getAgencyId();
        for (String routeId : routeIds) {
            Route route = gtfs.getRoute(routeId);
            if (route != null) {
                this.routes.add(new WmsRoute(route));
            }
        }
        
        this.departures = getDeparturesForStop(gtfs, stop, 3);
    }
    private List<WmsDeparture> getDeparturesForStop(TransitServerGtfs gtfs, Stop stop, int nDepartures) {
        Date now = new Date();
        Set<AgencyAndId> serviceIdsActive = gtfs.getServiceIdsOnDate(now);
        
        List<WmsDeparture> allDepartures = new ArrayList<WmsDeparture>();
        
        List<StopTime> stopTimes = gtfs.getStopTimesForStopId(stop.getId().toString());
        for (StopTime stopTime : stopTimes) {
            // first verify that the stoptime is active for the particular day
            Trip trip = stopTime.getTrip();
            AgencyAndId tripServiceId = trip.getServiceId();
            if (!serviceIdsActive.contains(tripServiceId)) {
                continue;
            }
            int departureTime = stopTime.getDepartureTime();
            Date date = convertTimeToDate(departureTime);
            if (date.compareTo(now) > 0) {
                String headsign = trip.getTripHeadsign();
                Route route = trip.getRoute();
                WmsDeparture departure = new WmsDeparture(route, headsign, date);
                allDepartures.add(departure);
            }
        }
        Collections.sort(allDepartures);
        
        List<WmsDeparture> result = new ArrayList<WmsDeparture>();
        for (int i = 0; i < Math.min(nDepartures, allDepartures.size()); i++) {
            result.add(allDepartures.get(i));
        }
        return result;
    }

    private Date convertTimeToDate(int departureTime) {
        long currentTime = System.currentTimeMillis();
        this.calendar.setTimeInMillis(currentTime);
        this.calendar.set(Calendar.HOUR_OF_DAY, 0);
        this.calendar.set(Calendar.MINUTE, 0);
        this.calendar.set(Calendar.SECOND, 0);
        this.calendar.set(Calendar.MILLISECOND, 0);
        this.calendar.add(Calendar.SECOND, departureTime);
        Date date = this.calendar.getTime();
        return date;
    }
}
