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

package org.opentripplanner.updater.transportation_network_company;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.opentripplanner.routing.transportation_network_company.ArrivalTime;
import org.opentripplanner.routing.transportation_network_company.EstimatedRideTime;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class UberTransportationNetworkCompanyDataSource implements TransportationNetworkCompanyDataSource {
    private static final String UBER_API_URL = "https://api.uber.com/v1.2/";

    private String authToken;
    private String baseUrl;

    public UberTransportationNetworkCompanyDataSource (String authToken) {
        this.authToken = authToken;
        this.baseUrl = UBER_API_URL;
    }

    public UberTransportationNetworkCompanyDataSource (String authToken, String baseUrl) {
        this.authToken = authToken;
        this.baseUrl = baseUrl;
    }

    @Override
    public List<ArrivalTime> getArrivalTimes(double lat, double lon) throws IOException {
        // prepare request
        UriBuilder uriBuilder = UriBuilder.fromUri(baseUrl + "estimates/time");
        uriBuilder.queryParam("start_latitude", lat);
        uriBuilder.queryParam("start_longitude", lon);
        URL uberUrl = new URL(uriBuilder.toString());
        HttpURLConnection connection = (HttpURLConnection) uberUrl.openConnection();
        connection.setRequestProperty("Authorization", authToken);
        connection.setRequestProperty("Accept-Language", "en_US");
        connection.setRequestProperty("Content-Type", "application/json");

        // make request, parse repsonse
        InputStream responseStream = connection.getInputStream();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode json = objectMapper.readValue(responseStream, JsonNode.class);

        // serialize into Arrival Time objects
        ArrayList<ArrivalTime> arrivalTimes = new ArrayList<ArrivalTime>();

        ArrayNode times = (ArrayNode) json.get("times");

        for (final JsonNode time: times) {
            arrivalTimes.add(
                new ArrivalTime(
                    time.get("product_id").asText(),
                    time.get("localized_display_name").asText(),
                    time.get("estimate").asInt()
                )
            );
        }

        return arrivalTimes;
    }

    @Override
    public EstimatedRideTime getEstimatedRideTime(double startLat, double startLon, double endLat, double endLon) {
        return null;
    }
}
