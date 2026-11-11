package org.opentripplanner.service.shape.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.service.shape.ScheduledShapeData;
import org.opentripplanner.service.shape.ShapeRepository;
import org.opentripplanner.service.shape.ShapeRepositorySnapshot;
import org.opentripplanner.service.shape.model.CompactLineStringSequence;

/**
 * A single long-lived buffer, shared by all transactions and only accessed on the single writer
 * thread. Lookups check the real-time geometries first, then the scheduled ones.
 * <p>
 * Real-time geometries are grouped by feed, so a feed can be cleared at once with
 * {@link #clear(String)}. A feed's map is copied on its first write after a snapshot (copy on
 * write), so a snapshot only costs as much as the number of feeds written since the previous one.
 */
public class DefaultShapeRepository implements ShapeRepository {

  private final ScheduledShapeData scheduledShapeData;

  /**
   * Real-time geometries by feed id, and then by trip pattern id.
   */
  private final Map<String, Map<FeedScopedId, CompactLineStringSequence>> realTimeGeometries;

  /**
   * The feeds whose inner map was copied since the latest snapshot, and can therefore be modified.
   */
  private final Set<String> copiedFeeds = new HashSet<>();

  public DefaultShapeRepository(ScheduledShapeData scheduledShapeData) {
    this(scheduledShapeData, new HashMap<>());
  }

  private DefaultShapeRepository(
    ScheduledShapeData scheduledShapeData,
    Map<String, Map<FeedScopedId, CompactLineStringSequence>> realTimeGeometries
  ) {
    this.scheduledShapeData = scheduledShapeData;
    this.realTimeGeometries = realTimeGeometries;
  }

  /**
   * A read-only snapshot of the scheduled data only, without any real-time data.
   */
  public static ShapeRepositorySnapshot scheduledOnly(ScheduledShapeData scheduledShapeData) {
    return new DefaultShapeRepository(scheduledShapeData, Map.of());
  }

  @Override
  public void setTripPatternGeometry(
    FeedScopedId tripPatternId,
    CompactLineStringSequence tripPatternGeometry
  ) {
    var feedId = tripPatternId.getFeedId();
    var tripPatternGeometriesForFeed = realTimeGeometries.get(feedId);
    if (copiedFeeds.add(feedId)) {
      if (tripPatternGeometriesForFeed == null) {
        tripPatternGeometriesForFeed = new HashMap<>();
      } else {
        tripPatternGeometriesForFeed = new HashMap<>(tripPatternGeometriesForFeed);
      }
      realTimeGeometries.put(feedId, tripPatternGeometriesForFeed);
    }
    tripPatternGeometriesForFeed.put(tripPatternId, tripPatternGeometry);
  }

  @Override
  public void clear(String feedId) {
    realTimeGeometries.remove(feedId);
    copiedFeeds.remove(feedId);
  }

  @Override
  @Nullable
  public CompactLineStringSequence getTripPatternGeometry(FeedScopedId tripPatternId) {
    var tripPatternGeometriesForFeed = realTimeGeometries.get(tripPatternId.getFeedId());
    if (tripPatternGeometriesForFeed != null) {
      var tripPatternGeometry = tripPatternGeometriesForFeed.get(tripPatternId);
      if (tripPatternGeometry != null) {
        return tripPatternGeometry;
      }
    }
    return scheduledShapeData.getTripPatternGeometry(tripPatternId);
  }

  @Override
  public ShapeRepositorySnapshot createSnapshot() {
    for (String feedId : copiedFeeds) {
      realTimeGeometries.put(feedId, Collections.unmodifiableMap(realTimeGeometries.get(feedId)));
    }
    copiedFeeds.clear();
    return new DefaultShapeRepository(scheduledShapeData, Map.copyOf(realTimeGeometries));
  }
}
