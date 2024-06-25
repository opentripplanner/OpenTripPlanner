package org.opentripplanner.ext.siri.updater;

import java.util.List;
import org.opentripplanner.ext.siri.EntityResolver;
import org.opentripplanner.ext.siri.SiriFuzzyTripMatcher;
import org.opentripplanner.ext.siri.SiriTimetableSnapshotSource;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.updater.spi.UpdateResult;
import org.opentripplanner.updater.trip.UpdateIncrementality;
import uk.org.siri.siri20.EstimatedTimetableDeliveryStructure;

/**
 * A consumer of estimated timetables that applies the real-time updates to the transit model.
 */
public class EstimatedTimetableHandler {

  private final SiriTimetableSnapshotSource snapshotSource;
  private final SiriFuzzyTripMatcher fuzzyTripMatcher;
  private final EntityResolver entityResolver;
  /**
   * The ID for the static feed to which these real time updates are applied
   */
  private final String feedId;

  public EstimatedTimetableHandler(
    SiriTimetableSnapshotSource snapshotSource,
    boolean fuzzyMatching,
    TransitService transitService,
    String feedId
  ) {
    this(
      snapshotSource,
      fuzzyMatching ? SiriFuzzyTripMatcher.of(transitService) : null,
      transitService,
      feedId
    );
  }

  /**
   * Constructor for tests only.
   */
  public EstimatedTimetableHandler(
    SiriTimetableSnapshotSource snapshotSource,
    SiriFuzzyTripMatcher siriFuzzyTripMatcher,
    TransitService transitService,
    String feedId
  ) {
    this.snapshotSource = snapshotSource;
    this.fuzzyTripMatcher = siriFuzzyTripMatcher;
    this.entityResolver = new EntityResolver(transitService, feedId);
    this.feedId = feedId;
  }

  /**
   * Apply the update to the transit model.
   */
  public UpdateResult applyUpdate(
    List<EstimatedTimetableDeliveryStructure> estimatedTimetableDeliveries,
    UpdateIncrementality updateMode
  ) {
    return snapshotSource.applyEstimatedTimetable(
      fuzzyTripMatcher,
      entityResolver,
      feedId,
      updateMode,
      estimatedTimetableDeliveries
    );
  }
}
