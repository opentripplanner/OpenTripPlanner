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

package org.opentripplanner.geocoder.bano;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vividsolutions.jts.geom.Envelope;
import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.GeoJsonObject;
import org.geojson.Point;
import org.opentripplanner.geocoder.Geocoder;
import org.opentripplanner.geocoder.GeocoderResult;
import org.opentripplanner.geocoder.GeocoderResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * A geocoder using the data.gouv.fr API of BANO (Base Nationale d'Adresse Ouverte), the official
 * open-data address source covering the whole of France.
 *
 * The returned data is rather simple to use, as it returns a GeoJSON features collection.
 * 
 * Obviously, this geocoder will only work in France.
 *
 * @author laurent
 */
public class BanoGeocoder implements Geocoder {
    private static final Logger LOG = LoggerFactory.getLogger(BanoGeocoder.class);

    private static final String BANO_URL = "http://api.adresse.data.gouv.fr/search/";

    private static final int CLAMP_RESULTS = 10;

    private ObjectMapper mapper;

    public BanoGeocoder() {
        mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     */
    @Override
    public GeocoderResults geocode(String address, Envelope bbox) {

        try {
            URL banoUrl = getBanoGeocoderUrl(address, bbox);
            URLConnection conn = banoUrl.openConnection();
            InputStream in = conn.getInputStream();
            FeatureCollection featureCollection = mapper.readValue(in, FeatureCollection.class);
            in.close();

            List<GeocoderResult> geocoderResults = new ArrayList<GeocoderResult>();
            for (Feature feature : featureCollection.getFeatures()) {
                GeoJsonObject geom = feature.getGeometry();
                if (geom instanceof Point) {
                    Point p = (Point) geom;
                    GeocoderResult res = new GeocoderResult();
                    res.setLat(p.getCoordinates().getLatitude());
                    res.setLng(p.getCoordinates().getLongitude());
                    res.setDescription(feature.getProperties().get("label").toString());
                    /*
                     * Note: We also have here as properties a break-down of other details, such as
                     * the house number, street, city, postcode... Can be useful if needed.
                     */
                    geocoderResults.add(res);
                } else {
                    // Should not happen according to the API
                }
            }
            return new GeocoderResults(geocoderResults);

        } catch (IOException e) {
            LOG.error("Error processing BANO geocoder results", e);
            return new GeocoderResults(e.getLocalizedMessage());
        }
    }

    private URL getBanoGeocoderUrl(String address, Envelope bbox) throws IOException {
        UriBuilder uriBuilder = UriBuilder.fromUri(BANO_URL);
        uriBuilder.queryParam("q", address);
        uriBuilder.queryParam("limit", CLAMP_RESULTS);
        if (bbox != null) {
            uriBuilder.queryParam("lat", bbox.centre().y);
            uriBuilder.queryParam("lon", bbox.centre().x);
        }
        URI uri = uriBuilder.build();
        return new URL(uri.toString());
    }
}
