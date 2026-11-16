package org.opentripplanner.transit.transfer.regular;

import java.util.Set;
import org.opentripplanner.core.model.basic.CostLinearFunction;
import org.opentripplanner.core.model.id.FeedScopedId;

/**
 * The size of one profile after the regular transfers are generated.
 *
 * @param id                 the id of the profile
 * @param preferences        the user preferences of the profile, for the log
 * @param reuseLimit         the limit for when the path of an earlier profile is used instead of a
 *                           path found by the profile, for the log
 * @param paths              the number of paths of the profile, also the ones that use a path of
 *                           an earlier profile
 * @param reused             the number of paths that are the path of an earlier profile, so the
 *                           path is stored once for both profiles
 * @param stopsWithTransfers the stops that are the start or the end of at least one path of the
 *                           profile
 */
public record RegularTransferProfileStatistics(
  String id,
  String preferences,
  CostLinearFunction reuseLimit,
  long paths,
  long reused,
  Set<FeedScopedId> stopsWithTransfers
) {
  /** The paths that only this profile stores. */
  public long stored() {
    return paths - reused;
  }
}
