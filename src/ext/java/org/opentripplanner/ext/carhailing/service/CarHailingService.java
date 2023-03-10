package org.opentripplanner.ext.carhailing.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CarHailingService {

  private static final Logger LOG = LoggerFactory.getLogger(CarHailingService.class);

  // This value should be no longer than 30 minutes (according to Uber API docs) TODO check Lyft time limit
  private static final Duration CACHE_TIME = Duration.ofMinutes(2);

  private final Cache<WgsCoordinate, List<ArrivalTime>> arrivalTimeCache = CacheBuilder
    .newBuilder()
    .expireAfterWrite(CACHE_TIME)
    .build();
  private final Cache<RideEstimateRequest, List<RideEstimate>> rideEstimateCache = CacheBuilder
    .newBuilder()
    .expireAfterWrite(CACHE_TIME)
    .build();

  protected String wheelChairAccessibleRideType;

  private OAuthToken token = OAuthToken.empty();

  // Abstract method to return the TransportationNetworkCompany enum type
  public abstract CarHailingCompany carHailingCompany();

  // get the next arrivals for a specific location
  public List<ArrivalTime> getArrivalTimes(WgsCoordinate coordinate) throws ExecutionException {
    return arrivalTimeCache.get(coordinate, () -> queryArrivalTimes(coordinate));
  }

  protected abstract List<ArrivalTime> queryArrivalTimes(WgsCoordinate position) throws IOException;

  // get the estimated trip time for a specific rideType
  public List<RideEstimate> getRideEstimates(WgsCoordinate start, WgsCoordinate end)
    throws ExecutionException {
    // Truncate lat/lon values in order to reduce the number of API requests made.
    RideEstimateRequest request = new RideEstimateRequest(start, end);
    return rideEstimateCache.get(request, () -> queryRideEstimates(request));
  }

  protected abstract List<RideEstimate> queryRideEstimates(RideEstimateRequest request)
    throws IOException;

  protected boolean productIsWheelChairAccessible(String productId) {
    return productId.equals(wheelChairAccessibleRideType);
  }

  /**
   * Obtains and caches an OAuth API access token.
   * @return A token holder with the token value (including null token values if the call was unsuccessful).
   */
  public OAuthToken getToken() throws IOException {
    if (token.isExpired()) {
      // prepare request to get token
      HttpURLConnection connection = buildOAuthConnection();

      // send request and parse response (if a connection object was provided)
      if (connection != null) {
        String companyType = carHailingCompany().name();
        LOG.info("Requesting new {} access token", companyType);

        token = new OAuthToken(connection);

        connection.disconnect();
        LOG.info("Received new {} access token", companyType);
      }
    }

    return token;
  }

  /**
   * Prepares a connection object to obtain an OAuth token.
   * @return An {@link HttpURLConnection} object for obtaining the token.
   * @throws IOException Thrown if setting up the connection fails.
   */
  protected abstract HttpURLConnection buildOAuthConnection() throws IOException;
}
