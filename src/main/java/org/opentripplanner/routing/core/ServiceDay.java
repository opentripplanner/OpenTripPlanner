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

package org.opentripplanner.routing.core;

import java.io.Serializable;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.opentripplanner.routing.graph.Graph;

/**
 * Represents a day of transit services. 
 * Intended for quickly checking whether a service is running during path searches.
 * 
 * @author andrewbyrd
 *
 */
public class ServiceDay implements Serializable {
    private static final long serialVersionUID = -1206371243806996680L;

    protected long midnight;
    protected ServiceDate serviceDate;
    protected BitSet serviceIdsRunning;

    /* 
     * make a ServiceDay including the given time's day's starting second and a set of 
     * serviceIds running on that day.
     */
    public ServiceDay(Graph graph, long time, CalendarService cs, String agencyId) {
        TimeZone timeZone = cs.getTimeZoneForAgencyId(agencyId);
        GregorianCalendar calendar = new GregorianCalendar(timeZone);
        calendar.setTime(new Date(time * 1000));
        serviceDate = new ServiceDate(calendar);

        init(graph, cs, timeZone);
    }

    public ServiceDay(Graph graph, ServiceDate serviceDate, CalendarService cs, String agencyId) {
        TimeZone timeZone = cs.getTimeZoneForAgencyId(agencyId);
        this.serviceDate = new ServiceDate(serviceDate);

        init(graph, cs, timeZone);
    }

    private void init(Graph graph, CalendarService cs, TimeZone timeZone) {
        Date d = serviceDate.getAsDate(timeZone);
        this.midnight = d.getTime() / 1000;
        serviceIdsRunning = new BitSet(cs.getServiceIds().size());
        
        for (AgencyAndId serviceId : cs.getServiceIdsOnDate(serviceDate)) {
            int n = graph.serviceCodes.get(serviceId);
            if (n < 0)
                continue;
            serviceIdsRunning.set(n);
        }
    }

    /** Does the given serviceId run on this ServiceDay? */
    public boolean serviceRunning(int serviceCode) {
        return this.serviceIdsRunning.get(serviceCode);
    }

    /** Do any of the services for this set of service codes run on this ServiceDay? */
    public boolean anyServiceRunning(BitSet serviceCodes) {
        return this.serviceIdsRunning.intersects(serviceCodes);
    }

    /**
     * Return the ServiceDate for this ServiceDay.
     */
    public ServiceDate getServiceDate() {
        return serviceDate;
    }
    
    /* 
     * Return number of seconds after midnight on this ServiceDay
     * for the given time.
     * 
     * Note that the parameter and the return value are in seconds since the epoch
     * 
     * Return value may be negative, indicating that the time is 
     * before this ServiceDay.
     */
    public int secondsSinceMidnight(long time) {
        return (int) (time - this.midnight);
    }
    
    /* 
     * Return number of seconds since the epoch
     * based on the given number of seconds after midnight on this ServiceDay
     * 
     * Input value may be negative, indicating that the time is 
     * before this ServiceDay.
     */
    public long time(int secondsSinceMidnight) {
        return this.midnight + secondsSinceMidnight;
    }
    
    public String toString() {
        return Long.toString(this.midnight) + Arrays.asList(serviceIdsRunning);
    }
    
    public boolean equals(Object o) {
        if (!(o instanceof ServiceDay)) return false;
        ServiceDay other = (ServiceDay) o;
        return other.midnight == midnight;
    }
    
    @Override
    public int hashCode() {
        return (int) midnight;
    }
    
}
