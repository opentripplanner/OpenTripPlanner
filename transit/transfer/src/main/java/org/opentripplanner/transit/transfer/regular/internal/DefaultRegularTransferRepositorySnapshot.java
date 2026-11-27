package org.opentripplanner.transit.transfer.regular.internal;

import static org.opentripplanner.utils.time.StopWatchMs.startStopWatch;

import java.util.Set;
import org.opentripplanner.raptor.data.transfers.regular.RaptorTransferStore;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepositorySnapshot;
import org.opentripplanner.transit.transfer.regular.api.RegularTransfersService;
import org.opentripplanner.transit.transfer.regular.api.preferences.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.api.report.RegularTransferCacheMetrics;
import org.opentripplanner.transit.transfer.regular.api.report.RegularTransferCacheReport;
import org.opentripplanner.transit.transfer.regular.spi.TransferRequestMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default {@link RegularTransferRepositorySnapshot}, created by
 * {@link RegularTransferRepositoryLifecycle#freeze}. The snapshot owns its paths, which are never
 * written to after creation. Hence, the paths are effectively immutable and safe to read from any
 * number of request threads. The Raptor transfers and the paths of the requests are both read from
 * the {@link OpenTemplatePaths} of the snapshot, so they always agree on the path of a transfer.
 * The cache of the Raptor transfers is the only mutable state, and it is thread-safe.
 *
 * @param <P> the street path type
 * @param <R> the request type
 */
class DefaultRegularTransferRepositorySnapshot<
  P,
  R
> implements RegularTransferRepositorySnapshot<P, R> {

  private static final Logger LOG = LoggerFactory.getLogger(
    DefaultRegularTransferRepositorySnapshot.class
  );

  private final OpenTemplatePaths<P> openPaths;
  private final RegularTransferServiceCache<RaptorTransferStore> cache;
  private final CacheFactory cacheFactory;
  private final TransferRequestMapper<R> mapper;
  private final ProfileMatcher matcher;
  private final RegularTransferCacheReporter reporter;

  DefaultRegularTransferRepositorySnapshot(
    OpenTemplatePaths<P> openPaths,
    RegularTransferServiceCache<RaptorTransferStore> cache,
    RaptorTransfersFactory<P, R> raptorTransfersFactory,
    TransferRequestMapper<R> mapper,
    ProfileMatcher matcher,
    RegularTransferCacheReporter reporter
  ) {
    this.openPaths = openPaths;
    this.cache = cache;
    this.mapper = mapper;
    this.matcher = matcher;
    this.reporter = reporter;
    this.cacheFactory = new CacheFactory(raptorTransfersFactory);
  }

  @Override
  public void warmUp() {
    var totalStopWatch = startStopWatch();
    var warmup = reporter.warmup();
    // The cache is thread-safe, the entries are independent
    warmup.parallelStream().forEach(entry -> {
      var stopWatch = startStopWatch();
      cache.get(entry.userPreferences(), cacheFactory);
      LOG.info("Warm-up transfers: {} '{}' in {}", entry.source(), entry.id(), stopWatch.stop());
    });
    LOG.info("Warm-up transfers total: {} entries in {}", warmup.size(), totalStopWatch.stop());
  }

  @Override
  public RegularTransfersService<P> forRequest(R request) {
    var userPreferences = mapper.toUserPreferences(request);
    var profile = matcher.closestProfile(userPreferences);
    reporter.record(userPreferences);
    return new DefaultRegularTransfersService<>(
      openPaths,
      cache,
      cacheFactory,
      userPreferences,
      profile
    );
  }

  @Override
  public RegularTransferCacheReport cacheReport() {
    return reporter.report(cache, matcher);
  }

  @Override
  public RegularTransferCacheMetrics cacheMetrics() {
    return cache.metrics();
  }

  /** The paths of this snapshot and its closed facilities. */
  OpenTemplatePaths<P> openPaths() {
    return openPaths;
  }

  /** The cache of the Raptor transfers of this snapshot, for the lifecycle. */
  RegularTransferServiceCache<RaptorTransferStore> cache() {
    return cache;
  }

  /** Creates and patches the cached Raptor transfers from the paths of this snapshot. */
  private final class CacheFactory
    implements RegularTransferServiceCache.Factory<RaptorTransferStore>
  {

    private final RaptorTransfersFactory<P, R> raptorTransfersFactory;

    private CacheFactory(RaptorTransfersFactory<P, R> raptorTransfersFactory) {
      this.raptorTransfersFactory = raptorTransfersFactory;
    }

    @Override
    public RaptorTransferStore create(AbstractUserPreferences<?> userPreferences) {
      return raptorTransfersFactory.create(
        openPaths,
        userPreferences,
        matcher.closestProfile(userPreferences)
      );
    }

    @Override
    public RaptorTransferStore patch(
      AbstractUserPreferences<?> userPreferences,
      RaptorTransferStore previous,
      Set<Integer> changedFromStops
    ) {
      return raptorTransfersFactory.patch(
        openPaths,
        userPreferences,
        matcher.closestProfile(userPreferences),
        previous,
        changedFromStops
      );
    }
  }
}
