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

import java.util.Date;
import java.util.Set;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.gtfs.services.calendar.CalendarService;

/**
 * Represents a day of transit services. 
 * Intended for quickly checking whether a service is running during path searches.
 * 
 * @author andrewbyrd
 *
 */
public class ServiceDay {
    protected long midnight;
    protected Set<AgencyAndId> serviceIdsRunning;
    
    /* 
     * make a ServiceDay including the given time's day's starting second and a set of 
     * serviceIds running on that day.
     */
    public ServiceDay(long time, CalendarService cs) {
        ServiceDate sd = new ServiceDate(new Date(time));
        Date d = sd.getAsDate();        
        this.midnight = d.getTime();
        this.serviceIdsRunning = cs.getServiceIdsOnDate(sd);
    }

    /* 
     * Does the given serviceId run on this ServiceDay?
     */
    public boolean serviceIdRunning(AgencyAndId serviceId) {
        return this.serviceIdsRunning.contains(serviceId);
    }

    /* 
     * Return number of seconds after midnight on this ServiceDay
     * for the given time.
     * 
     * Note that time is in milliseconds since the epoch,
     * and return value is in seconds.
     * 
     * Return value may be negative, indicating that the time is 
     * before this ServiceDay.
     */
    public int secondsSinceMidnight(long time) {
        return (int) (time - this.midnight) / 1000;
    }
    
    /* 
     * Return number of milliseconds since the epoch
     * based on the given number of seconds after midnight on this ServiceDay
     * 
     * Input value may be negative, indicating that the time is 
     * before this ServiceDay.
     */
    public long time(int secondsSinceMidnight) {
        return this.midnight + (1000L * (long)secondsSinceMidnight);
    }
    
    public String toString() {
        return Long.toString(this.midnight / 1000) + serviceIdsRunning;
    }
}
