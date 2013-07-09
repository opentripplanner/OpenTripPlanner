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

package org.opentripplanner.analyst.request;

import java.util.GregorianCalendar;

public class SPTRequest {

    public final double lon; 
    public final double lat;
    public final long time;

    public SPTRequest(double lon, double lat, GregorianCalendar gcal) {
        this.lon = lon;
        this.lat = lat;
        if (gcal != null)
            this.time = gcal.getTimeInMillis() / 1000;
        else 
            this.time = System.currentTimeMillis() / 1000;
    }

    public SPTRequest(double lon, double lat, long time) {
        this.lon = lon;
        this.lat = lat;
        this.time = time;
    }
    
    public int hashCode() {
        return (int)(lon * 42677 + lat * 1307 + time);
    }
    
    public boolean equals(Object other) {
        if (other instanceof SPTRequest) {
            SPTRequest that = (SPTRequest) other;
            return this.lon  == that.lon &&
                   this.lat  == that.lat &&
                   this.time == that.time;
        }
        return false;
    }

    public String toString() {
        return String.format("<SPT request, lon=%f lat=%f time=%d>", lon, lat, time);
    }
}
