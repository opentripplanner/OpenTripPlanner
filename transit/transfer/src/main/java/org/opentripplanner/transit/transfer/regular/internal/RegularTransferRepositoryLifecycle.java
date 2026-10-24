package org.opentripplanner.transit.transfer.regular.internal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.opentripplanner.core.model.transaction.RepositoryLifecycle;
import org.opentripplanner.raptor.data.transfers.regular.RaptorTransferStore;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepository;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepositorySnapshot;
import org.opentripplanner.transit.transfer.regular.api.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.api.TransferProfilesParameters;
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
 * This class is stateless and only accessed on the writer thread.
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

  /**
   * @param profiles the configuration of the profiles the paths are stored under, the Raptor
   *                 transfers are derived with the request of the profile closest to the request
   */
  public RegularTransferRepositoryLifecycle(
    TransferPathProvider<P, R> pathProvider,
    TransferRequestMapper<R> mapper,
    TransferProfilesParameters<R> profiles,
    int numberOfStops
  ) {
    this(
      pathProvider,
      mapper,
      requestsByProfile(profiles, mapper),
      new ProfileMatcher(ProfileUserPreferences.warmupParents(profiles, mapper)),
      warmupEntries(profiles),
      numberOfStops
    );
  }

  RegularTransferRepositoryLifecycle(
    TransferPathProvider<P, R> pathProvider,
    TransferRequestMapper<R> mapper,
    Map<AbstractUserPreferences<?>, R> profileRequests,
    int numberOfStops
  ) {
    this(pathProvider, mapper, profileRequests, ProfileMatcher.NO_WARMUP, List.of(), numberOfStops);
  }

  private RegularTransferRepositoryLifecycle(
    TransferPathProvider<P, R> pathProvider,
    TransferRequestMapper<R> mapper,
    Map<AbstractUserPreferences<?>, R> profileRequests,
    ProfileMatcher matcher,
    List<DefaultRegularTransferRepositorySnapshot.WarmupEntry> warmup,
    int numberOfStops
  ) {
    this.matcher = matcher;
    this.warmup = warmup;
    this.mapper = Objects.requireNonNull(mapper);
    this.raptorTransfersFactory = new RaptorTransfersFactory<>(
      numberOfStops,
      Objects.requireNonNull(pathProvider),
      mapper,
      profileRequests
    );
  }

  private static <R> Map<AbstractUserPreferences<?>, R> requestsByProfile(
    TransferProfilesParameters<R> parameters,
    TransferRequestMapper<R> mapper
  ) {
    var result = new LinkedHashMap<AbstractUserPreferences<?>, R>();
    ProfileUserPreferences.resolveAll(parameters, mapper).forEach((preferences, profile) ->
      result.put(preferences, profile.routingRequest())
    );
    return result;
  }

  private static List<DefaultRegularTransferRepositorySnapshot.WarmupEntry> warmupEntries(
    TransferProfilesParameters<?> parameters
  ) {
    return parameters
      .warmup()
      .stream()
      .map(w ->
        new DefaultRegularTransferRepositorySnapshot.WarmupEntry(
          w.id(),
          w.parent(),
          w.userPreferences()
        )
      )
      .toList();
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
      warmup
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
