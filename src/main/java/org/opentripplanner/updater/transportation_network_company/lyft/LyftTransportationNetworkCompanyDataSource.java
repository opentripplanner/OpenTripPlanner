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

package org.opentripplanner.updater.transportation_network_company.lyft;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opentripplanner.routing.transportation_network_company.ArrivalTime;
import org.opentripplanner.routing.transportation_network_company.RideEstimate;
import org.opentripplanner.routing.transportation_network_company.TransportationNetworkCompany;
import org.opentripplanner.updater.transportation_network_company.Position;
import org.opentripplanner.updater.transportation_network_company.RideEstimateRequest;
import org.opentripplanner.updater.transportation_network_company.TransportationNetworkCompanyDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class LyftTransportationNetworkCompanyDataSource extends TransportationNetworkCompanyDataSource {

    private static final Logger LOG = LoggerFactory.getLogger(LyftTransportationNetworkCompanyDataSource.class);

    private static final String LYFT_API_URL = "https://api.lyft.com/";

    private String accessToken;
    private String baseUrl;  // for testing purposes
    private String clientId;
    private String clientSecret;
    private Date tokenExpirationTime;

    public LyftTransportationNetworkCompanyDataSource(JsonNode config) {
        this.baseUrl = LYFT_API_URL;
        this.clientId = config.path("clientId").asText();
        this.clientSecret = config.path("clientSecret").asText();
        this.wheelChairAccessibleRideType = config.path("wheelChairAccessibleRideType").asText();
    }

    // intended for use during testing
    public LyftTransportationNetworkCompanyDataSource(String baseUrl, String clientId, String clientSecret) {
        this.baseUrl = baseUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    private String getAccessToken() throws IOException {
        // check if token needs to be obtained
        Date now = new Date();
        if (tokenExpirationTime == null || now.after(tokenExpirationTime)) {
            // token needs to be obtained
            LOG.info("Requesting new lyft access token");

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            // prepare request to get token
            UriBuilder uriBuilder = UriBuilder.fromUri(baseUrl + "oauth/token");
            URL url = new URL(uriBuilder.toString());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            String userpass = clientId + ":" + clientSecret;
            String basicAuth = "Basic " + javax.xml.bind.DatatypeConverter.printBase64Binary(userpass.getBytes());
            connection.setRequestProperty("Authorization", basicAuth);
            connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");

            // set request body
            LyftAuthenticationRequestBody authRequest = new LyftAuthenticationRequestBody(
                "client_credentials",
                "public"
            );
            connection.setDoOutput(true);
            mapper.writeValue(connection.getOutputStream(), authRequest);

            // send request and parse repsonse
            InputStream responseStream = connection.getInputStream();
            LyftAuthenticationResponse response = mapper.readValue(responseStream, LyftAuthenticationResponse.class);
            accessToken = response.access_token;
            tokenExpirationTime = new Date();
            tokenExpirationTime.setTime(tokenExpirationTime.getTime() + (response.expires_in - 60) * 1000);

            LOG.info("Received new lyft access token");
        }

        return accessToken;
    }

    @Override
    public TransportationNetworkCompany getTransportationNetworkCompanyType() {
        return TransportationNetworkCompany.LYFT;
    }

    @Override
    public List<ArrivalTime> queryArrivalTimes(Position request) throws IOException {
        // prepare request
        UriBuilder uriBuilder = UriBuilder.fromUri(baseUrl + "v1/eta");
        uriBuilder.queryParam("lat", request.latitude);
        uriBuilder.queryParam("lng", request.longitude);
        String requestUrl = uriBuilder.toString();
        URL url = new URL(requestUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Authorization", "Bearer " + getAccessToken());
        connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");

        LOG.info("Made request to lyft API at following URL: " + requestUrl);

        // make request, parse repsonse
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        if (connection.getResponseCode() < HttpURLConnection.HTTP_BAD_REQUEST) {
            LyftArrivalEstimateResponse response = mapper.readValue(
                connection.getInputStream(),
                LyftArrivalEstimateResponse.class
            );

            // serialize into Arrival Time objects
            ArrayList<ArrivalTime> arrivalTimes = new ArrayList<ArrivalTime>();

            LOG.info("Received " + response.eta_estimates.size() + " lyft arrival time estimates");

            for (final LyftArrivalEstimate time: response.eta_estimates) {
                arrivalTimes.add(
                    new ArrivalTime(
                        TransportationNetworkCompany.LYFT,
                        time.ride_type,
                        time.display_name,
                        time.eta_seconds,
                        productIsWheelChairAccessible(time.ride_type)
                    )
                );
            }

            return arrivalTimes;
        } else {
            LyftError error = mapper.readValue(connection.getErrorStream(), LyftError.class);
            if (
                error.error != null &&
                    (error.error.equals("no_service_in_area") ||
                        error.error.equals("ridetype_unavailable_in_region"))
            ) {
                LOG.warn(error.toString());
                LOG.warn(
                    "No Lyft service available at {}, {}",
                    request.latitude,
                    request.longitude
                );
                return Collections.emptyList();
            }
            LOG.error(error.toString());
            if (error.error_description != null) {
                throw new IOException(error.error_description);
            }
            throw new IOException("received an error from the Lyft API");
        }
    }

    @Override
    public List<RideEstimate> queryRideEstimates(
        RideEstimateRequest request
    ) throws IOException {
        // prepare request
        UriBuilder uriBuilder = UriBuilder.fromUri(baseUrl + "v1/cost");
        uriBuilder.queryParam("start_lat", request.startPosition.latitude);
        uriBuilder.queryParam("start_lng", request.startPosition.longitude);
        uriBuilder.queryParam("end_lat", request.endPosition.latitude);
        uriBuilder.queryParam("end_lng", request.endPosition.longitude);
        String requestUrl = uriBuilder.toString();
        URL url = new URL(requestUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Authorization", "Bearer " + getAccessToken());
        connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");

        LOG.info("Made request to lyft API at following URL: " + requestUrl);

        // make request, parse repsonse
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        if (connection.getResponseCode() < HttpURLConnection.HTTP_BAD_REQUEST) {
            InputStream responseStream = connection.getInputStream();
            LyftRideEstimateResponse response = mapper.readValue(responseStream, LyftRideEstimateResponse.class);

            if (response.cost_estimates == null) {
                throw new IOException("Unrecocginzed response format");
            }

            LOG.info("Recieved " + response.cost_estimates.size() + " lyft price/time estimates");

            List<RideEstimate> estimates = new ArrayList<RideEstimate>();

            for (final LyftRideEstimate estimate: response.cost_estimates) {
                estimates.add(new RideEstimate(
                    TransportationNetworkCompany.LYFT,
                    estimate.currency,
                    estimate.estimated_duration_seconds,
                    // Lyft's esimated cost is in the "minor" unit, so the following
                    // may not work in countries that don't have 100 minor units per major unit
                    // see https://en.wikipedia.org/wiki/ISO_4217#Treatment_of_minor_currency_units_(the_"exponent")
                    estimate.estimated_cost_cents_max / 100.0,
                    estimate.estimated_cost_cents_min / 100.0,
                    estimate.ride_type,
                    productIsWheelChairAccessible(estimate.ride_type)
                ));
            }

            return estimates;
        } else {
            LyftError error = mapper.readValue(connection.getErrorStream(), LyftError.class);
            if (
                error.error != null &&
                    (error.error.equals("no_service_in_area") ||
                        error.error.equals("ridetype_unavailable_in_region"))
            ) {
                LOG.warn(error.toString());
                LOG.warn(
                    "No Lyft service available for trip from {}, {} to {}, {}",
                    request.startPosition.latitude,
                    request.startPosition.longitude,
                    request.endPosition.latitude,
                    request.endPosition.longitude
                );
                return Collections.emptyList();
            }
            LOG.error(error.toString());
            if (error.error_description != null) {
                throw new IOException(error.error_description);
            }
            throw new IOException("received an error from the Lyft API");
        }
    }
}