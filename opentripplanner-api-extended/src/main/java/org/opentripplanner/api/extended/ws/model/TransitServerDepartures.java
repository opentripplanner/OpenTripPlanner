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
import org.opentripplanner.api.extended.ws.TransitServerGtfs;

@XmlRootElement(name="departures")
public class TransitServerDepartures {
    
    private Calendar calendar;

    @XmlElement(name="departure")
    private List<TransitServerDeparture> departures = new ArrayList<TransitServerDeparture>();

    public TransitServerDepartures() {       
    }

    public TransitServerDepartures(String latlon, int nDepartures, TransitServerGtfs transitServerGtfs) {
        this.calendar = Calendar.getInstance();
        
        Date now = new Date();
        Set<AgencyAndId> serviceIdsActive = transitServerGtfs.getServiceIdsOnDate(now);
        
        List<TransitServerDeparture> allDepartures = new ArrayList<TransitServerDeparture>();
        
        List<Stop> stopsForLatLon = transitServerGtfs.getStopsForLatLon(latlon);
        for (Stop stop : stopsForLatLon) {
            List<StopTime> stopTimes = transitServerGtfs.getStopTimesForStopId(stop.getId().toString());
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
                    TransitServerDeparture departure = new TransitServerDeparture(route, headsign, date);
                    allDepartures.add(departure);
                }
            }
        }
        Collections.sort(allDepartures);
        for (int i = 0; i < Math.min(nDepartures, allDepartures.size()); i++) {
            this.departures.add(allDepartures.get(i));
        }
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
