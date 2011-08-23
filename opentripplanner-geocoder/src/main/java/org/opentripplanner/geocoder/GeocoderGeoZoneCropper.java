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

package org.opentripplanner.geocoder;

import java.util.ArrayList;
import java.util.List;

import org.opentripplanner.geocoder.Geocoder;
import org.opentripplanner.geocoder.GeocoderResult;
import org.opentripplanner.geocoder.GeocoderResults;

import com.vividsolutions.jts.geom.Envelope;

/**
 * Filter results of a geocoding request by removing elements outside of the covered geographical
 * zone.
 */
public class GeocoderGeoZoneCropper implements Geocoder {

    private Geocoder decorated;

    private double minLat, minLon, maxLat, maxLon;

    public GeocoderGeoZoneCropper(Geocoder decorated, double minLat, double minLon, double maxLat,
            double maxLon) {
        this.minLat = minLat;
        this.minLon = minLon;
        this.maxLat = maxLat;
        this.maxLon = maxLon;
        this.decorated = decorated;
    }

    @Override
    public GeocoderResults geocode(String address, Envelope bbox) {
        GeocoderResults retval = decorated.geocode(address, bbox);
        if (retval.getResults() != null) {
            List<GeocoderResult> results = new ArrayList<GeocoderResult>(retval.getCount());
            for (GeocoderResult result : retval.getResults()) {
                if (result.getLat() > minLat && result.getLng() > minLon
                        && result.getLat() < maxLat && result.getLng() < maxLon)
                    results.add(result);
            }
            retval.setResults(results);
        }
        return retval;
    }

}
