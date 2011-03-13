/**
 * 
 */
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
