package org.opentripplanner.transit.transfer.regular;

import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.basic.CostLinearFunction;
import org.opentripplanner.core.model.id.FeedScopedId;

/**
 * The size of one profile after the regular transfers are generated.
 *
 * @param id                 the id of the profile
 * @param preferences        the user preferences of the profile, for the log
 * @param reuseLimit         the limit for when the path of the parent is used instead of a path
 *                           found by the profile, for the log
 * @param parentId           the id of the parent of the profile, {@code null} if it has none
 * @param paths              the number of paths of the profile, also the ones that use a path of
 *                           the parent
 * @param reused             the number of paths that are the path of the parent, so the path is
 *                           stored once for both profiles
 * @param stopsWithTransfers the stops that are the start or the end of at least one path of the
 *                           profile
 */
public record RegularTransferProfileStatistics(
  String id,
  String preferences,
  CostLinearFunction reuseLimit,
  @Nullable String parentId,
  long paths,
  long reused,
  Set<FeedScopedId> stopsWithTransfers
) {
  /** The paths that only this profile stores. */
  public long stored() {
    return paths - reused;
  }
}
