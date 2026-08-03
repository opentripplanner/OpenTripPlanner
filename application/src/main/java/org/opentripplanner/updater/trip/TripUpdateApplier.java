package org.opentripplanner.updater.trip;

import org.opentripplanner.transit.model.timetable.RealTimeTripUpdate;
import org.opentripplanner.transit.repository.TimetableRepository;
import org.opentripplanner.updater.spi.UpdateSuccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applies a {@link RealTimeTripUpdate} to a mutable timetable snapshot in three ordered phases:
 * <ol>
 *   <li>Revert any previous real-time modifications to the trip's pattern if requested.</li>
 *   <li>Mark the trip as deleted in its scheduled pattern when it is being moved to a modified
 *       pattern with a different stop sequence.</li>
 *   <li>Apply the main update.</li>
 * </ol>
 */
public class TripUpdateApplier {

  private static final Logger LOG = LoggerFactory.getLogger(TripUpdateApplier.class);

  private TripUpdateApplier() {}

  public static UpdateSuccess apply(TimetableRepository buffer, RealTimeTripUpdate update) {
    var trip = update.updatedTripTimes().getTrip();
    var serviceDate = update.serviceDate();

    // Phase 1: Revert previous real-time modifications if requested
    if (update.revertPreviousRealTimeUpdates()) {
      buffer.revertTripToScheduledTripPattern(trip.getId(), serviceDate);
    }

    // Phase 2: Mark trip as deleted in scheduled pattern if moving to a modified pattern
    var scheduledPattern = update.hideTripInScheduledPattern();
    if (scheduledPattern != null) {
      var scheduledTripTimes = scheduledPattern.getScheduledTimetable().getTripTimes(trip);
      if (scheduledTripTimes != null) {
        var builder = scheduledTripTimes.createRealTimeFromScheduledTimes();
        builder.withDeleted();
        buffer.update(
          RealTimeTripUpdate.of(scheduledPattern, builder.build(), serviceDate).build()
        );
      } else if (LOG.isDebugEnabled()) {
        LOG.debug(
          "Trip {} not found in scheduled pattern {}, skipping deletion.",
          trip.getId(),
          scheduledPattern.logName()
        );
      }
    }

    // Phase 3: Apply the main update
    buffer.update(update);
    return UpdateSuccess.noWarnings(update.producer());
  }
}
