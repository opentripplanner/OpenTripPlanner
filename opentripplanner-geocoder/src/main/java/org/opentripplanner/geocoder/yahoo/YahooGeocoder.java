package org.opentripplanner.geocoder.yahoo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.UriBuilder;

import org.opentripplanner.geocoder.Geocoder;
import org.opentripplanner.geocoder.GeocoderResult;
import org.opentripplanner.geocoder.GeocoderResults;

public class YahooGeocoder implements Geocoder {

	private String appId;
	private YahooJsonDeserializer yahooJsonDeserializer;

	public YahooGeocoder() {
		yahooJsonDeserializer = new YahooJsonDeserializer();
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	@Override
	public GeocoderResults geocode(String address) {
		if (appId == null) throw new NullPointerException("appid not set");
		
		String content = null;
		
		try {
			// make json request
			URL googleGeocoderUrl = getYahooGeocoderUrl(address);
            URLConnection conn = googleGeocoderUrl.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            
            StringBuilder sb = new StringBuilder(128);
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            reader.close();
            content = sb.toString();

		} catch (IOException e) {
			e.printStackTrace();
			return noGeocoderResult("Error parsing geocoder response");
		}
		
		YahooGeocoderResults yahooGeocoderResults = yahooJsonDeserializer.parseResults(content);
		YahooGeocoderResultSet resultSet = yahooGeocoderResults.getResultSet();
		List<YahooGeocoderResult> results = resultSet.getResults();
		List<GeocoderResult> geocoderResults = new ArrayList<GeocoderResult>();
		for (YahooGeocoderResult yahooGeocoderResult : results) {
			double lat = yahooGeocoderResult.getLatDouble();
			double lng = yahooGeocoderResult.getLngDouble();
			String line1 = yahooGeocoderResult.getLine1();
			String line2 = yahooGeocoderResult.getLine2();
			String addressString = null;
			if (line1 != null && !line1.trim().isEmpty()) {
				addressString = line1 + ", " + line2;
			} else {
				addressString = line2;
			}
			geocoderResults.add(new GeocoderResult(lat, lng, addressString));
		}
		return new GeocoderResults(geocoderResults);
	}
	
	private URL getYahooGeocoderUrl(String address) throws IOException {
		UriBuilder uriBuilder = UriBuilder.fromUri("http://where.yahooapis.com/geocode");
		uriBuilder.queryParam("location", address);
		uriBuilder.queryParam("flags", "J");
		uriBuilder.queryParam("appid", appId);
		URI uri = uriBuilder.build();
		return new URL(uri.toString());
	}


	private GeocoderResults noGeocoderResult(String error) {
		return new GeocoderResults(error);
	}

}
