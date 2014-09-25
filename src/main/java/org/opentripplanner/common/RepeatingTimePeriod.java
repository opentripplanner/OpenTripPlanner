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

package org.opentripplanner.common;

import java.io.Serializable;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Represents a repeating time period, used for opening hours &c.
 * For instance: Monday - Friday 8AM to 8PM, Satuday 10AM to 5PM, Sunday closed.
 * For now it is week-based so doesn't handle every possible case, but since it is encapsulated 
 * that could conceivably be changed.
 * 
 * @author mattwigway
 *
 */
public class RepeatingTimePeriod implements Serializable {
    private static final long serialVersionUID = -5977328371879835782L;
    
    private RepeatingTimePeriod () {
        this.timeZone = null;
    }
    
    /** 
     * This stores the time periods this is active/open, stored as seconds from noon
     * (positive or negative) on the given day.
     */
    private int[][] monday;
    private int[][] tuesday;
    private int[][] wednesday;
    private int[][] thursday;
    private int[][] friday;
    private int[][] saturday;
    private int[][] sunday;
    
    /**
     * The timezone this is represented in.
     */
    private TimeZone timeZone;
   
    /**
     * Parse the time specification from an OSM turn restriction
     * @param day_on
     * @param day_off
     * @param hour_on
     * @param hour_off
     * @return
     */
    public static RepeatingTimePeriod parseFromOsmTurnRestriction (String day_on, String day_off, 
            String hour_on, String hour_off) {
        // first, create the opening and closing times. This is easy because there is the same one
        // every day of the week that this restriction is in force.
        String[] parsedOn = hour_on.split(";");
        String[] parsedOff = hour_off.split(";");
        if (parsedOn.length != parsedOff.length)
            return null;
        
        int[][] onOff = new int[parsedOn.length][];
        
        for (int i = 0; i < parsedOn.length; i++) {
            onOff[i] = new int[] {parseHour(parsedOn[i]), parseHour(parsedOff[i])};
        }
             
        boolean active = false;
        RepeatingTimePeriod ret = new RepeatingTimePeriod();
        
        // loop through twice to handle cases like Saturday - Tuesday
        for (String today : new String[] {"monday", "tuesday", "wednesday", "thursday", "friday",
                "saturday", "sunday", "monday", "tuesday", "wednesday", "thursday", "friday",
                "saturday", "sunday"}) {
            
            if (today.startsWith(day_on.toLowerCase()))
                active = true;
            
            if (active) {
                if (today == "monday")
                    ret.monday = onOff;
                
                else if (today == "tuesday")
                    ret.tuesday = onOff;
        
                else if (today == "wednesday")
                    ret.wednesday = onOff;
                    
                else if (today == "thursday")
                    ret.thursday = onOff;
                
                else if (today == "friday")
                    ret.friday = onOff;
                
                else if (today == "saturday")
                    ret.saturday = onOff;
                
                else if (today == "sunday")
                    ret.sunday = onOff;
            }
                
            if (today.startsWith(day_off.toLowerCase()))
                active = false;
        }
        
        return ret;
    }
    
    /**
     * Return seconds before or after noon for the given hour. 
     * @param hour
     */
    private static int parseHour(String hour) {
        String[] parsed = hour.split(":");
        int ret = Integer.parseInt(parsed[0]) * 3600;
        
        if (parsed.length >= 2) {
            ret += Integer.parseInt(parsed[1]) * 60;
        }
        
        // subtract 12 hours to make it noon-relative. This implicitly handles DST.
        ret -= 12 * 3600;
        
        return ret;
    }

    public boolean active(long time) {
        // TODO: Timezone/locale
        Calendar cal;
        // TODO offsets
        if (this.timeZone != null)
            cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        else
            // FIXME hardwired time zone
            cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        
        cal.setTimeInMillis(time * 1000);
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        
        int[][] times = null;
                
        switch(dayOfWeek) {
        case Calendar.MONDAY:
            times = monday;
            break;
        case Calendar.TUESDAY:
            times = tuesday;
            break;
        case Calendar.WEDNESDAY:
            times = wednesday;
            break;
        case Calendar.THURSDAY:
            times = thursday;
            break;
        case Calendar.FRIDAY:
            times = friday;
            break;
        case Calendar.SATURDAY:
            times = saturday;
            break;
        case Calendar.SUNDAY:
            times = sunday;
            break;
        }

        if (times == null) {
            //no restriction today
            return false;
        }
        
        int timeOfDay = cal.get(Calendar.HOUR_OF_DAY) * 3600 + cal.get(Calendar.MINUTE) * 60 +
                cal.get(Calendar.SECOND) - 12 * 3600; 
        
        for (int[] range : times) {
            if (timeOfDay >= range[0] && timeOfDay <= range[1])
                return true;
        }
        
        return false;   
    }
}
