package org.opentripplanner.transit.transfer.regular;


import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.opentripplanner.raptor.data.stop.StopIndex;
import org.opentripplanner.raptor.data.transfers.regular.RaptorTransferStore;
import org.opentripplanner.raptor.spi.RaptorTransfer;
import org.opentripplanner.transit.transfer.regular.internal.RegularTransferRepository;
import org.opentripplanner.transit.transfer.regular.spi.PathCriteria;
import org.opentripplanner.transit.transfer.regular.spi.TransferPathProvider;

/**
 * Builds a {@link RaptorTransferStore} for a given {@code (profileId, preferences)} by re-costing the
 * path templates {@link RegularTransferRepository} generated once at graph-build time, under
 * request-time preferences. Results are cached, bounded LRU, keyed on
 * {@code (profileId, preferences)} - normalizing {@code U} so equivalent requests share a cache
 * entry is the caller's responsibility, not this factory's.
 *
 * @param <P> the transfer path/template type
 * @param <U> the user preferences type
 */
public class RaptorRegularTransferServiceFactory<P, U> {

  private static final int DEFAULT_MAX_CACHE_SIZE = 32;

  private final StopIndex stopIndex;
  private final RegularTransferRepository<P> repository;
  private final TransferPathProvider<P, U> pathProvider;
  private final Map<CacheKey<U>, RaptorRegularTransferService> cache;

  public RaptorRegularTransferServiceFactory(
    StopIndex stopIndex,
    RegularTransferRepository<P> repository,
    TransferPathProvider<P, U> pathProvider
  ) {
    this(stopIndex, repository, pathProvider, DEFAULT_MAX_CACHE_SIZE);
  }

  public RaptorRegularTransferServiceFactory(
    StopIndex stopIndex,
    RegularTransferRepository<P> repository,
    TransferPathProvider<P, U> pathProvider,
    int maxCacheSize
  ) {
    this.stopIndex = stopIndex;
    this.repository = repository;
    this.pathProvider = pathProvider;
    this.cache = new LinkedHashMap<>(16, 0.75f, true) {
      @Override
      protected boolean removeEldestEntry(Map.Entry<CacheKey<U>, RaptorRegularTransferService> eldest) {
        return size() > maxCacheSize;
      }
    };
  }

  /**
   * Coarse-grained lock: a cache miss rebuilds the whole {@link RaptorTransferStore} for that
   * {@code (profileId, preferences)}, which is CPU-bound, not I/O - acceptable for the expected
   * handful of distinct combinations, revisit if contention shows up under load.
   */
  public synchronized RaptorRegularTransferService create(
    RaptorTransferProfile profileId,
    U preferences
  ) {
    return cache.computeIfAbsent(
      new CacheKey<>(profileId, preferences),
      key -> build(profileId, preferences)
    );
  }

  /**
   * Recover the real street path template for a resolved transfer - used by itinerary mapping to
   * build a transfer leg's geometry/walk-steps, since a {@link RaptorTransferStore} lookup only
   * carries {@code (stop, duration, c1)}, not the path itself.
   */
  @Nullable
  public P findPath(RaptorTransferProfile profileId, int fromStop, int toStop) {
    return repository.findPath(profileId, fromStop, toStop);
  }

  private RaptorRegularTransferService build(RaptorTransferProfile profileId, U preferences) {
    var builder = RaptorTransferStore.of(stopIndex.size());
    for (var stored : repository.pathsFor(profileId)) {
      Optional<PathCriteria> criteria = pathProvider.computePathCriteria(
        stored.path(),
        profileId,
        preferences
      );
      if (criteria.isEmpty()) {
        continue;
      }
      builder.addTransfer(
        stored.fromStop(),
        stored.toStop(),
        criteria.get().durationInSeconds(),
        criteria.get().c1()
      );
    }
    return asRegularTransferService(builder.build());
  }

  /**
   * {@link RaptorTransferStore} lives in {@code raptor-data}, which this module depends on - it
   * cannot itself implement {@link RaptorRegularTransferService}, defined here. Both expose the
   * same two methods, so this just delegates.
   */
  private static RaptorRegularTransferService asRegularTransferService(RaptorTransferStore store) {
    return new RaptorRegularTransferService() {
      @Override
      public Iterator<? extends RaptorTransfer> getTransfersFromStop(int fromStop) {
        return store.getTransfersFromStop(fromStop);
      }

      @Override
      public Iterator<? extends RaptorTransfer> getTransfersToStop(int toStop) {
        return store.getTransfersToStop(toStop);
      }
    };
  }

  private record CacheKey<U>(RaptorTransferProfile profileId, U preferences) {}
}
