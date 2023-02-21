package org.opentripplanner.ext.siri;

import java.time.LocalDate;
import javax.annotation.Nonnull;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;

record TripUpdate(
  @Nonnull Trip trip,
  @Nonnull StopPattern stopPattern,
  @Nonnull TripTimes tripTimes,
  @Nonnull LocalDate serviceDate
) {}
