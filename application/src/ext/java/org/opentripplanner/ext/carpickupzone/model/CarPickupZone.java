package org.opentripplanner.ext.carpickupzone.model;

import java.io.Serializable;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.timetable.booking.BookingInfo;

public record CarPickupZone(
  Geometry geometry,
  Route route,
  @Nullable BookingInfo pickupBookingInfo,
  @Nullable BookingInfo dropOffBookingInfo
) implements Serializable {}
