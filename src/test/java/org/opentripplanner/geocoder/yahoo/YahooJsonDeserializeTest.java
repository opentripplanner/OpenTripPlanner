package org.opentripplanner.geocoder.yahoo;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.junit.Test;

public class YahooJsonDeserializeTest {

	@Test
	public void testDeserialize() throws Exception {
        String json = getJsonString();
        
        YahooJsonDeserializer yahooJsonDeserializer = new YahooJsonDeserializer();
		YahooGeocoderResults geocoderResults = yahooJsonDeserializer.parseResults(json);
		
        YahooGeocoderResultSet resultSet = geocoderResults.getResultSet();
        assertNotNull("didn't parse yahoo results correctly", resultSet);
        
        List<YahooGeocoderResult> results = resultSet.getResults();
        assertNotNull("didn't parse yahoo results correctly", results);
        assertEquals("unexpected number of results", 1, results.size());
        
        YahooGeocoderResult yahooGeocoderResult = results.get(0);
        
        // verify geometry
        double lat = yahooGeocoderResult.getLatDouble();
        double lng = yahooGeocoderResult.getLngDouble();
        assertEquals(37.779160, lat, 0.00001);
        assertEquals(-122.420049, lng, 0.00001);
        
        // verify address lines
        String line1 = yahooGeocoderResult.getLine1();
        String line2 = yahooGeocoderResult.getLine2();
        assertEquals("first yahoo address line wrong", "", line1);
        assertEquals("second yahoo address line wrong", "San Francisco, CA", line2);
	}

	private String getJsonString() throws IOException {
		InputStream inputStream = this.getClass().getResourceAsStream("jsonYahooResult.txt");
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder sb = new StringBuilder(255);
        String line = null;
        while ((line = reader.readLine()) != null) {
        	sb.append(line);
        }
        return sb.toString();
	}
	
}
