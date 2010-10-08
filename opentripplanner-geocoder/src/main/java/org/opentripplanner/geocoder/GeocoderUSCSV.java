package org.opentripplanner.geocoder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;

import javax.ws.rs.core.UriBuilder;

public class GeocoderUSCSV implements Geocoder {

    private String geocoderBaseUrl;

    public void setGeocoderBaseUrl(String geocoderBaseUrl) {
        this.geocoderBaseUrl = geocoderBaseUrl;
    }

    private URL getGeocoderURL(String geocoderBaseUrl, String address) throws MalformedURLException {
        UriBuilder builder = UriBuilder.fromUri(geocoderBaseUrl);
        builder.queryParam("address", address);
        URI uri = builder.build();
        return new URL(uri.toString());
    }

    /* (non-Javadoc)
     * @see org.opentripplanner.api.geocode.Geocoder#geocode(java.lang.String)
     */
    @Override
    public GeocoderResults geocode(String address) {
        assert geocoderBaseUrl != null;
        
        String content = null;        
        
        try {
            URL url = getGeocoderURL(geocoderBaseUrl, address);
            URLConnection conn = url.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            
            StringBuilder sb = new StringBuilder(128);
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            reader.close();
            content = sb.toString();
        } catch (MalformedURLException e) {
            return noGeocoderResult("invalid geocoder");
        } catch (IOException e) {
            return noGeocoderResult("communication error");
        }
        
        Collection<GeocoderResult> results = new ArrayList<GeocoderResult>();
        try {
            String[] lines = content.split("\n");
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                GeocoderResult result = parseGeocoderResult(line);
                results.add(result);
            }
        } catch (NumberFormatException e) {
            return noGeocoderResult(content);
        } catch (ArrayIndexOutOfBoundsException e) {
            return noGeocoderResult(content);
        }
        return new GeocoderResults(results);
    }

    private GeocoderResult parseGeocoderResult(String line) {
        String[] fields = line.split(",", 3);
        double lat = Double.parseDouble(fields[0]);
        double lng = Double.parseDouble(fields[1]);
        String description = fields[2];
        return new GeocoderResult(lat, lng, description);
    }

    private GeocoderResults noGeocoderResult(String content) {
        // use the response as the error message returned back
        return new GeocoderResults(content);
    }
}
