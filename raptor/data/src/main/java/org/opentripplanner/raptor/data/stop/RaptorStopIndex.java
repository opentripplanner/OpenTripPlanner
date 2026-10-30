package org.opentripplanner.raptor.data.stop;

import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.raptor.spi.RaptorConstants;

/**
 * Maps {@code FeedScopedId <-> raptor stop index}. Stop ids are stable across serialization, so
 * they are used at the boundary; everything downstream of transfer generation is index-keyed.
 */
public interface RaptorStopIndex {
  /**
   * The raptor stop index of the stop, or {@link RaptorConstants#NOT_FOUND} if the id is not in
   * the index.
   */
  int toStopIndex(FeedScopedId stopId);

  /**
   * The id of the stop with the given raptor stop index, or {@code null} if no stop in the index
   * has it.
   */
  @Nullable
  FeedScopedId toStopId(int stopIndex);

  /**
   * The size of the index space: all valid stop indexes are in {@code [0, size())}. Some indexes
   * in the range may not be used.
   */
  int size();
}
