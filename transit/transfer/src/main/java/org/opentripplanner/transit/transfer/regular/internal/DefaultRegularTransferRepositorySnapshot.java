package org.opentripplanner.transit.transfer.regular.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.opentripplanner.transit.transfer.regular.RaptorRegularTransferService;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepositorySnapshot;
import org.opentripplanner.transit.transfer.regular.api.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.parameters.TransferProfileType;

/**
 * Default {@link RegularTransferRepositorySnapshot}, created by
 * {@link DefaultRegularTransferRepository#freeze()}. The snapshot owns its profile and
 * {@code fromStop} maps; the {@code toStop} maps are shared with repositories, which never modify
 * them. Hence, the paths are effectively immutable and safe to read from any number of request
 * threads. The transfer-service cache is the only mutable state, and it is thread-safe.
 *
 * @param <P> the transfer path/template type
 */
class DefaultRegularTransferRepositorySnapshot<P> implements RegularTransferRepositorySnapshot<P> {

  private final Map<TransferProfileType, Map<Integer, Map<Integer, P>>> pathsByProfile;
  private final RegularTransferServiceCache transferServiceCache =
    new RegularTransferServiceCache();

  DefaultRegularTransferRepositorySnapshot(
    Map<TransferProfileType, Map<Integer, Map<Integer, P>>> pathsByProfile
  ) {
    this.pathsByProfile = pathsByProfile;
  }

  @Nullable
  @Override
  public P findPath(TransferProfileType profileType, int fromStop, int toStop) {
    var byFromStop = pathsByProfile.get(profileType);
    if (byFromStop == null) {
      return null;
    }
    var byToStop = byFromStop.get(fromStop);
    return byToStop == null ? null : byToStop.get(toStop);
  }

  @Override
  public List<StoredPath<P>> pathsFor(TransferProfileType profileType) {
    return listPaths(pathsByProfile, profileType);
  }

  /** Shared with {@link DefaultRegularTransferRepository#pathsFor}. */
  static <P> List<StoredPath<P>> listPaths(
    Map<TransferProfileType, Map<Integer, Map<Integer, P>>> pathsByProfile,
    TransferProfileType profileType
  ) {
    var byFromStop = pathsByProfile.get(profileType);
    if (byFromStop == null) {
      return List.of();
    }
    List<StoredPath<P>> result = new ArrayList<>();
    for (var fromEntry : byFromStop.entrySet()) {
      for (var toEntry : fromEntry.getValue().entrySet()) {
        result.add(new StoredPath<>(fromEntry.getKey(), toEntry.getKey(), toEntry.getValue()));
      }
    }
    return result;
  }

  @Override
  public RaptorRegularTransferService getOrCreateTransferService(
    TransferProfileType profileType,
    AbstractUserPreferences<?> preferences,
    Supplier<RaptorRegularTransferService> factory
  ) {
    return transferServiceCache.getOrCreate(profileType, preferences, factory);
  }

  /**
   * Create a new mutable repository initialized with the state of this snapshot. Only the
   * lifecycle should have access to this, hence the package local access.
   */
  DefaultRegularTransferRepository<P> copyOnWrite() {
    return new DefaultRegularTransferRepository<>(pathsByProfile);
  }
}
