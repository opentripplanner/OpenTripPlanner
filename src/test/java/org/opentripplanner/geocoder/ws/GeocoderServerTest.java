/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

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

import com.vividsolutions.jts.geom.Envelope;

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
