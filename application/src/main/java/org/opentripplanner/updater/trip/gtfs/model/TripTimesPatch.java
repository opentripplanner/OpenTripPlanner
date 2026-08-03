package org.opentripplanner.updater.trip.gtfs.model;

import org.opentripplanner.transit.model.framework.DataValidationException;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimes;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimesBuilder;
import org.opentripplanner.updater.spi.DataValidationExceptionMapper;
import org.opentripplanner.updater.spi.UpdateException;

/**
 * The real-time changes computed from a GTFS-RT trip update, ready to be materialized into
 * {@link RealTimeTripTimes}. Besides the not-yet-built trip times it carries the
 * {@link StopPatternChanges}, which the caller uses to decide whether the trip must be moved onto a
 * modified trip pattern.
 */
public final class TripTimesPatch {

  private final RealTimeTripTimesBuilder builder;
  private final StopPatternChanges stopPatternChanges;

  public TripTimesPatch(RealTimeTripTimesBuilder builder, StopPatternChanges stopPatternChanges) {
    this.builder = builder;
    this.stopPatternChanges = stopPatternChanges;
  }

  public StopPatternChanges stopPatternChanges() {
    return stopPatternChanges;
  }

  /**
   * Flag the resulting trip times as running on a modified trip pattern, so the API reports the
   * trip as {@code MODIFIED}. The caller marks the patch once it has determined, from
   * {@link #stopPatternChanges()}, that the update moves the trip onto a real-time pattern that
   * differs from the planned one.
   */
  public TripTimesPatch withModifiedTripPattern() {
    builder.withModifiedTripPattern();
    return this;
  }

  /**
   * Materialize the updated trip times.
   */
  public RealTimeTripTimes tripTimes() throws UpdateException {
    try {
      return builder.build();
    } catch (DataValidationException e) {
      throw DataValidationExceptionMapper.map(e);
    }
  }
}
