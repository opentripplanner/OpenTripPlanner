package org.opentripplanner.transit.transfer.regular.internal;

import java.util.Objects;
import java.util.Set;
import org.opentripplanner.core.model.transaction.RepositoryLifecycle;
import org.opentripplanner.raptor.data.transfers.regular.RaptorTransferStore;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepository;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepositorySnapshot;
import org.opentripplanner.transit.transfer.regular.api.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.spi.TransferPathProvider;

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
 */
public class RegularTransferRepositoryLifecycle<P> implements
  RepositoryLifecycle<RegularTransferRepositorySnapshot<P>, RegularTransferRepository<P>> {

  private final RaptorTransfersFactory<P> raptorTransfersFactory;

  public RegularTransferRepositoryLifecycle(
    TransferPathProvider<P> pathProvider,
    int numberOfStops
  ) {
    this.raptorTransfersFactory = new RaptorTransfersFactory<>(
      numberOfStops,
      Objects.requireNonNull(pathProvider)
    );
  }

  @Override
  public RegularTransferRepository<P> copyOnWrite(RegularTransferRepositorySnapshot<P> snapshot) {
    // the cast is safe: all snapshots are created by freeze() below
    return DefaultRegularTransferRepository.continueFrom(
      (DefaultRegularTransferRepositorySnapshot<P>) Objects.requireNonNull(snapshot)
    );
  }

  @Override
  public RegularTransferRepositorySnapshot<P> freeze(RegularTransferRepository<P> repository) {
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
      raptorTransfersFactory
    );
  }

  /**
   * The stops whose transfers changed under the profile the Raptor transfers for
   * {@code userPreferences} are derived from.
   */
  private static Set<Integer> changedFromStops(
    DefaultRegularTransferRepository.Frozen<?> frozen,
    AbstractUserPreferences<?> userPreferences
  ) {
    var profile = frozen.paths().closestProfile(userPreferences);
    if (profile == null) {
      return Set.of();
    }
    return frozen.changedFromStops().getOrDefault(profile, Set.of());
  }
}
