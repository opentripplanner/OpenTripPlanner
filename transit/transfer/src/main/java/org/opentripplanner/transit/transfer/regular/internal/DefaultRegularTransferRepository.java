package org.opentripplanner.transit.transfer.regular.internal;

import java.util.Map;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepository;
import org.opentripplanner.transit.transfer.regular.api.AbstractUserPreferences;

/**
 * Default {@link RegularTransferRepository}. A new instance is created for each transaction that
 * writes transfers - initialized from the last committed snapshot - and is only accessed on the
 * single writer thread. Neither creating it nor {@link #freeze()} copies the whole path map, see
 * {@link TemplatePathsByUserProfile}.
 *
 * @param <P> the transfer path/template type
 */
class DefaultRegularTransferRepository<P> implements RegularTransferRepository<P> {

  private final TemplatePathsByUserProfile<P> paths;

  /** Create an empty repository. */
  DefaultRegularTransferRepository() {
    this(new TemplatePathsByUserProfile<>());
  }

  /** Create a repository initialized with the paths of a snapshot. */
  DefaultRegularTransferRepository(TemplatePathsByUserProfile<P> paths) {
    this.paths = paths.copy();
  }

  @Override
  public void setPath(
    AbstractUserPreferences<?> userPreferences,
    int fromStop,
    int toStop,
    P path
  ) {
    setPaths(userPreferences, fromStop, Map.of(toStop, path));
  }

  @Override
  public void setPaths(
    AbstractUserPreferences<?> userPreferences,
    int fromStop,
    Map<Integer, P> pathsByToStop
  ) {
    paths.setPaths(userPreferences, fromStop, pathsByToStop);
  }

  /**
   * Publish the current state as an immutable snapshot. Only the lifecycle should have access to
   * this, hence the package local access.
   */
  DefaultRegularTransferRepositorySnapshot<P> freeze() {
    return new DefaultRegularTransferRepositorySnapshot<>(paths.copy());
  }
}
