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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opentripplanner.routing.transportation_network_company.ArrivalTime;
import org.opentripplanner.routing.transportation_network_company.EstimatedRideTime;
import org.opentripplanner.updater.transportation_network_company.TransportationNetworkCompanyDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class LyftTransportationNetworkCompanyDataSource implements TransportationNetworkCompanyDataSource {

    private static final Logger LOG = LoggerFactory.getLogger(LyftTransportationNetworkCompanyDataSource.class);

    private static final String LYFT_API_URL = "https://api.lyft.com/";

    private String accessToken;
    private String baseUrl;  // for testing purposes
    private String clientId;
    private String clientSecret;
    private Date tokenExpirationTime;

    public LyftTransportationNetworkCompanyDataSource(String clientId, String clientSecret) {
        this.baseUrl = LYFT_API_URL;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public LyftTransportationNetworkCompanyDataSource(String baseUrl, String clientId, String clientSecret) {
        this.baseUrl = baseUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    private String getAccessToken() throws IOException {
        // check if token needs to be obtained
        if (tokenExpirationTime == null || tokenExpirationTime.after(new Date())) {
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
            tokenExpirationTime.setTime(tokenExpirationTime.getTime() + response.expires_in - 60);

            LOG.info("Received new lyft access token");
        }

        return accessToken;
    }

    @Override
    public List<ArrivalTime> getArrivalTimes(double latitude, double longitude) throws IOException {
        // prepare request
        UriBuilder uriBuilder = UriBuilder.fromUri(baseUrl + "v1/eta");
        uriBuilder.queryParam("lat", latitude);
        uriBuilder.queryParam("lng", longitude);
        String requestUrl = uriBuilder.toString();
        URL url = new URL(requestUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Authorization", "Bearer " + getAccessToken());
        connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");

        LOG.info("Made request to lyft API at following URL: " + requestUrl);

        // make request, parse repsonse
        InputStream responseStream = connection.getInputStream();
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        LyftArrivalEstimateResponse response = mapper.readValue(responseStream, LyftArrivalEstimateResponse.class);

        // serialize into Arrival Time objects
        ArrayList<ArrivalTime> arrivalTimes = new ArrayList<ArrivalTime>();

        LOG.info("Received " + response.eta_estimates.size() + " lyft arrival time estimates");

        for (final LyftArrivalEstimate time: response.eta_estimates) {
            arrivalTimes.add(
                new ArrivalTime(
                    time.ride_type,
                    time.display_name,
                    time.eta_seconds
                )
            );
        }

        return arrivalTimes;
    }

    @Override
    public EstimatedRideTime getEstimatedRideTime(
        String rideType,
        double startLatitude,
        double startLongitude,
        double endLatitude,
        double endLongitude
    ) throws IOException {
        // prepare request
        UriBuilder uriBuilder = UriBuilder.fromUri(baseUrl + "v1/cost");
        uriBuilder.queryParam("start_lat", startLatitude);
        uriBuilder.queryParam("start_lng", startLongitude);
        uriBuilder.queryParam("end_lat", endLatitude);
        uriBuilder.queryParam("end_lng", endLongitude);
        String requestUrl = uriBuilder.toString();
        URL url = new URL(requestUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Authorization", "Bearer " + getAccessToken());
        connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");

        LOG.info("Made request to lyft API at following URL: " + requestUrl);

        // make request, parse repsonse
        InputStream responseStream = connection.getInputStream();
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        LyftRideEstimateResponse response = mapper.readValue(responseStream, LyftRideEstimateResponse.class);

        LOG.info("Recieved " + response.cost_estimates.size() + " lyft price/time estimates");

        EstimatedRideTime rideTime = null;

        for (final LyftRideEstimate estimate: response.cost_estimates) {
            if (estimate.ride_type.equals(rideType)) {
                rideTime = new EstimatedRideTime(estimate.estimated_duration_seconds);
                break;
            }
        }

        return rideTime;
    }

    public void authenticate() throws IOException {
        getAccessToken();
    }
}
