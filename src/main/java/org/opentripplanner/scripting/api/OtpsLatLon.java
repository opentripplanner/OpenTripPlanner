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

package org.opentripplanner.scripting.api;

/**
 * Simple geographical coordinates.
 * 
 * @author laurent
 */
public class OtpsLatLon {

    private double lat, lon;

    protected OtpsLatLon(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    /**
     * @return The latitude.
     */
    public double getLat() {
        return lat;
    }

    /**
     * @return The longitude.
     */
    public double getLon() {
        return lon;
    }

    @Override
    public String toString() {
        return "(" + lat + "," + lon + ")";
    }

}
