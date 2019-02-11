package org.opentripplanner.geocoder.ws;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;

import javax.ws.rs.WebApplicationException;

import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.api.resource.ExternalGeocoderResource;
import org.opentripplanner.geocoder.Geocoder;
import org.opentripplanner.geocoder.GeocoderResult;
import org.opentripplanner.geocoder.GeocoderResults;

import org.locationtech.jts.geom.Envelope;

public class GeocoderServerTest {

    private ExternalGeocoderResource geocoderServer;

    @Before
    public void setUp() {
        geocoderServer = new ExternalGeocoderResource();
    }
    
    @Test(expected = WebApplicationException.class)
    public void testGeocodeNoAddress() {
        geocoderServer.geocode(null, null);
        fail("Should have thrown an error");
    }
    
    @Test
    public void testGeocodeValidAddress() {
        final double lat = 78.121;
        final double lng = -43.237;
        final String description = "121 elm street";
        
        geocoderServer.geocoder = new Geocoder() {
            @Override
            public GeocoderResults geocode(String address, Envelope bbox) {
                GeocoderResult result = new GeocoderResult(lat, lng, description);
                return new GeocoderResults(Arrays.asList(result));
            }
        };
        
        GeocoderResults results = geocoderServer.geocode("121 elm street", null);
        for (GeocoderResult result : results.getResults()) {
            // should only have one result
            assertEquals("description matches", description, result.getDescription());
            assertEquals(lat, result.getLat(), 0.001);
            assertEquals(lng, result.getLng(), 0.001);
        }
    }
    
    @Test
    public void testGeocodeInvalidAddress() {
        final String error = "uh oh";
        geocoderServer.geocoder = new Geocoder() {
            @Override
            public GeocoderResults geocode(String address, Envelope bbox) {
                return new GeocoderResults(error);
            }
        };

        GeocoderResults result = geocoderServer.geocode("121 elm street", null);
        assertEquals("error returned", error, result.getError());
    }
}
