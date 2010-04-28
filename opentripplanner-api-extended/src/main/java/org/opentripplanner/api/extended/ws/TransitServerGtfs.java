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

package org.opentripplanner.api.extended.ws;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.ShapePoint;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.gtfs.services.GtfsRelationalDao;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.gtfs.GtfsLibrary;

/*
 * used to facilitate getting access to the gtfscontext in the extended api
 */
public class TransitServerGtfs {

    private Map<String, Route> routeIdsToRoutes = new HashMap<String, Route>();
    private Map<String, List<Stop>> routeIdsToStops = new HashMap<String, List<Stop>>();
    private Map<String, List<StopTime>> stopIdsToStopTimes = new HashMap<String, List<StopTime>>();
    private Map<String, Stop> stopIdsToStops = new HashMap<String, Stop>();
    private Map<String, Set<String>> stopIdsToRouteIds = new HashMap<String, Set<String>>();

    // XXX the list of trip ids that have shapes that are representative of a particular route
    // this was hand generated for new york city
    private Set<String> representativeTripIds;
    
    private File gtfsFile;
    private GtfsContext gtfsContext;
    private String geoserverBaseUri;

    public void setGtfsFile(File gtfsFile) {
        this.gtfsFile = gtfsFile;
    }

    public File getGtfsFile() {
        return gtfsFile;
    }
    
    public void setGtfsContext(GtfsContext gtfsContext) {
        this.gtfsContext = gtfsContext;
    }

    public GtfsContext getGtfsContext() {
        return gtfsContext;
    }
    
    public String getGeoserverBaseUri() {
        return this.geoserverBaseUri;
    }
    
    public void setGeoserverBaseUri(String geoserverBaseUri) {
        this.geoserverBaseUri = geoserverBaseUri;
    }
    
    private void loadRepresentativeShapeIds() {
        String[] ids = new String[] {
        		"TriMet 10W1010", "TriMet 10W1020",
        		"TriMet 10W1040", "TriMet 10W1050",  
                "MTA NYCT A20100125W_048000_1..S03R",
                "MTA NYCT A20100125W_048050_2..S01R",
                "MTA NYCT A20100125W_048350_3..S01R",
                "MTA NYCT A20100125A_048450_4..S06R",
                "MTA NYCT A20100125W_053850_5..S03R",
                "MTA NYCT A20100125W_048000_6..S02R",
                "MTA NYCT A20100125U_048300_6..N01R",
                "MTA NYCT A20100125W_048200_7..S14R",
                "MTA NYCT A20100125A_048300_7..S01R",
                "MTA NYCT B20100125W_048450_A..S55R",
                "MTA NYCT B20100125W_048250_B..S45R",
                "MTA NYCT B20100125A_048300_C..S04R",
                "MTA NYCT B20100125W_048300_D..S14R",
                "MTA NYCT B20100125W_049100_E..S56R",
                "MTA NYCT B20100125U_048250_F..S69R",
                "MTA NYCT B20100125U_048050_G..N12R",
                "MTA NYCT B20100125W_048000_J..N12R",
                "MTA NYCT B20100125A_048400_L..S01R",
                "MTA NYCT B20100125W_048550_M..N71R",
                "MTA NYCT B20100125W_048700_N..S34R",
                "MTA NYCT B20100125A_048000_Q..S41R",
                "MTA NYCT B20100125W_048350_R..N93R",
                "MTA NYCT A20100125A_048000_GS.S01R",
                "MTA NYCT B20100125W_048150_FS.S01R",
                "MTA NYCT B20100125U_048100_H..S21R",
                "MTA NYCT S20100125A_048100_SI.S01R",
                "MTA NYCT B20100125W_048150_V..S01R",
                "MTA NYCT B20100125W_048300_W..S25R",
                "MTA NYCT B20100125W_101500_J..N16R"
        };
        List<String> listIds = Arrays.asList(ids);
        representativeTripIds = new HashSet<String>(listIds);
    }

    public void initialize() throws IOException {
        if (gtfsFile == null) {
            throw new IllegalStateException("Gtfs file is not set");
        }
        if (geoserverBaseUri == null) {
            throw new IllegalStateException("Geoserver base uri not set");
        }
        System.out.println("Reading gtfs file: " + gtfsFile.getPath());
        this.setGtfsContext(GtfsLibrary.readGtfs(gtfsFile));
        System.out.println("GTFS loaded");
        this.loadRepresentativeShapeIds();
        this.assembleGtfsData();
        System.out.println("GTFS data assembled");
    }
    
    private void assembleGtfsData() {
        // take the loaded gtfs data, and group it to answer queries
        // efficiently
        
        GtfsRelationalDao dao = getGtfsContext().getDao();
        
        // associate routes with stops and shape points
        for (StopTime stopTime : dao.getAllStopTimes()) {
            Stop stop = stopTime.getStop();
            Trip trip = stopTime.getTrip();
            String shapeId = trip.getShapeId().toString();
            if (shapeId == null) {
                System.out.println("shape id is null, continuing");
                continue;
            }
            Route route = trip.getRoute();
            String routeId = route.getId().toString();
            String tripId = trip.getId().toString();
            
            String stopId = stop.getId().toString();
            
            if (!stopIdsToStopTimes.containsKey(stopId)) {
                stopIdsToStopTimes.put(stopId, new ArrayList<StopTime>());
            }
            List<StopTime> stopTimesForStopId = stopIdsToStopTimes.get(stopId);
            stopTimesForStopId.add(stopTime);
            
            if (!stopIdsToStops.containsKey(stopId)) {
                stopIdsToStops.put(stopId, stop);
            }

            if (!representativeTripIds.contains(tripId)) {
                continue;
            }
            if (!routeIdsToStops.containsKey(routeId)) {
                routeIdsToStops.put(routeId, new ArrayList<Stop>());
            }
            List<Stop> stopsForRoute = routeIdsToStops.get(routeId);
            stopsForRoute.add(stop);
            routeIdsToRoutes.put(routeId, route);
            Set<String> routeIdsForStopId = stopIdsToRouteIds.get(stopId);
            if (routeIdsForStopId == null) {
                routeIdsForStopId = new HashSet<String>();
                stopIdsToRouteIds.put(stopId, routeIdsForStopId);
            }
            routeIdsForStopId.add(routeId);
            
        }
    }

    public List<Route> getRoutes() {
        List<Route> routes = new ArrayList<Route>();
        routes.addAll(routeIdsToRoutes.values());
        return routes;
    }

    public List<Stop> getStopsForRoute(String routeId) {
        return routeIdsToStops.get(routeId);
    }

    public Route getRoute(String routeId) {
        return routeIdsToRoutes.get(routeId);
    }    

    public List<StopTime> getStopTimesForStopId(String stopId) {
        if (stopIdsToStopTimes.containsKey(stopId)) {
            return stopIdsToStopTimes.get(stopId);
        } else {
            System.out.println("No stop times found for stop id: " + stopId);
            return new ArrayList<StopTime>();
        }
    }
    
    public Set<AgencyAndId> getServiceIdsOnDate(Date date) {
    	ServiceDate serviceDate = new ServiceDate(date);
        return gtfsContext.getCalendarService().getServiceIdsOnDate(serviceDate);
    }
    
    public Set<String> getRouteIdsForStopId(String stopId) {
        if (stopIdsToRouteIds.containsKey(stopId)) {
            return stopIdsToRouteIds.get(stopId);
        } else {
            System.out.println("No route ids found for stop id: " + stopId);
            return new HashSet<String>();
        }
    }
}
