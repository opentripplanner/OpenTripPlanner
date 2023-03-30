package org.opentripplanner.ext.ridehailing.service.uber;

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
import org.opentripplanner.ext.ridehailing.CachingRideHailingService;
import org.opentripplanner.ext.ridehailing.RideHailingServiceParameters;
import org.opentripplanner.ext.ridehailing.model.ArrivalTime;
import org.opentripplanner.ext.ridehailing.model.RideEstimate;
import org.opentripplanner.ext.ridehailing.model.RideEstimateRequest;
import org.opentripplanner.ext.ridehailing.model.RideHailingProvider;
import org.opentripplanner.ext.ridehailing.service.oauth.OAuthService;
import org.opentripplanner.ext.ridehailing.service.oauth.UrlEncodedOAuthService;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.io.HttpUtils;
import org.opentripplanner.framework.json.ObjectMappers;
import org.opentripplanner.transit.model.basic.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a ride hailing service for Uber.
 */
public class UberService extends CachingRideHailingService {

  private static final Logger LOG = LoggerFactory.getLogger(UberService.class);
  private static final String DEFAULT_BASE_URL = "https://api.uber.com/v1.2/";
  private static final String DEFAULT_TIME_ESTIMATE_URI = DEFAULT_BASE_URL + "estimates/time";
  private static final String DEFAULT_PRICE_ESTIMATE_URI = DEFAULT_BASE_URL + "estimates/price";
  private static final ObjectMapper MAPPER = ObjectMappers.ignoringExtraFields();

  private final OAuthService oauthService;
  private final String timeEstimateUri;
  private final String priceEstimateUri;

  public UberService(RideHailingServiceParameters.UberServiceParameters config) {
    this(
      new UrlEncodedOAuthService(
        config.clientSecret(),
        config.clientId(),
        "ride_request.estimate",
        UriBuilder.fromUri("https://login.uber.com/oauth/v2/token").build()
      ),
      DEFAULT_PRICE_ESTIMATE_URI,
      DEFAULT_TIME_ESTIMATE_URI
    );
  }

  UberService(OAuthService oauthService, String priceEstimateUri, String timeEstimateUri) {
    this.oauthService = oauthService;
    this.priceEstimateUri = priceEstimateUri;
    this.timeEstimateUri = timeEstimateUri;
  }

  @Override
  public RideHailingProvider provider() {
    return RideHailingProvider.UBER;
  }

  @Override
  public List<ArrivalTime> queryArrivalTimes(WgsCoordinate coord) throws IOException {
    var uri = UriBuilder.fromUri(timeEstimateUri).build();

    var finalUri = uri;
    if (uri.getScheme().equalsIgnoreCase("https")) {
      finalUri =
        UriBuilder
          .fromUri(uri)
          .queryParam("start_latitude", coord.latitude())
          .queryParam("start_longitude", coord.longitude())
          .build();
    }

    LOG.info("Made arrival time request to Uber API at following URL: {}", uri);

    InputStream responseStream = HttpUtils.openInputStream(finalUri, headers());
    var response = MAPPER.readValue(responseStream, UberArrivalEstimateResponse.class);

    LOG.debug("Received {} Uber arrival time estimates", response.times().size());

    var arrivalTimes = response
      .times()
      .stream()
      .map(time ->
        new ArrivalTime(
          RideHailingProvider.UBER,
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
    var uri = UriBuilder.fromUri(priceEstimateUri).build();

    var finalUri = uri;
    if (uri.getScheme().equalsIgnoreCase("https")) {
      finalUri =
        UriBuilder
          .fromUri(uri)
          .queryParam("start_latitude", request.startPosition().latitude())
          .queryParam("start_longitude", request.startPosition().longitude())
          .queryParam("end_latitude", request.endPosition().latitude())
          .queryParam("end_longitude", request.endPosition().longitude())
          .build();
    }

    LOG.info("Made price estimate request to Uber API at following URL: {}", uri);

    InputStream responseStream = HttpUtils.openInputStream(finalUri, headers());
    var response = MAPPER.readValue(responseStream, UberTripTimeEstimateResponse.class);

    if (response.prices() == null) {
      throw new IOException("Unexpected response format");
    }

    LOG.debug("Received {} Uber price estimates", response.prices().size());

    return response
      .prices()
      .stream()
      .map(price -> {
        var currency = Currency.getInstance(price.currency_code);
        return new RideEstimate(
          RideHailingProvider.UBER,
          Duration.ofSeconds(price.duration),
          new Money(currency, price.low_estimate * 100),
          new Money(currency, price.high_estimate * 100),
          price.product_id,
          price.display_name,
          productIsWheelchairAccessible(price.product_id)
        );
      })
      .toList();
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
