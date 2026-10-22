package org.opentripplanner.transit.transfer.regular.internal;

import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.transaction.RepositoryLifecycle;
import org.opentripplanner.raptor.data.stop.StopIndex;
import org.opentripplanner.raptor.data.transfers.regular.RaptorTransferStore;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepository;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepositorySnapshot;
import org.opentripplanner.transit.transfer.regular.api.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.spi.TransferPathProvider;

/**
 * Copy-on-write / freeze lifecycle for the regular-transfer repository. The repository is a single
 * long-lived buffer shared by all transactions: {@link #copyOnWrite} hands out the same buffer
 * instance, and {@link #freeze} publishes an immutable snapshot of it. Every snapshot is created
 * by {@link #freeze}, including the initial one.
 * <p>
 * Publishing a snapshot copies only the transfers replaced at runtime, never the paths generated
 * at graph build, see {@link OverlaidTemplatePaths}. The cache of the Raptor transfers derived
 * from the paths is carried forward from snapshot to snapshot: a cached value is only updated for
 * the stops whose transfers changed, see {@link RegularTransferServiceCache#carryForward}.
 * <p>
 * <b>Rollback.</b> A failed transaction is not frozen; the framework passes the last published
 * snapshot to the next {@link #copyOnWrite}. If the buffer has changes at that point, they belong
 * to the failed transaction, and are discarded.
 * <p>
 * This class is only accessed on the writer thread.
 *
 * @param <P> the transfer path/template type
 */
public class RegularTransferRepositoryLifecycle<P> implements
  RepositoryLifecycle<RegularTransferRepositorySnapshot<P>, RegularTransferRepository<P>> {

  private final DefaultRegularTransferRepository<P> buffer;
  private final RaptorTransfersFactory<P> raptorTransfersFactory;

  /**
   * The cache of the last published snapshot, the snapshot all requests started after the last
   * commit read. The cache of the next snapshot is carried forward from it.
   */
  @Nullable
  private RegularTransferServiceCache<RaptorTransferStore> publishedCache;

  /**
   * @param repository the buffer, created by
   *                   {@link org.opentripplanner.transit.transfer.regular.RegularTransferBuildRepository#createRuntimeRepository()}
   */
  public RegularTransferRepositoryLifecycle(
    RegularTransferRepository<P> repository,
    StopIndex stopIndex,
    TransferPathProvider<P, AbstractUserPreferences<?>> pathProvider
  ) {
    // the cast is safe: the build repository creates the only repository implementation
    this.buffer = (DefaultRegularTransferRepository<P>) Objects.requireNonNull(repository);
    this.raptorTransfersFactory = new RaptorTransfersFactory<>(
      stopIndex.size(),
      Objects.requireNonNull(pathProvider)
    );
  }

  @Override
  public RegularTransferRepository<P> copyOnWrite(RegularTransferRepositorySnapshot<P> snapshot) {
    if (buffer.hasChanges()) {
      // the cast is safe: all snapshots are created by freeze() below
      buffer.discardChanges(((DefaultRegularTransferRepositorySnapshot<P>) snapshot).paths());
    }
    return buffer;
  }

  @Override
  public RegularTransferRepositorySnapshot<P> freeze(RegularTransferRepository<P> repository) {
    if (repository != buffer) {
      throw new IllegalArgumentException("Only the buffer of this lifecycle can be frozen.");
    }
    var frozen = buffer.freeze();
    var cache =
      publishedCache == null
        ? new RegularTransferServiceCache<RaptorTransferStore>()
        : publishedCache.carryForward(userPreferences -> changedFromStops(frozen, userPreferences));
    publishedCache = cache;
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
