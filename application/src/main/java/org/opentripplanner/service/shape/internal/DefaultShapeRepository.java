package org.opentripplanner.service.shape.internal;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.service.shape.ScheduledShapeData;
import org.opentripplanner.service.shape.ShapeRepository;
import org.opentripplanner.service.shape.ShapeRepositorySnapshot;
import org.opentripplanner.service.shape.model.CompactLineStringSequence;

/**
 * A single long-lived buffer, shared by all transactions and only accessed on the single writer
 * thread. Lookups check the real-time geometries first, then the scheduled ones.
 */
public class DefaultShapeRepository implements ShapeRepository {

  private final ScheduledShapeData scheduledShapeData;
  private final Map<FeedScopedId, CompactLineStringSequence> realTimeGeometriesByTripPattern;

  public DefaultShapeRepository(ScheduledShapeData scheduledShapeData) {
    this(scheduledShapeData, new HashMap<>());
  }

  private DefaultShapeRepository(
    ScheduledShapeData scheduledShapeData,
    Map<FeedScopedId, CompactLineStringSequence> realTimeGeometriesByTripPattern
  ) {
    this.scheduledShapeData = scheduledShapeData;
    this.realTimeGeometriesByTripPattern = realTimeGeometriesByTripPattern;
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
    realTimeGeometriesByTripPattern.put(tripPatternId, tripPatternGeometry);
  }

  @Override
  @Nullable
  public CompactLineStringSequence getTripPatternGeometry(FeedScopedId tripPatternId) {
    var tripPatternGeometry = realTimeGeometriesByTripPattern.get(tripPatternId);
    if (tripPatternGeometry != null) {
      return tripPatternGeometry;
    }
    return scheduledShapeData.getTripPatternGeometry(tripPatternId);
  }

  @Override
  public ShapeRepositorySnapshot createSnapshot() {
    return new DefaultShapeRepository(
      scheduledShapeData,
      Map.copyOf(realTimeGeometriesByTripPattern)
    );
  }
}
