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
 * thread. Lookups check the real-time shapes first, then the scheduled ones.
 * <p>
 * Real-time shapes are grouped by feed in a {@link FeedPartitionedMap}, so a feed can be
 * cleared at once with {@link #clear(String)}, and a snapshot only copies the feeds changed since
 * the previous one.
 * <p>
 * See {@code package.md} for how the real-time shapes are kept for full dataset and
 * differential feeds.
 */
public class DefaultShapeRepository implements ShapeRepository {

  private final ScheduledShapeData scheduledShapeData;

  private final FeedPartitionedMap<
    FeedScopedId,
    CompactLineStringSequence
  > realTimeShapesByTripPattern;

  public DefaultShapeRepository(ScheduledShapeData scheduledShapeData) {
    this(scheduledShapeData, new FeedPartitionedMap<>());
  }

  private DefaultShapeRepository(
    ScheduledShapeData scheduledShapeData,
    FeedPartitionedMap<FeedScopedId, CompactLineStringSequence> realTimeShapesByTripPattern
  ) {
    this.scheduledShapeData = scheduledShapeData;
    this.realTimeShapesByTripPattern = realTimeShapesByTripPattern;
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
    realTimeShapesByTripPattern.put(tripPatternId.getFeedId(), tripPatternId, tripPatternShape);
  }

  @Override
  public void clear(String feedId) {
    realTimeShapesByTripPattern.clear(feedId);
  }

  @Override
  @Nullable
  public CompactLineStringSequence getTripPatternShape(FeedScopedId tripPatternId) {
    var realTimeShape = realTimeShapesByTripPattern.get(tripPatternId.getFeedId(), tripPatternId);
    if (realTimeShape != null) {
      return realTimeShape;
    }
    return scheduledShapeData.getTripPatternShape(tripPatternId);
  }

  @Override
  public ShapeRepositorySnapshot createSnapshot() {
    return new DefaultShapeRepository(scheduledShapeData, realTimeShapesByTripPattern.freeze());
  }
}
