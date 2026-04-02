package org.opentripplanner.routing.algorithm.raptoradapter.transit.request.transfercache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RaptorTransferIndex;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.Transfer;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RaptorRequestTransferCache {

  private static final Logger LOG = LoggerFactory.getLogger(RaptorRequestTransferCache.class);

  private final LoadingCache<RaptorRequestTransferCacheKey, RaptorTransferIndex> transferCache;

  public RaptorRequestTransferCache(int maximumSize) {
    transferCache = CacheBuilder.newBuilder().maximumSize(maximumSize).build(cacheLoader());
  }

  public LoadingCache<RaptorRequestTransferCacheKey, RaptorTransferIndex> getTransferCache() {
    return transferCache;
  }

  public void put(List<List<Transfer>> transfersByStopIndex, RouteRequest request) {
    final RaptorRequestTransferCacheKey cacheKey = new RaptorRequestTransferCacheKey(
      transfersByStopIndex,
      request
    );
    final RaptorTransferIndex raptorTransferIndex = RaptorTransferIndex.createInitialSetup(
      transfersByStopIndex,
      cacheKey.request()
    );

    LOG.info("Initializing cache with request: {}", cacheKey.options());
    transferCache.put(cacheKey, raptorTransferIndex);
  }

  public RaptorTransferIndex get(List<List<Transfer>> transfersByStopIndex, RouteRequest request) {
    try {
      return transferCache.get(new RaptorRequestTransferCacheKey(transfersByStopIndex, request));
    } catch (ExecutionException e) {
      throw new RuntimeException("Failed to get item from transfer cache", e);
    }
  }

  private CacheLoader<RaptorRequestTransferCacheKey, RaptorTransferIndex> cacheLoader() {
    return new CacheLoader<>() {
      @Override
      public RaptorTransferIndex load(RaptorRequestTransferCacheKey cacheKey) {
        LOG.info("Adding runtime request to cache: {}", cacheKey.options());
        return RaptorTransferIndex.createRequestScope(
          cacheKey.transfersByStopIndex(),
          cacheKey.request()
        );
      }
    };
  }
}
