package org.opentripplanner.ext.carhailing.service.lyft;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import org.opentripplanner.ext.carhailing.CarHailingService;
import org.opentripplanner.ext.carhailing.model.ArrivalTime;
import org.opentripplanner.ext.carhailing.model.CarHailingProvider;
import org.opentripplanner.ext.carhailing.model.RideEstimate;
import org.opentripplanner.ext.carhailing.model.RideEstimateRequest;
import org.opentripplanner.ext.carhailing.service.CarHailingServiceParameters.LyftServiceParameters;
import org.opentripplanner.ext.carhailing.service.oauth.JsonPostAuthService;
import org.opentripplanner.ext.carhailing.service.oauth.OAuthService;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.transit.model.basic.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LyftService extends CarHailingService {

  private static final Logger LOG = LoggerFactory.getLogger(LyftService.class);
  private static final String DEFAULT_LYFT_API_URL = "https://api.lyft.com/";
  private static final ObjectMapper mapper;

  private final String baseUrl; // for testing purposes
  private final OAuthService oauthService;

  static {
    mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  public LyftService(LyftServiceParameters params) {
    this(DEFAULT_LYFT_API_URL, params);
  }

  // intended for use during testing
  public LyftService(String baseUrl, LyftServiceParameters params) {
    this.baseUrl = baseUrl;
    this.oauthService =
      new JsonPostAuthService(params.clientSecret(), params.clientSecret(), baseUrl);
    this.wheelchairAccessibleRideType = params.wheelchairAccessibleRideType();
  }

  @Override
  public CarHailingProvider carHailingCompany() {
    return CarHailingProvider.LYFT;
  }

  @Override
  public List<ArrivalTime> queryArrivalTimes(WgsCoordinate request) throws IOException {
    // prepare request
    UriBuilder uriBuilder = UriBuilder.fromUri(baseUrl + "v1/eta");
    uriBuilder.queryParam("lat", request.latitude());
    uriBuilder.queryParam("lng", request.longitude());
    String requestUrl = uriBuilder.toString();
    URL url = new URL(requestUrl);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestProperty("Authorization", "Bearer " + oauthService.getToken());
    connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");

    LOG.info("Made request to lyft API at following URL: " + requestUrl);

    // make request, parse response
    if (connection.getResponseCode() < HttpURLConnection.HTTP_BAD_REQUEST) {
      LyftArrivalEstimateResponse response = mapper.readValue(
        connection.getInputStream(),
        LyftArrivalEstimateResponse.class
      );

      // serialize into Arrival Time objects
      List<ArrivalTime> arrivalTimes = new ArrayList<>();

      LOG.info("Received " + response.eta_estimates.size() + " lyft arrival time estimates");

      for (final LyftArrivalEstimate time : response.eta_estimates) {
        arrivalTimes.add(
          new ArrivalTime(
            CarHailingProvider.LYFT,
            time.ride_type,
            time.display_name,
            Duration.ofSeconds(time.eta_seconds),
            productIsWheelchairAccessible(time.ride_type)
          )
        );
      }

      return arrivalTimes;
    } else {
      LyftError error = mapper.readValue(connection.getErrorStream(), LyftError.class);
      if (
        error.error() != null &&
        (
          error.error().equals("no_service_in_area") ||
          error.error().equals("ridetype_unavailable_in_region")
        )
      ) {
        LOG.warn(error.toString());
        LOG.warn("No Lyft service available at {}", request);
        return Collections.emptyList();
      }
      LOG.error(error.toString());
      if (error.error_description() != null) {
        throw new IOException(error.error_description());
      }
      throw new IOException("received an error from the Lyft API");
    }
  }

  @Override
  public List<RideEstimate> queryRideEstimates(RideEstimateRequest request) throws IOException {
    // prepare request
    UriBuilder uriBuilder = UriBuilder.fromUri(baseUrl + "v1/cost");
    uriBuilder.queryParam("start_lat", request.startPosition().latitude());
    uriBuilder.queryParam("start_lng", request.startPosition().longitude());
    uriBuilder.queryParam("end_lat", request.endPosition().latitude());
    uriBuilder.queryParam("end_lng", request.endPosition().longitude());
    String requestUrl = uriBuilder.toString();
    URL url = new URL(requestUrl);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestProperty(AUTHORIZATION, "Bearer " + oauthService.getToken());
    connection.setRequestProperty(CONTENT_TYPE, "application/json;charset=UTF-8");

    LOG.info("Made request to lyft API at following URL: " + requestUrl);

    // make request, parse response
    if (connection.getResponseCode() < HttpURLConnection.HTTP_BAD_REQUEST) {
      InputStream responseStream = connection.getInputStream();
      LyftRideEstimateResponse response = mapper.readValue(
        responseStream,
        LyftRideEstimateResponse.class
      );

      if (response.cost_estimates == null) {
        throw new IOException("Unrecognized response format");
      }

      LOG.info("Received " + response.cost_estimates.size() + " lyft price/time estimates");

      List<RideEstimate> estimates = new ArrayList<>();

      for (final LyftRideEstimate estimate : response.cost_estimates) {
        var currency = Currency.getInstance(estimate.currency);
        estimates.add(
          new RideEstimate(
            CarHailingProvider.LYFT,
            Duration.ofSeconds(estimate.estimated_duration_seconds),
            // Lyft's estimated cost is in the "minor" unit, so the following
            // may not work in countries that don't have 100 minor units per major unit
            // see https://en.wikipedia.org/wiki/ISO_4217#Treatment_of_minor_currency_units_(the_"exponent")
            new Money(currency, estimate.estimated_cost_cents_max),
            new Money(currency, estimate.estimated_cost_cents_min),
            estimate.ride_type,
            productIsWheelchairAccessible(estimate.ride_type)
          )
        );
      }

      return estimates;
    } else {
      LyftError error = mapper.readValue(connection.getErrorStream(), LyftError.class);
      if (
        error.error() != null &&
        (
          error.error().equals("no_service_in_area") ||
          error.error().equals("ridetype_unavailable_in_region")
        )
      ) {
        LOG.warn(error.toString());
        LOG.warn(
          "No Lyft service available for trip from {} to {}",
          request.startPosition(),
          request.endPosition()
        );
        return Collections.emptyList();
      }
      LOG.error(error.toString());
      if (error.error_description() != null) {
        throw new IOException(error.error_description());
      }
      throw new IOException("received an error from the Lyft API");
    }
  }
}
