package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnull;
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

public class RaptorRequestTransferCache {

  private final LoadingCache<CacheKey, RaptorTransferIndex> transferCache;

  public RaptorRequestTransferCache(int maximumSize) {
    transferCache = CacheBuilder.newBuilder().maximumSize(maximumSize).build(cacheLoader());
  }

  public LoadingCache<CacheKey, RaptorTransferIndex> getTransferCache() {
    return transferCache;
  }

  public RaptorTransferIndex get(List<List<Transfer>> transfersByStopIndex, RouteRequest request) {
    try {
      return transferCache.get(
        new CacheKey(
          transfersByStopIndex,
          StreetSearchRequestMapper.mapToTransferRequest(request).build()
        )
      );
    } catch (ExecutionException e) {
      throw new RuntimeException("Failed to get item from transfer cache", e);
    }
  }

  private CacheLoader<CacheKey, RaptorTransferIndex> cacheLoader() {
    return new CacheLoader<>() {
      @Override
      @Nonnull
      public RaptorTransferIndex load(@Nonnull CacheKey cacheKey) {
        return RaptorTransferIndex.create(cacheKey.transfersByStopIndex, cacheKey.request);
      }
    };
  }

  private static class CacheKey {

    private final List<List<Transfer>> transfersByStopIndex;
    private final StreetSearchRequest request;
    private final StreetRelevantOptions options;

    private CacheKey(List<List<Transfer>> transfersByStopIndex, StreetSearchRequest request) {
      this.transfersByStopIndex = transfersByStopIndex;
      this.request = request;
      this.options = new StreetRelevantOptions(request);
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
