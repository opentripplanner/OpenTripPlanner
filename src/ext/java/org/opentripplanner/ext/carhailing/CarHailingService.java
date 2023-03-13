package org.opentripplanner.ext.carhailing;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.opentripplanner.ext.carhailing.model.ArrivalTime;
import org.opentripplanner.ext.carhailing.model.CarHailingCompany;
import org.opentripplanner.ext.carhailing.model.RideEstimate;
import org.opentripplanner.ext.carhailing.service.RideEstimateRequest;
import org.opentripplanner.framework.geometry.WgsCoordinate;

public abstract class CarHailingService {

  // This value should be no longer than 30 minutes (according to Uber API docs) TODO check Lyft time limit
  private static final Duration CACHE_DURATION = Duration.ofMinutes(2);

  private final Cache<WgsCoordinate, List<ArrivalTime>> arrivalTimeCache = CacheBuilder
    .newBuilder()
    .expireAfterWrite(CACHE_DURATION)
    .build();
  private final Cache<RideEstimateRequest, List<RideEstimate>> rideEstimateCache = CacheBuilder
    .newBuilder()
    .expireAfterWrite(CACHE_DURATION)
    .build();

  protected String wheelChairAccessibleRideType;

  // Abstract method to return the TransportationNetworkCompany enum type
  public abstract CarHailingCompany carHailingCompany();

  // get the next arrivals for a specific location
  public List<ArrivalTime> arrivalTimes(WgsCoordinate coordinate) throws ExecutionException {
    return arrivalTimeCache.get(coordinate.rounded(), () -> queryArrivalTimes(coordinate));
  }

  protected abstract List<ArrivalTime> queryArrivalTimes(WgsCoordinate position) throws IOException;

  /**
   * Get the estimated trip time for a specific rideType
   */
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
}
