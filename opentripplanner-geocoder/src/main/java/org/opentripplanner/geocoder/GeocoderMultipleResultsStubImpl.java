package org.opentripplanner.geocoder;

import java.util.Collection;


public class GeocoderMultipleResultsStubImpl implements Geocoder {
    
    private Collection<GeocoderResult> results;

    public GeocoderMultipleResultsStubImpl(Collection<GeocoderResult> results) {
        this.results = results;
    }

    @Override
    public GeocoderResults geocode(String address) {
        return new GeocoderResults(results);
    }

}
