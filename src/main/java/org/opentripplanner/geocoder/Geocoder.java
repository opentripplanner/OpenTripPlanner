package org.opentripplanner.geocoder;

import com.vividsolutions.jts.geom.Envelope;

public interface Geocoder {

    public GeocoderResults geocode(String address, Envelope bbox);

}