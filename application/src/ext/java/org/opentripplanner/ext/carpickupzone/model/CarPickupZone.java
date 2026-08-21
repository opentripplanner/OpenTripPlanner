package org.opentripplanner.ext.carpickupzone.model;

import java.io.Serializable;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.booking.BookingInfo;

public record CarPickupZone(
  Geometry geometry,
  Trip trip,
  @Nullable BookingInfo pickupBookingInfo,
  @Nullable BookingInfo dropOffBookingInfo
) implements Serializable {}
