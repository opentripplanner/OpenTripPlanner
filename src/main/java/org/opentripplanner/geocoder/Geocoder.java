package org.opentripplanner.geocoder;

import org.locationtech.jts.geom.Envelope;

public interface Geocoder {

    public GeocoderResults geocode(String address, Envelope bbox);

}