package org.opentripplanner.geocoder;

public class GeocoderNullImpl implements Geocoder {
    
    static final String ERROR_MSG = "no geocoder configured";
    
    @Override
    public GeocoderResults geocode(String address) {
        return new GeocoderResults(ERROR_MSG);
    }
}
