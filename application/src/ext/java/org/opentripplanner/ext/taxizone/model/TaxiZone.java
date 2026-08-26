package org.opentripplanner.ext.taxizone.model;

import java.io.Serializable;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.core.model.time.LocalDateRange;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.booking.BookingInfo;

public record TaxiZone(
  Geometry geometry,
  Trip trip,
  @Nullable BookingInfo pickupBookingInfo,
  @Nullable BookingInfo dropOffBookingInfo,
  LocalDateRange serviceDateRange
) implements Serializable {}
