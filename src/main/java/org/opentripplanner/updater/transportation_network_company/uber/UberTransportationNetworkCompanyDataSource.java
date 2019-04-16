package org.opentripplanner.updater.transportation_network_company.uber;

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
import java.util.List;

public class UberTransportationNetworkCompanyDataSource extends TransportationNetworkCompanyDataSource {

    private static final Logger LOG = LoggerFactory.getLogger(UberTransportationNetworkCompanyDataSource.class);

    private static final String UBER_API_URL = "https://api.uber.com/v1.2/";

    private String serverToken;
    private String baseUrl;

    public UberTransportationNetworkCompanyDataSource(JsonNode config) {
        this.serverToken = config.path("serverToken").asText();
        this.baseUrl = UBER_API_URL;
        this.wheelChairAccessibleRideType = config.path("wheelChairAccessibleRideType").asText();
    }

    public UberTransportationNetworkCompanyDataSource (String serverToken, String baseUrl) {
        this.serverToken = serverToken;
        this.baseUrl = baseUrl;
    }

    @Override
    public TransportationNetworkCompany getTransportationNetworkCompanyType() {
        return TransportationNetworkCompany.UBER;
    }

    @Override
    public List<ArrivalTime> queryArrivalTimes(Position position) throws IOException {
        // prepare request
        UriBuilder uriBuilder = UriBuilder.fromUri(baseUrl + "estimates/time");
        uriBuilder.queryParam("start_latitude", position.latitude);
        uriBuilder.queryParam("start_longitude", position.longitude);
        String requestUrl = uriBuilder.toString();
        URL uberUrl = new URL(requestUrl);
        HttpURLConnection connection = (HttpURLConnection) uberUrl.openConnection();
        connection.setRequestProperty("Authorization", "Token " + serverToken);
        connection.setRequestProperty("Accept-Language", "en_US");
        connection.setRequestProperty("Content-Type", "application/json");

        LOG.info("Made arrival time request to Uber API at following URL: {}", requestUrl);

        // Make request, parse response
        InputStream responseStream = connection.getInputStream();
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        UberArrivalEstimateResponse response = mapper.readValue(responseStream, UberArrivalEstimateResponse.class);

        // serialize into Arrival Time objects
        List<ArrivalTime> arrivalTimes = new ArrayList<>();

        LOG.debug("Received {} Uber arrival time estimates", response.times.size());

        for (final UberArrivalEstimate time: response.times) {
            arrivalTimes.add(
                new ArrivalTime(
                    TransportationNetworkCompany.UBER,
                    time.product_id,
                    time.localized_display_name,
                    time.estimate,
                    productIsWheelChairAccessible(time.product_id)
                )
            );
        }

        if (arrivalTimes.size() == 0) {
            LOG.warn(
                "No Uber service available at {}, {}",
                position.latitude,
                position.longitude
            );
        }

        return arrivalTimes;
    }

    @Override
    public List<RideEstimate> queryRideEstimates(
        RideEstimateRequest request
    ) throws IOException {
        // prepare request
        UriBuilder uriBuilder = UriBuilder.fromUri(baseUrl + "estimates/price");
        uriBuilder.queryParam("start_latitude", request.startPosition.latitude);
        uriBuilder.queryParam("start_longitude", request.startPosition.longitude);
        uriBuilder.queryParam("end_latitude", request.endPosition.latitude);
        uriBuilder.queryParam("end_longitude", request.endPosition.longitude);
        String requestUrl = uriBuilder.toString();
        URL uberUrl = new URL(requestUrl);
        HttpURLConnection connection = (HttpURLConnection) uberUrl.openConnection();
        connection.setRequestProperty("Authorization", "Token " + serverToken);
        connection.setRequestProperty("Accept-Language", "en_US");
        connection.setRequestProperty("Content-Type", "application/json");

        LOG.info("Made price estimate request to Uber API at following URL: " + requestUrl);

        // Make request, parse response
        InputStream responseStream = connection.getInputStream();
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        UberTripTimeEstimateResponse response = mapper.readValue(responseStream, UberTripTimeEstimateResponse.class);

        if (response.prices == null) {
            throw new IOException("Unexpected response format");
        }

        LOG.debug("Received {} Uber price estimates", response.prices.size());

        List<RideEstimate> estimates = new ArrayList<>();

        for (final UberTripTimeEstimate price: response.prices) {
            estimates.add(new RideEstimate(
                TransportationNetworkCompany.UBER,
                price.currency_code,
                price.duration,
                price.high_estimate,
                price.low_estimate,
                price.product_id,
                productIsWheelChairAccessible(price.product_id)
            ));
        }

        if (estimates.size() == 0) {
            LOG.warn(
                "No Uber service available for trip from {}, {} to {}, {}",
                request.startPosition.latitude,
                request.startPosition.longitude,
                request.endPosition.latitude,
                request.endPosition.longitude
            );
        }

        return estimates;
    }
}