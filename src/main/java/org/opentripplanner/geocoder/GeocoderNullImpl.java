package org.opentripplanner.geocoder;

import org.locationtech.jts.geom.Envelope;

public class GeocoderNullImpl implements Geocoder {
    
    static final String ERROR_MSG = "no geocoder configured";
    
    @Override
    public GeocoderResults geocode(String address, Envelope bbox) {
        return new GeocoderResults(ERROR_MSG);
    }
}
