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

/**
 * Used in the SampleCache. Bins sample requests by rounded latitude and longitude.
 * This prevents cache misses due to projection and floating point slop. Pixel bins 
 * will become visible at higher zoom levels, but initial rendering of tiles at those 
 * levels will be much faster and use less memory. 
 */
public class SampleRequest {

    public final int lon; 
    public final int lat;

    /* rounding to 4 decimal places gives house-sized pixel bins. */
    public SampleRequest(double lon, double lat) {
        this.lon = (int) (lon * 10000);
        this.lat = (int) (lat * 10000);
    }
    
    //  90 degrees to 4 decimal digits = 900000.
    //  900000 << 12 = 3686400000 -- fits into a 32 bit int with wraparound. 
    public int hashCode() {
        return ((lat << 10) ^ lon);
    }
    
    public boolean equals(Object other) {
        if (other instanceof SampleRequest) {
            SampleRequest that = (SampleRequest) other;
            return this.lon  == that.lon &&
                    this.lat  == that.lat ;
        }
        return false;
    }

    public String toString() {
        return String.format("<Sample request (integer binned), lon=%d lat=%d>", lon, lat);
    }

}
