package org.opentripplanner.ext.carhailing.service.uber;

import static jakarta.ws.rs.core.HttpHeaders.ACCEPT_LANGUAGE;
import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static java.util.Map.entry;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import org.opentripplanner.ext.carhailing.service.ArrivalTime;
import org.opentripplanner.ext.carhailing.service.CarHailingCompany;
import org.opentripplanner.ext.carhailing.service.CarHailingService;
import org.opentripplanner.ext.carhailing.service.RideEstimate;
import org.opentripplanner.ext.carhailing.service.RideEstimateRequest;
import org.opentripplanner.ext.carhailing.service.oauth.UrlEncodedOauthService;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.io.HttpUtils;
import org.opentripplanner.transit.model.basic.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UberService extends CarHailingService {

  private static final Logger LOG = LoggerFactory.getLogger(UberService.class);
  private static final String DEFAULT_BASE_URL = "https://api.uber.com/v1.2/";
  private static final String DEFAULT_AUTHENTICATION_URL = "https://login.uber.com/";
  private static final ObjectMapper mapper;

  private final String baseUrl;
  private final String authenticationUrl;
  private final UberServiceParameters config;
  private final UrlEncodedOauthService oauthService;

  static {
    mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  public UberService(UberServiceParameters config) {
    this.baseUrl = DEFAULT_BASE_URL;
    this.authenticationUrl = DEFAULT_AUTHENTICATION_URL;
    this.config = config;

    var uri = UriBuilder.fromUri(authenticationUrl + "oauth/v2/token").build();
    this.oauthService = new UrlEncodedOauthService(config.clientSecret(), config.clientId(), uri);
  }

  @Override
  public CarHailingCompany carHailingCompany() {
    return CarHailingCompany.UBER;
  }

  @Override
  public List<ArrivalTime> queryArrivalTimes(WgsCoordinate coord) throws IOException {
    var uri = UriBuilder
      .fromUri(baseUrl + "estimates/time")
      .queryParam("start_latitude", coord.latitude())
      .queryParam("start_longitude", coord.longitude())
      .build();

    var headers = Map.ofEntries(
      entry(AUTHORIZATION, "Bearer " + oauthService.getToken()),
      entry(ACCEPT_LANGUAGE, "en_US"),
      entry(CONTENT_TYPE, "application/json")
    );

    LOG.info("Made arrival time request to Uber API at following URL: {}", uri);

    // Make request, parse response
    InputStream responseStream = HttpUtils.openInputStream(uri, headers);
    UberArrivalEstimateResponse response = mapper.readValue(
      responseStream,
      UberArrivalEstimateResponse.class
    );

    LOG.debug("Received {} Uber arrival time estimates", response.times.size());

    var arrivalTimes = response.times
      .stream()
      .map(time ->
        new ArrivalTime(
          CarHailingCompany.UBER,
          time.product_id(),
          time.localized_display_name(),
          Duration.ofSeconds(time.estimate()),
          productIsWheelChairAccessible(time.product_id())
        )
      )
      .toList();

    if (arrivalTimes.isEmpty()) {
      LOG.warn("No Uber service available at {}", coord);
    }

    return arrivalTimes;
  }

  @Override
  public List<RideEstimate> queryRideEstimates(RideEstimateRequest request) throws IOException {
    // prepare request
    UriBuilder uriBuilder = UriBuilder.fromUri(baseUrl + "estimates/price");
    uriBuilder.queryParam("start_latitude", request.startPosition().latitude());
    uriBuilder.queryParam("start_longitude", request.startPosition().longitude());
    uriBuilder.queryParam("end_latitude", request.endPosition().latitude());
    uriBuilder.queryParam("end_longitude", request.endPosition().longitude());
    String requestUrl = uriBuilder.toString();
    URL uberUrl = new URL(requestUrl);
    HttpURLConnection connection = (HttpURLConnection) uberUrl.openConnection();
    connection.setRequestProperty("Authorization", "Bearer " + oauthService.getToken());
    connection.setRequestProperty("Accept-Language", "en_US");
    connection.setRequestProperty("Content-Type", "application/json");

    LOG.info("Made price estimate request to Uber API at following URL: {}", requestUrl);

    // Make request, parse response
    InputStream responseStream = connection.getInputStream();
    UberTripTimeEstimateResponse response = mapper.readValue(
      responseStream,
      UberTripTimeEstimateResponse.class
    );

    if (response.prices == null) {
      throw new IOException("Unexpected response format");
    }

    LOG.debug("Received {} Uber price estimates", response.prices.size());

    List<RideEstimate> estimates = new ArrayList<>();

    for (final UberTripTimeEstimate price : response.prices) {
      var currency = Currency.getInstance(price.currency_code);

      estimates.add(
        new RideEstimate(
          CarHailingCompany.UBER,
          Duration.ofSeconds(price.duration),
          new Money(currency, price.high_estimate),
          new Money(currency, price.low_estimate),
          price.product_id,
          productIsWheelChairAccessible(price.product_id)
        )
      );
    }

    if (estimates.isEmpty()) {
      LOG.warn(
        "No Uber service available for trip from {} to {}",
        request.startPosition(),
        request.endPosition()
      );
    }

    return estimates;
  }
}
