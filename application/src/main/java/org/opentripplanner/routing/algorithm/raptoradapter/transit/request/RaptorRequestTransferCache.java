package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RaptorTransferIndex;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.Transfer;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.preference.BikePreferences;
import org.opentripplanner.routing.api.request.preference.StreetPreferences;
import org.opentripplanner.routing.api.request.preference.WalkPreferences;
import org.opentripplanner.routing.api.request.preference.WheelchairPreferences;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.request.StreetSearchRequestMapper;
import org.opentripplanner.utils.tostring.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RaptorRequestTransferCache {

  private static final Logger LOG = LoggerFactory.getLogger(RaptorRequestTransferCache.class);

  private final LoadingCache<CacheKey, RaptorTransferIndex> transferCache;

  public RaptorRequestTransferCache(int maximumSize) {
    transferCache = CacheBuilder.newBuilder().maximumSize(maximumSize).build(cacheLoader());
  }

  public LoadingCache<CacheKey, RaptorTransferIndex> getTransferCache() {
    return transferCache;
  }

  public void put(List<List<Transfer>> transfersByStopIndex, RouteRequest request) {
    final CacheKey cacheKey = new CacheKey(transfersByStopIndex, request);
    final RaptorTransferIndex raptorTransferIndex = RaptorTransferIndex.createInitialSetup(
      transfersByStopIndex,
      cacheKey.request
    );

    LOG.info("Initializing cache with request: {}", cacheKey.options);
    transferCache.put(cacheKey, raptorTransferIndex);
  }

  public RaptorTransferIndex get(List<List<Transfer>> transfersByStopIndex, RouteRequest request) {
    try {
      return transferCache.get(new CacheKey(transfersByStopIndex, request));
    } catch (ExecutionException e) {
      throw new RuntimeException("Failed to get item from transfer cache", e);
    }
  }

  private CacheLoader<CacheKey, RaptorTransferIndex> cacheLoader() {
    return new CacheLoader<>() {
      @Override
      public RaptorTransferIndex load(CacheKey cacheKey) {
        LOG.info("Adding runtime request to cache: {}", cacheKey.options);
        return RaptorTransferIndex.createRequestScope(
          cacheKey.transfersByStopIndex,
          cacheKey.request
        );
      }
    };
  }

  private static class CacheKey {

    private final List<List<Transfer>> transfersByStopIndex;
    private final StreetSearchRequest request;
    private final StreetRelevantOptions options;

    private CacheKey(List<List<Transfer>> transfersByStopIndex, RouteRequest request) {
      this.transfersByStopIndex = transfersByStopIndex;
      this.request = StreetSearchRequestMapper.mapToTransferRequest(request).build();
      this.options = new StreetRelevantOptions(this.request);
    }

    @Override
    public int hashCode() {
      // transfersByStopIndex is ignored on purpose since it should not change (there is only
      // one instance per graph) and calculating the hashCode() would be expensive
      return options.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      CacheKey cacheKey = (CacheKey) o;
      // transfersByStopIndex is checked using == on purpose since the instance should not change
      // (there is only one instance per graph)
      return (
        transfersByStopIndex == cacheKey.transfersByStopIndex && options.equals(cacheKey.options)
      );
    }
  }

  /**
   * This contains an extract of the parameters which may influence transfers.
   * <p>
   */
  private static class StreetRelevantOptions {

    private final StreetMode transferMode;
    private final boolean wheelchair;
    private final WalkPreferences walk;
    private final BikePreferences bike;
    private final StreetPreferences street;
    private final WheelchairPreferences wheelchairPreferences;

    public StreetRelevantOptions(StreetSearchRequest request) {
      this.transferMode = request.mode();
      this.wheelchair = request.wheelchair();

      var preferences = request.preferences();
      this.walk = preferences.walk();
      this.bike = transferMode.includesBiking() ? preferences.bike() : BikePreferences.DEFAULT;
      this.street = preferences.street();
      this.wheelchairPreferences =
        this.wheelchair ? preferences.wheelchair() : WheelchairPreferences.DEFAULT;
    }

    @Override
    public String toString() {
      return ToStringBuilder
        .of(StreetRelevantOptions.class)
        .addEnum("transferMode", transferMode)
        .addBoolIfTrue("wheelchair", wheelchair)
        .addObj("walk", walk, WalkPreferences.DEFAULT)
        .addObj("bike", bike, BikePreferences.DEFAULT)
        .addObj("street", street, StreetPreferences.DEFAULT)
        .addObj("wheelchairPreferences", wheelchairPreferences, WheelchairPreferences.DEFAULT)
        .toString();
    }

    @Override
    public int hashCode() {
      return Objects.hash(transferMode, wheelchair, walk, bike, street, wheelchairPreferences);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof StreetRelevantOptions that)) {
        return false;
      }
      return (
        transferMode == that.transferMode &&
        wheelchair == that.wheelchair &&
        Objects.equals(that.walk, walk) &&
        Objects.equals(that.bike, bike) &&
        Objects.equals(that.street, street) &&
        Objects.equals(that.wheelchairPreferences, wheelchairPreferences)
      );
    }
  }
}
