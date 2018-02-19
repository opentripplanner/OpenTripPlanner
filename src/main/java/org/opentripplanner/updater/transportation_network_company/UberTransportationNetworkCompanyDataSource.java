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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class UberTransportationNetworkCompanyDataSource implements TransportationNetworkCompanyDataSource {

    private static final Logger LOG = LoggerFactory.getLogger(UberTransportationNetworkCompanyDataSource.class);

    private static final String UBER_API_URL = "https://api.uber.com/v1.2/";

    private String serverToken;
    private String baseUrl;

    public UberTransportationNetworkCompanyDataSource (String serverToken) {
        this.serverToken = serverToken;
        this.baseUrl = UBER_API_URL;
    }

    public UberTransportationNetworkCompanyDataSource (String serverToken, String baseUrl) {
        this.serverToken = serverToken;
        this.baseUrl = baseUrl;
    }

    @Override
    public List<ArrivalTime> getArrivalTimes(double lat, double lon) throws IOException {
        // prepare request
        UriBuilder uriBuilder = UriBuilder.fromUri(baseUrl + "estimates/time");
        uriBuilder.queryParam("start_latitude", lat);
        uriBuilder.queryParam("start_longitude", lon);
        String requestUrl = uriBuilder.toString();
        URL uberUrl = new URL(requestUrl);
        HttpURLConnection connection = (HttpURLConnection) uberUrl.openConnection();
        connection.setRequestProperty("Authorization", "Token " + serverToken);
        connection.setRequestProperty("Accept-Language", "en_US");
        connection.setRequestProperty("Content-Type", "application/json");

        LOG.info("Made request to uber API at following URL: " + requestUrl);

        // make request, parse repsonse
        InputStream responseStream = connection.getInputStream();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode json = objectMapper.readValue(responseStream, JsonNode.class);

        // serialize into Arrival Time objects
        ArrayList<ArrivalTime> arrivalTimes = new ArrayList<ArrivalTime>();

        ArrayNode times = (ArrayNode) json.get("times");

        LOG.info("Received " + times.size() + " uber arrival time estimates");

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
    public EstimatedRideTime getEstimatedRideTime(
        String productId,
        double startLatitude,
        double startLongitude,
        double endLatitude,
        double endLongitude
    ) throws IOException {
        // prepare request
        UriBuilder uriBuilder = UriBuilder.fromUri(baseUrl + "estimates/time");
        uriBuilder.queryParam("start_latitude", startLatitude);
        uriBuilder.queryParam("start_longitude", startLongitude);
        uriBuilder.queryParam("end_latitude", endLatitude);
        uriBuilder.queryParam("end_longitude", endLongitude);
        String requestUrl = uriBuilder.toString();
        URL uberUrl = new URL(requestUrl);
        HttpURLConnection connection = (HttpURLConnection) uberUrl.openConnection();
        connection.setRequestProperty("Authorization", "Token " + serverToken);
        connection.setRequestProperty("Accept-Language", "en_US");
        connection.setRequestProperty("Content-Type", "application/json");

        LOG.info("Made request to uber API at following URL: " + requestUrl);

        // make request, parse repsonse
        InputStream responseStream = connection.getInputStream();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode json = objectMapper.readValue(responseStream, JsonNode.class);

        ArrayNode prices = (ArrayNode) json.get("prices");

        LOG.info("Recieved " + prices.size() + " uber price/time estimates");

        EstimatedRideTime rideTime = null;

        for (final JsonNode price: prices) {
            if (price.get("product_id").textValue().equals(productId)) {
                rideTime = new EstimatedRideTime(price.get("duration").asInt());
                break;
            }
        }

        return rideTime;
    }
}
