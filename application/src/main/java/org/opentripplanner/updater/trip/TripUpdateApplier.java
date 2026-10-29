package org.opentripplanner.updater.trip;

import org.opentripplanner.transit.model.timetable.RealTimeTripUpdate;
import org.opentripplanner.transit.repository.TimetableRepository;
import org.opentripplanner.updater.spi.UpdateSuccess;
import org.opentripplanner.updater.trip.patterncache.RealTimeShapeResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applies a {@link RealTimeTripUpdate} to a mutable timetable snapshot in four ordered phases:
 * <ol>
 *   <li>Revert any previous real-time modifications to the trip's pattern if requested.</li>
 *   <li>Mark the trip as deleted in its scheduled pattern when it is being moved to a modified
 *       pattern with a different stop sequence.</li>
 *   <li>Apply the main update.</li>
 *   <li>Ensure that the pattern of the update has a geometry. This is done last, so that no shape
 *       is written for an update that could not be applied.</li>
 * </ol>
 */
public class TripUpdateApplier {

  private static final Logger LOG = LoggerFactory.getLogger(TripUpdateApplier.class);

  private TripUpdateApplier() {}

  public static UpdateSuccess apply(
    TimetableRepository buffer,
    RealTimeShapeResolver shapeResolver,
    RealTimeTripUpdate update
  ) {
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

    // Phase 4: Make sure the pattern has a geometry
    shapeResolver.ensureGeometry(update.pattern());
    return UpdateSuccess.of(update.producer());
  }
}
