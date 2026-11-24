package org.opentripplanner.transit.transfer.regular.internal;

import java.util.LinkedHashMap;
import java.util.Map;
import org.opentripplanner.transit.transfer.regular.FakeRequestMapper;
import org.opentripplanner.transit.transfer.regular.RegularTransferBuildRepository;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepositorySnapshot;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferParameters;
import org.opentripplanner.transit.transfer.regular.api.preferences.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.api.preferences.WalkPreferences;
import org.opentripplanner.transit.transfer.regular.spi.TransferPathProvider;
import org.opentripplanner.transit.transfer.regular.spi.TransferSearch;

/**
 * Lifecycles for tests that only read and write the stored paths, not the Raptor transfers
 * derived from them.
 */
final class TestLifecycles {

  private TestLifecycles() {}

  /**
   * A lifecycle whose snapshots cannot derive Raptor transfers.
   *
   * @param profiles the declared profiles, a request is matched to the closest of them
   */
  static <P> RegularTransferRepositoryLifecycle<P, WalkPreferences> pathsOnly(
    WalkPreferences... profiles
  ) {
    var profileRequests = new LinkedHashMap<AbstractUserPreferences<?>, WalkPreferences>();
    for (var profile : profiles) {
      profileRequests.put(profile, profile);
    }
    return pathsOnly(profileRequests);
  }

  /**
   * Create the runtime repository from the build repository, and return its initial snapshot,
   * which cannot derive Raptor transfers.
   *
   * @param profiles the declared profiles, a request is matched to the closest of them
   */
  static <P> RegularTransferRepositorySnapshot<P, WalkPreferences> initialSnapshot(
    RegularTransferBuildRepository<P> buildRepository,
    WalkPreferences... profiles
  ) {
    return TestLifecycles.<P>pathsOnly(profiles).freeze(buildRepository.createRuntimeRepository());
  }

  /**
   * Like {@link #initialSnapshot(RegularTransferBuildRepository, WalkPreferences...)}, with the
   * profiles of the transfer parameters the paths were generated with.
   */
  static <P> RegularTransferRepositorySnapshot<P, WalkPreferences> initialSnapshot(
    RegularTransferBuildRepository<P> buildRepository,
    TransferParameters<WalkPreferences> parameters
  ) {
    var profileRequests = new LinkedHashMap<AbstractUserPreferences<?>, WalkPreferences>();
    ProfileUserPreferences.resolveAll(parameters, FakeRequestMapper.INSTANCE).forEach(
      (preferences, profile) -> profileRequests.put(preferences, profile.routingRequest())
    );
    return TestLifecycles.<P>pathsOnly(profileRequests).freeze(
      buildRepository.createRuntimeRepository()
    );
  }

  private static <P> RegularTransferRepositoryLifecycle<P, WalkPreferences> pathsOnly(
    Map<AbstractUserPreferences<?>, WalkPreferences> profileRequests
  ) {
    return new RegularTransferRepositoryLifecycle<>(
      new UnsupportedPathProvider<>(),
      FakeRequestMapper.INSTANCE,
      profileRequests,
      0
    );
  }

  private static final class UnsupportedPathProvider<P> implements
    TransferPathProvider<P, WalkPreferences> {

    @Override
    public TransferSearch<P> searchFor(WalkPreferences request) {
      throw new UnsupportedOperationException();
    }
  }
}
