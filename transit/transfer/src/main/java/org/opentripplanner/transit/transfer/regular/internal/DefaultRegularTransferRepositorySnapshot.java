package org.opentripplanner.transit.transfer.regular.internal;

import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.data.transfers.regular.RaptorTransferStore;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepositorySnapshot;
import org.opentripplanner.transit.transfer.regular.api.RegularTransfersService;
import org.opentripplanner.transit.transfer.regular.api.preferences.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.spi.TransferRequestMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default {@link RegularTransferRepositorySnapshot}, created by
 * {@link RegularTransferRepositoryLifecycle#freeze}. The snapshot owns its
 * {@link OverlaidTemplatePaths}, which is never written to after creation. Hence, the paths are
 * effectively immutable and safe to read from any number of request threads. The cache of the
 * Raptor transfers derived from them is the only mutable state, and it is thread-safe.
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

  private final OverlaidTemplatePaths<P> paths;
  private final RegularTransferServiceCache<RaptorTransferStore> cache;
  private final CacheFactory cacheFactory;
  private final TransferRequestMapper<R> mapper;
  private final ProfileMatcher matcher;
  private final List<WarmupEntry> warmup;

  /**
   * The user preferences to create the Raptor transfers for when the server starts: the
   * preferences of a profile, or of a warmup entry.
   *
   * @param parent the parent profile of a warmup entry, {@code null} for a profile
   */
  record WarmupEntry(
    String id,
    @Nullable String parent,
    AbstractUserPreferences<?> userPreferences
  ) {}

  DefaultRegularTransferRepositorySnapshot(
    OverlaidTemplatePaths<P> paths,
    RegularTransferServiceCache<RaptorTransferStore> cache,
    RaptorTransfersFactory<P, R> raptorTransfersFactory,
    TransferRequestMapper<R> mapper,
    ProfileMatcher matcher,
    List<WarmupEntry> warmup
  ) {
    this.paths = paths;
    this.cache = cache;
    this.mapper = mapper;
    this.matcher = matcher;
    this.warmup = List.copyOf(warmup);
    this.cacheFactory = new CacheFactory(raptorTransfersFactory);
  }

  @Override
  public void warmUp() {
    for (var entry : warmup) {
      long start = System.currentTimeMillis();
      cache.get(entry.userPreferences(), cacheFactory);
      var time = System.currentTimeMillis() - start;
      if (entry.parent() == null) {
        LOG.info(
          "Warmed up the Routing Transfer Cache for the profile '{}' in {} ms.",
          entry.id(),
          time
        );
      } else {
        LOG.info(
          "Warmed up the Routing Transfer Cache for '{}' from the paths of '{}' in {} ms.",
          entry.id(),
          entry.parent(),
          time
        );
      }
    }
  }

  @Override
  public RegularTransfersService<P> forRequest(R request) {
    var userPreferences = mapper.toUserPreferences(request);
    return new DefaultRegularTransfersService<>(
      paths,
      cache,
      cacheFactory,
      userPreferences,
      matcher.closestProfile(paths, userPreferences)
    );
  }

  /** The paths of this snapshot, for the lifecycle. */
  OverlaidTemplatePaths<P> paths() {
    return paths;
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
        paths,
        userPreferences,
        matcher.closestProfile(paths, userPreferences)
      );
    }

    @Override
    public RaptorTransferStore patch(
      AbstractUserPreferences<?> userPreferences,
      RaptorTransferStore previous,
      Set<Integer> changedFromStops
    ) {
      return raptorTransfersFactory.patch(
        paths,
        userPreferences,
        matcher.closestProfile(paths, userPreferences),
        previous,
        changedFromStops
      );
    }
  }
}
