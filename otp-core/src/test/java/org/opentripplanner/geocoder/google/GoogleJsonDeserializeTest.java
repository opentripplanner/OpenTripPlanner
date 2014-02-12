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

package org.opentripplanner.geocoder.google;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.junit.Test;
import org.opentripplanner.geocoder.google.GoogleGeocoderResults;


public class GoogleJsonDeserializeTest {

	@Test
	public void testDeserialize() throws Exception {
        String json = getJsonString();
        
        GoogleJsonDeserializer googleJsonDeserializer = new GoogleJsonDeserializer();
		GoogleGeocoderResults geocoderResults = googleJsonDeserializer.parseResults(json);
        List<GoogleGeocoderResult> results = geocoderResults.getResults();
        
        assertEquals("unexpected number of results", 1, results.size());
        
        GoogleGeocoderResult googleGeocoderResult = results.get(0);
        
        // verify geometry
        Geometry geometry = googleGeocoderResult.getGeometry();
        Location location = geometry.getLocation();
        double lat = location.getLat();
        double lng = location.getLng();
        assertEquals(37.4217080, lat, 0.00001);
        assertEquals(-122.0829964, lng, 0.00001);
        
        // verify formatted address of response
        String formattedAddress = googleGeocoderResult.getFormatted_address();
        assertEquals("invalid address", "1600 Amphitheatre Pkwy, Mountain View, CA 94043, USA", formattedAddress);
	}

	private String getJsonString() throws IOException {
		InputStream inputStream = this.getClass().getResourceAsStream("jsonGoogleResult.txt");
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder sb = new StringBuilder(255);
        String line = null;
        while ((line = reader.readLine()) != null) {
        	sb.append(line);
        }
        return sb.toString();
	}
	
}
