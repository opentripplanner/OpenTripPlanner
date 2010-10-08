package org.opentripplanner.geocoder;

import static org.junit.Assert.*;

import org.junit.Test;

public class GeocoderNullImplTest {

    @Test
    public void testGeocode() {
        Geocoder nullGeocoder = new GeocoderNullImpl();
        GeocoderResults result = nullGeocoder.geocode("121 elm street");
        assertEquals("stub response", GeocoderNullImpl.ERROR_MSG, result.getError());
    }
}