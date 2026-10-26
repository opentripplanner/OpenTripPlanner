package org.opentripplanner.transit.transfer.regular.internal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.opentripplanner.core.model.transaction.RepositoryLifecycle;
import org.opentripplanner.raptor.data.transfers.regular.RaptorTransferStore;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepository;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepositorySnapshot;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferParameters;
import org.opentripplanner.transit.transfer.regular.api.preferences.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.spi.TransferPathProvider;
import org.opentripplanner.transit.transfer.regular.spi.TransferRequestMapper;

/**
 * Copy-on-write / freeze lifecycle for the regular-transfer repository. Each transaction that
 * writes transfers gets a new repository continuing from the last published snapshot, see
 * {@link #copyOnWrite}, and {@link #freeze} publishes its paths in a new immutable snapshot. Every
 * snapshot is created by {@link #freeze}, including the initial one, from the repository created
 * by the build repository.
 * <p>
 * Neither direction copies the paths generated at graph build, only the transfers replaced at
 * runtime, see {@link OverlaidTemplatePaths}. The cache of the Raptor transfers derived from the
 * paths is carried forward from the snapshot a repository continues from: a cached value is only
 * updated for the stops whose transfers changed, see
 * {@link RegularTransferServiceCache#carryForward}.
 * <p>
 * <b>Rollback.</b> A repository that is not published is discarded with its changes, whether its
 * transaction failed before or after it was frozen. The next transaction gets a new repository
 * continuing from the last published snapshot.
 * <p>
 * This class is only accessed on the writer thread, except the statistics of the cache. They are
 * shared by all snapshots, and are thread-safe.
 *
 * @param <P> the street path type
 * @param <R> the request type
 */
public class RegularTransferRepositoryLifecycle<
  P,
  R
