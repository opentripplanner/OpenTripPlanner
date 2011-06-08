package org.opentripplanner.geocoder;

import java.util.ArrayList;
import java.util.List;

import org.opentripplanner.geocoder.Geocoder;
import org.opentripplanner.geocoder.GeocoderResult;
import org.opentripplanner.geocoder.GeocoderResults;

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
    public GeocoderResults geocode(String address) {
        GeocoderResults retval = decorated.geocode(address);
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
