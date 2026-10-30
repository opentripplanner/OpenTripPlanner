package org.opentripplanner.transit.transfer.regular.internal;

import javax.annotation.Nullable;
import org.opentripplanner.raptor.data.transfers.regular.RaptorRegularTransferService;
import org.opentripplanner.raptor.data.transfers.regular.RaptorTransferStore;
import org.opentripplanner.transit.transfer.regular.api.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.api.RegularTransfers;

/**
 * Default {@link RegularTransfers}, created by
 * {@link DefaultRegularTransferRepositorySnapshot#forRequest}. It holds the user preferences of
 * the request and the profile closest to them, both found once, and reads the paths and the cache
 * of the Raptor transfers of the snapshot it is created from. The paths are immutable and the
 * cache is thread-safe, so the view is thread-safe.
 *
 * @param <P> the street path type
 */
final class DefaultRegularTransfers<P> implements RegularTransfers<P> {

  private final OverlaidTemplatePaths<P> paths;
  private final RegularTransferServiceCache<RaptorTransferStore> cache;
  private final RegularTransferServiceCache.Factory<RaptorTransferStore> cacheFactory;
  private final AbstractUserPreferences<?> userPreferences;

  @Nullable
  private final AbstractUserPreferences<?> profile;

  /**
   * @param userPreferences the user preferences of the request, the key of the cache
   * @param profile         the stored profile closest to {@code userPreferences}, the paths are
   *                        read from it. {@code null} if there are no stored paths.
   */
  DefaultRegularTransfers(
    OverlaidTemplatePaths<P> paths,
    RegularTransferServiceCache<RaptorTransferStore> cache,
    RegularTransferServiceCache.Factory<RaptorTransferStore> cacheFactory,
    AbstractUserPreferences<?> userPreferences,
    @Nullable AbstractUserPreferences<?> profile
  ) {
    this.paths = paths;
    this.cache = cache;
    this.cacheFactory = cacheFactory;
    this.userPreferences = userPreferences;
    this.profile = profile;
  }

  @Override
  public RaptorRegularTransferService raptorTransfers() {
    return cache.get(userPreferences, cacheFactory);
  }

  @Nullable
  @Override
  public P findPath(int fromStop, int toStop) {
    return paths.findPath(profile, fromStop, toStop);
  }
}