> implements
  RepositoryLifecycle<RegularTransferRepositorySnapshot<P, R>, RegularTransferRepository<P>> {

  private final RaptorTransfersFactory<P, R> raptorTransfersFactory;
  private final TransferRequestMapper<R> mapper;
  private final ProfileMatcher matcher;
  private final List<DefaultRegularTransferRepositorySnapshot.WarmupEntry> warmup;
  private final RegularTransferCacheStatistics statistics;

  /**
   * The statistics of the cache are not collected, see
   * {@link #RegularTransferRepositoryLifecycle(TransferPathProvider, TransferRequestMapper,
   * TransferParameters, int, boolean)}.
   */
  public RegularTransferRepositoryLifecycle(
    TransferPathProvider<P, R> pathProvider,
    TransferRequestMapper<R> mapper,
    TransferParameters<R> profiles,
    int numberOfStops
  ) {
    this(pathProvider, mapper, profiles, numberOfStops, false);
  }

  /**
   * @param profiles               the configuration of the profiles the paths are stored under, the
   *                               Raptor transfers are derived with the request of the profile
   *                               closest to the request
   * @param collectCacheStatistics whether to count the requests that use each key of the cache,
   *                               for the report of the cache, see
   *                               {@link RegularTransferRepositorySnapshot#cacheReport}
   */
  public RegularTransferRepositoryLifecycle(
    TransferPathProvider<P, R> pathProvider,
    TransferRequestMapper<R> mapper,
    TransferParameters<R> profiles,
    int numberOfStops,
    boolean collectCacheStatistics
  ) {
    this(
      pathProvider,
      mapper,
      requestsByProfile(profiles, mapper),
      new ProfileMatcher(ProfileUserPreferences.warmupParents(profiles, mapper)),
      warmupEntries(profiles, mapper),
      numberOfStops,
      collectCacheStatistics
    );
  }

  RegularTransferRepositoryLifecycle(
    TransferPathProvider<P, R> pathProvider,
    TransferRequestMapper<R> mapper,
    Map<AbstractUserPreferences<?>, R> profileRequests,
    int numberOfStops
  ) {
    this(
      pathProvider,
      mapper,
      profileRequests,
      ProfileMatcher.NO_WARMUP,
      List.of(),
      numberOfStops,
      false
    );
  }

  private RegularTransferRepositoryLifecycle(
    TransferPathProvider<P, R> pathProvider,
    TransferRequestMapper<R> mapper,
    Map<AbstractUserPreferences<?>, R> profileRequests,
    ProfileMatcher matcher,
    List<DefaultRegularTransferRepositorySnapshot.WarmupEntry> warmup,
    int numberOfStops,
    boolean collectCacheStatistics
  ) {
    this.matcher = matcher;
    this.warmup = warmup;
    this.statistics = collectCacheStatistics
      ? RegularTransferCacheStatistics.of(RegularTransferServiceCache.DEFAULT_MAX_SIZE)
      : RegularTransferCacheStatistics.NONE;
    this.mapper = Objects.requireNonNull(mapper);
    this.raptorTransfersFactory = new RaptorTransfersFactory<>(
      numberOfStops,
      Objects.requireNonNull(pathProvider),
      mapper,
      profileRequests
    );
  }

  private static <R> Map<AbstractUserPreferences<?>, R> requestsByProfile(
    TransferParameters<R> parameters,
    TransferRequestMapper<R> mapper
  ) {
    var result = new LinkedHashMap<AbstractUserPreferences<?>, R>();
    ProfileUserPreferences.resolveAll(parameters, mapper).forEach((preferences, profile) ->
      result.put(preferences, profile.routingRequest())
    );
    return result;
  }

  /**
   * The user preferences to create the Raptor transfers for at start: the preferences of every
   * profile, and then the ones of the warmup entries.
   */
  private static <R> List<DefaultRegularTransferRepositorySnapshot.WarmupEntry> warmupEntries(
    TransferParameters<R> parameters,
    TransferRequestMapper<R> mapper
  ) {
    var result = new ArrayList<DefaultRegularTransferRepositorySnapshot.WarmupEntry>();
    ProfileUserPreferences.resolveAll(parameters, mapper).forEach((preferences, profile) ->
      result.add(
        new DefaultRegularTransferRepositorySnapshot.WarmupEntry(profile.id(), null, preferences)
      )
    );
    for (var warmup : parameters.warmup()) {
      result.add(
        new DefaultRegularTransferRepositorySnapshot.WarmupEntry(
          warmup.id(),
          warmup.parent(),
          warmup.userPreferences()
        )
      );
    }
    return List.copyOf(result);
  }

  @Override
  public RegularTransferRepository<P> copyOnWrite(
    RegularTransferRepositorySnapshot<P, R> snapshot
  ) {
    // the cast is safe: all snapshots are created by freeze() below
    return DefaultRegularTransferRepository.continueFrom(
      (DefaultRegularTransferRepositorySnapshot<P, R>) Objects.requireNonNull(snapshot)
    );
  }

  @Override
  public RegularTransferRepositorySnapshot<P, R> freeze(RegularTransferRepository<P> repository) {
    // the cast is safe: all repositories are created by the build repository or copyOnWrite()
    var frozen = ((DefaultRegularTransferRepository<P>) repository).freeze();
    var cache =
      frozen.base() == null
        ? new RegularTransferServiceCache<RaptorTransferStore>()
        : frozen
            .base()
            .cache()
            .carryForward(userPreferences -> changedFromStops(frozen, userPreferences));
    return new DefaultRegularTransferRepositorySnapshot<>(
      frozen.paths(),
      cache,
      raptorTransfersFactory,
      mapper,
      matcher,
      warmup,
      statistics
    );
  }

  /**
   * The stops whose transfers changed under the profile the Raptor transfers for
   * {@code userPreferences} are derived from.
   */
  private Set<Integer> changedFromStops(
    DefaultRegularTransferRepository.Frozen<?> frozen,
    AbstractUserPreferences<?> userPreferences
  ) {
    var profile = matcher.closestProfile(frozen.paths(), userPreferences);
    if (profile == null) {
      return Set.of();
    }
    return frozen.changedFromStops().getOrDefault(profile, Set.of());
  }
}
