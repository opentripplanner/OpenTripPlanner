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

package org.opentripplanner.geocoder.nominatim;

public class NominatimGeocoderResult {
    private String lat;
    private String lon;
    private String display_name;
    private String osm_type;
    
    public void setLat(String lat) {
        this.lat = lat;
    }
    
    public String getLat() {
        return lat;
    }
    
    public void setLon(String lon) {
        this.lon = lon;
    }

    public String getLon() {
        return lon;
    }
    
    public double getLatDouble() {
        return Double.parseDouble(lat);
    }
    
    public double getLngDouble() {
        return Double.parseDouble(lon);
    }
    
    public void setDisplay_name(String displayName) {
        this.display_name = displayName;
    }
    
    public String getDisplay_name() {
        return display_name;
    }
    
    public void setOsm_type(String osm_type) {
        this.osm_type = osm_type;
    }

    public String getOsm_type() {
        return osm_type;
    }
}