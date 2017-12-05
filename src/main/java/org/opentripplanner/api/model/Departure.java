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
package org.opentripplanner.api.model;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.trippattern.TripTimes;

import java.util.Calendar;
import java.util.TimeZone;

public class Departure {
    private AgencyAndId tripId;

    private Calendar departureTime;

    private int departureDelay;

    private boolean realTime;

    public Departure(TimeZone tz, ServiceDay sd, TripTimes tripTimes, int stopIndex) {
        tripId = tripTimes.trip.getId();
        long time = sd.time(tripTimes.getDepartureTime(stopIndex));
        Calendar calendar = Calendar.getInstance(tz);
        calendar.setTimeInMillis(time * 1000L);
        this.departureTime = calendar;
        this.realTime = !tripTimes.isScheduled();
        this.departureDelay = tripTimes.getDepartureDelay(stopIndex);
    }

    public AgencyAndId getTripId() {
        return tripId;
    }

    public Calendar getDepartureTime() {
        return departureTime;
    }

    public int getDepartureDelay() {
        return departureDelay;
    }

    public boolean isRealTime() {
        return realTime;
    }
}
