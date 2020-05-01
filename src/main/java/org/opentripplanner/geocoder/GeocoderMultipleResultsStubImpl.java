package org.opentripplanner.geocoder;

import java.util.Collection;

import org.locationtech.jts.geom.Envelope;


public class GeocoderMultipleResultsStubImpl implements Geocoder {
    
    private Collection<GeocoderResult> results;

    public GeocoderMultipleResultsStubImpl(Collection<GeocoderResult> results) {
        this.results = results;
    }

    @Override
    public GeocoderResults geocode(String address, Envelope bbox) {
        return new GeocoderResults(results);
    }

}
