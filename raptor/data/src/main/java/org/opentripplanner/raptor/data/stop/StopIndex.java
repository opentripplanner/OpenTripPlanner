package org.opentripplanner.raptor.data.stop;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import org.opentripplanner.core.model.id.FeedScopedId;

/**
 * Maps {@code FeedScopedId <-> raptor stop index}. Generation is naturally {@code FeedScopedId}-
 * keyed (stable across serialization); everything downstream of generation is index-keyed.
 */
public class StopIndex {

  private final FeedScopedId[] toStopIndex;
  private final TObjectIntMap<FeedScopedId> toStopId;

  public <T> StopIndex(
    int size,
    Iterable<T> index,
    ToIntFunction<T> getStopIndex,
    Function<T, FeedScopedId> getStopId
  ) {
    this.toStopIndex = new FeedScopedId[size];
    this.toStopId = new TObjectIntHashMap<>();

    for (T it : index) {
      FeedScopedId id = getStopId.apply(it);
      int stopIndex = getStopIndex.applyAsInt(it);
      this.toStopIndex[stopIndex] = id;
      this.toStopId.put(id, stopIndex);
    }
  }

  public FeedScopedId toStopId(int stopIndex) {
    return toStopIndex[stopIndex];
  }

  public int size() {
    return toStopIndex.length;
  }

  public int toStopIndex(FeedScopedId stopId) {
    return toStopId.get(stopId);
  }
}
