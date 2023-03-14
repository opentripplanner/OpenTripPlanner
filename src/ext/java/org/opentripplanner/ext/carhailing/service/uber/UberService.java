package org.opentripplanner.ext.carhailing.service.uber;

import static jakarta.ws.rs.core.HttpHeaders.ACCEPT_LANGUAGE;
import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static java.util.Map.entry;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import org.opentripplanner.ext.carhailing.CarHailingService;
import org.opentripplanner.ext.carhailing.model.ArrivalTime;
import org.opentripplanner.ext.carhailing.model.CarHailingProvider;
import org.opentripplanner.ext.carhailing.model.RideEstimate;
import org.opentripplanner.ext.carhailing.model.RideEstimateRequest;
import org.opentripplanner.ext.carhailing.service.CarHailingServiceParameters;
import org.opentripplanner.ext.carhailing.service.oauth.OAuthService;
import org.opentripplanner.ext.carhailing.service.oauth.UrlEncodedOAuthService;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.io.HttpUtils;
import org.opentripplanner.framework.json.ObjectMappers;
import org.opentripplanner.transit.model.basic.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UberService extends CarHailingService {

  private static final Logger LOG = LoggerFactory.getLogger(UberService.class);
  private static final String DEFAULT_BASE_URL = "https://api.uber.com/v1.2/";
  private static final String DEFAULT_AUTHENTICATION_URL = "https://login.uber.com/";
  private static final ObjectMapper MAPPER = ObjectMappers.ignoringExtraFields();

  private final String baseUrl;
  private final OAuthService oauthService;

  public UberService(CarHailingServiceParameters.UberServiceParameters config) {
    this.baseUrl = DEFAULT_BASE_URL;

    var authUrl = UriBuilder.fromUri(DEFAULT_AUTHENTICATION_URL + "oauth/v2/token").build();
    this.oauthService =
      new UrlEncodedOAuthService(
        config.clientSecret(),
        config.clientId(),
        "ride_request.estimate",
        authUrl
      );
  }

  @Override
  public CarHailingProvider carHailingCompany() {
    return CarHailingProvider.UBER;
  }

  @Override
  public List<ArrivalTime> queryArrivalTimes(WgsCoordinate coord) throws IOException {
    var uri = UriBuilder
      .fromUri(baseUrl + "estimates/time")
      .queryParam("start_latitude", coord.latitude())
      .queryParam("start_longitude", coord.longitude())
      .build();

    LOG.info("Made arrival time request to Uber API at following URL: {}", uri);

    InputStream responseStream = HttpUtils.openInputStream(uri, headers());
    var response = MAPPER.readValue(responseStream, UberArrivalEstimateResponse.class);

    LOG.debug("Received {} Uber arrival time estimates", response.times().size());

    var arrivalTimes = response
      .times()
      .stream()
      .map(time ->
        new ArrivalTime(
          CarHailingProvider.UBER,
          time.product_id(),
          time.localized_display_name(),
          Duration.ofSeconds(time.estimate()),
          productIsWheelchairAccessible(time.product_id())
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
    var uri = UriBuilder
      .fromUri(baseUrl + "estimates/price")
      .queryParam("start_latitude", request.startPosition().latitude())
      .queryParam("start_longitude", request.startPosition().longitude())
      .queryParam("end_latitude", request.endPosition().latitude())
      .queryParam("end_longitude", request.endPosition().longitude())
      .build();

    LOG.info("Made price estimate request to Uber API at following URL: {}", uri);

    InputStream responseStream = HttpUtils.openInputStream(uri, headers());
    var response = MAPPER.readValue(responseStream, UberTripTimeEstimateResponse.class);

    if (response.prices() == null) {
      throw new IOException("Unexpected response format");
    }

    LOG.debug("Received {} Uber price estimates", response.prices().size());

    var estimates = response
      .prices()
      .stream()
      .map(price -> {
        var currency = Currency.getInstance(price.currency_code);
        return new RideEstimate(
          CarHailingProvider.UBER,
          Duration.ofSeconds(price.duration),
          new Money(currency, price.high_estimate),
          new Money(currency, price.low_estimate),
          price.product_id,
          productIsWheelchairAccessible(price.product_id)
        );
      })
      .toList();

    if (estimates.isEmpty()) {
      LOG.warn(
        "No Uber service available for trip from {} to {}",
        request.startPosition(),
        request.endPosition()
      );
    }

    return estimates;
  }

  @Nonnull
  private Map<String, String> headers() throws IOException {
    return Map.ofEntries(
      entry(AUTHORIZATION, "Bearer %s".formatted(oauthService.getToken())),
      entry(ACCEPT_LANGUAGE, "en_US"),
      entry(CONTENT_TYPE, "application/json")
    );
  }
}
