package org.opentripplanner.ext.siri;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.updater.TimetableSnapshotSourceParameters;
import org.opentripplanner.updater.spi.UpdateResult;
import org.opentripplanner.updater.trip.AbstractRealtimeTestEnvironment;
import uk.org.siri.siri20.EstimatedTimetableDeliveryStructure;

public class SiriRealtimeTestEnvironment extends AbstractRealtimeTestEnvironment {

  private static final TimetableSnapshotSourceParameters PARAMETERS = new TimetableSnapshotSourceParameters(
    Duration.ZERO,
    false
  );
  private final SiriTimetableSnapshotSource source;

  public SiriRealtimeTestEnvironment() {
    super();
    source = new SiriTimetableSnapshotSource(PARAMETERS, transitModel);
  }

  public EntityResolver getEntityResolver() {
    return new EntityResolver(getTransitService(), getFeedId());
  }

  public TripPattern getPatternForTrip(FeedScopedId tripId) {
    return getPatternForTrip(tripId, serviceDate);
  }

  public TripPattern getPatternForTrip(FeedScopedId tripId, LocalDate serviceDate) {
    var transitService = getTransitService();
    var trip = transitService.getTripOnServiceDateById(tripId);
    return transitService.getPatternForTrip(trip.getTrip(), serviceDate);
  }

  /**
   * Find the current TripTimes for a trip id on the default serviceDate
   */
  public TripTimes getTripTimesForTrip(Trip trip) {
    return getTripTimesForTrip(trip.getId(), serviceDate);
  }

  /**
   * Find the current TripTimes for a trip id on the default serviceDate
   */
  public TripTimes getTripTimesForTrip(String id) {
    return getTripTimesForTrip(id(id), serviceDate);
  }

  public UpdateResult applyEstimatedTimetable(List<EstimatedTimetableDeliveryStructure> updates) {
    return source.applyEstimatedTimetable(null, getEntityResolver(), getFeedId(), false, updates);
  }
}
