package org.opentripplanner.service.shape.internal;

import org.jspecify.annotations.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.service.shape.ScheduledShapeData;
import org.opentripplanner.service.shape.ShapeRepository;
import org.opentripplanner.service.shape.ShapeRepositorySnapshot;
import org.opentripplanner.service.shape.TripShapeValidator;
import org.opentripplanner.service.shape.model.CompactLineStringSequence;
import org.opentripplanner.transit.model.network.TripPattern;

/**
 * A single long-lived buffer, shared by all transactions and only accessed on the single writer
 * thread. Lookups check the real-time geometries first, then the scheduled ones.
 * <p>
 * Real-time geometries are grouped by feed in a {@link FeedPartitionedMap}, so a feed can be
 * cleared at once with {@link #clear(String)}, and a snapshot only copies the feeds changed since
 * the previous one.
 */
public class DefaultShapeRepository implements ShapeRepository {

  private final ScheduledShapeData scheduledShapeData;

  private final FeedPartitionedMap<
    FeedScopedId,
    CompactLineStringSequence
  > realTimeGeometriesByTripPattern;

  public DefaultShapeRepository(ScheduledShapeData scheduledShapeData) {
    this(scheduledShapeData, new FeedPartitionedMap<>());
  }

  private DefaultShapeRepository(
    ScheduledShapeData scheduledShapeData,
    FeedPartitionedMap<FeedScopedId, CompactLineStringSequence> realTimeGeometriesByTripPattern
  ) {
    this.scheduledShapeData = scheduledShapeData;
    this.realTimeGeometriesByTripPattern = realTimeGeometriesByTripPattern;
  }

  /**
   * A read-only snapshot of the scheduled data only, without any real-time data.
   */
  public static ShapeRepositorySnapshot scheduledOnly(ScheduledShapeData scheduledShapeData) {
    return new DefaultShapeRepository(scheduledShapeData, FeedPartitionedMap.empty());
  }

  @Override
  public void setTripPatternShape(
    TripPattern tripPattern,
    CompactLineStringSequence tripPatternShape
  ) {
    TripShapeValidator.validate(tripPattern, tripPatternShape);
    var tripPatternId = tripPattern.getId();
    realTimeGeometriesByTripPattern.put(tripPatternId.getFeedId(), tripPatternId, tripPatternShape);
  }

  @Override
  public void clear(String feedId) {
    realTimeGeometriesByTripPattern.clear(feedId);
  }

  @Override
  @Nullable
  public CompactLineStringSequence getTripPatternShape(FeedScopedId tripPatternId) {
    var realTimeShape = realTimeGeometriesByTripPattern.get(
      tripPatternId.getFeedId(),
      tripPatternId
    );
    if (realTimeShape != null) {
      return realTimeShape;
    }
    return scheduledShapeData.getTripPatternShape(tripPatternId);
  }

  @Override
  public ShapeRepositorySnapshot createSnapshot() {
    return new DefaultShapeRepository(scheduledShapeData, realTimeGeometriesByTripPattern.freeze());
  }
}
