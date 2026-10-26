package org.opentripplanner.transit.transfer.regular.internal;

import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.raptor.data.stop.RaptorStopIndex;
import org.opentripplanner.raptor.spi.RaptorConstants;

/** A {@link RaptorStopIndex} where the index of a stop is its position in the given list. */
final class TestStopIndex implements RaptorStopIndex {

  private final List<FeedScopedId> stopIds;

  private TestStopIndex(List<FeedScopedId> stopIds) {
    this.stopIds = stopIds;
  }

  static TestStopIndex of(FeedScopedId... stopIds) {
    return new TestStopIndex(List.of(stopIds));
  }

  static TestStopIndex of(List<FeedScopedId> stopIds) {
    return new TestStopIndex(List.copyOf(stopIds));
  }

  @Override
  public int toStopIndex(FeedScopedId stopId) {
    int index = stopIds.indexOf(stopId);
    return index < 0 ? RaptorConstants.NOT_FOUND : index;
  }

  @Nullable
  @Override
  public FeedScopedId toStopId(int stopIndex) {
    return stopIndex < 0 || stopIndex >= stopIds.size() ? null : stopIds.get(stopIndex);
  }

  @Override
  public int size() {
    return stopIds.size();
  }
}
