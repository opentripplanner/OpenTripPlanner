package org.opentripplanner.ext.ridehailing;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.opentripplanner.ext.ridehailing.model.ArrivalTime;
import org.opentripplanner.ext.ridehailing.model.RideEstimate;
import org.opentripplanner.ext.ridehailing.model.RideEstimateRequest;
import org.opentripplanner.framework.geometry.WgsCoordinate;

/**
 * A base class for caching API responses from ride hailing services.
 */
public abstract class CachingRideHailingService implements RideHailingService {

  // This value should be no longer than 30 minutes (according to Uber API docs) TODO check Lyft time limit
  private static final Duration CACHE_DURATION = Duration.ofMinutes(2);

  private final Cache<WgsCoordinate, List<ArrivalTime>> arrivalTimeCache = CacheBuilder.newBuilder()
    .expireAfterWrite(CACHE_DURATION)
    .build();
  private final Cache<RideEstimateRequest, List<RideEstimate>> rideEstimateCache =
    CacheBuilder.newBuilder().expireAfterWrite(CACHE_DURATION).build();

  /**
   * Get the next arrivals for a specific location.
   */
  @Override
  public List<ArrivalTime> arrivalTimes(WgsCoordinate coordinate, boolean wheelchairAccessible)
    throws ExecutionException {
    return arrivalTimeCache.get(coordinate.roundToApproximate10m(), () ->
      queryArrivalTimes(coordinate, wheelchairAccessible)
    );
  }

  protected abstract List<ArrivalTime> queryArrivalTimes(
    WgsCoordinate position,
    boolean wheelchair
  ) throws IOException;

  /**
   * Get the ride estimate for a specific start and end pair.
   */
  @Override
  public List<RideEstimate> rideEstimates(
    WgsCoordinate start,
    WgsCoordinate end,
    boolean wheelchairAccessible
  ) throws ExecutionException {
    // Truncate lat/lon values in order to reduce the number of API requests made.
    var request = new RideEstimateRequest(
      start.roundToApproximate10m(),
      end.roundToApproximate10m(),
      wheelchairAccessible
    );
    return rideEstimateCache.get(request, () -> queryRideEstimates(request));
  }

  protected abstract List<RideEstimate> queryRideEstimates(RideEstimateRequest request)
    throws IOException;
}
