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

package org.opentripplanner.geocoder;

import java.util.Arrays;

import com.vividsolutions.jts.geom.Envelope;

public class GeocoderStubImpl implements Geocoder {
    
    private double lat;
    private double lng;
    private String description;
    
    public GeocoderStubImpl() {
        this(40.719991, -73.99953, "148 Lafayette St,New York,NY,10013");
    }

    public GeocoderStubImpl(double lat, double lng, String description) {
        this.lat = lat;
        this.lng = lng;
        this.description = description;
    }

    @Override
    public GeocoderResults geocode(String address, Envelope bbox) {
        GeocoderResult result = new GeocoderResult(lat, lng, description);
        return new GeocoderResults(Arrays.asList(result));
    }

    
    public double getLat() {
        return lat;
    }

    
    public void setLat(double lat) {
        this.lat = lat;
    }

    
    public double getLng() {
        return lng;
    }

    
    public void setLng(double lng) {
        this.lng = lng;
    }

    
    public String getDescription() {
        return description;
    }

    
    public void setDescription(String description) {
        this.description = description;
    }

}
