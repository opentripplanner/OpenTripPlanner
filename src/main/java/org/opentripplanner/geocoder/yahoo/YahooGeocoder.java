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

package org.opentripplanner.geocoder.yahoo;

import com.vividsolutions.jts.geom.Envelope;
import org.opentripplanner.geocoder.Geocoder;
import org.opentripplanner.geocoder.GeocoderResult;
import org.opentripplanner.geocoder.GeocoderResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class YahooGeocoder implements Geocoder {
	private static final Logger LOG = LoggerFactory.getLogger(YahooGeocoder.class);

	private String appId;
	private String locale;
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
	
	public String getLocale() {
		return locale;
	}
	
	public void setLocale(String locale) {
		this.locale = locale;
	}

	@Override
	public GeocoderResults geocode(String address, Envelope bbox) {
		if (appId == null) throw new NullPointerException("appid not set");
		
		String content = null;
		
		try {
			// make json request
			URL googleGeocoderUrl = getYahooGeocoderUrl(address);
            URLConnection conn = googleGeocoderUrl.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
            
            StringBuilder sb = new StringBuilder(128);
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            reader.close();
            content = sb.toString();

		} catch (IOException e) {
			LOG.error("Error parsing geocoder response", e);
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
		if (locale != null) {
			uriBuilder.queryParam("locale", locale);
			uriBuilder.queryParam("gflags", "L");
		}
		URI uri = uriBuilder.build();
		return new URL(uri.toString());
	}


	private GeocoderResults noGeocoderResult(String error) {
		return new GeocoderResults(error);
	}

}
