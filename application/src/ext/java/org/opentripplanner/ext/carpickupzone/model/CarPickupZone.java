package org.opentripplanner.ext.carpickupzone.model;

import java.io.Serializable;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.timetable.booking.BookingInfo;

/**
 * Zone data for a single car pickup provider, loaded from a dedicated Flex data source.
 */
public record CarPickupZone(
  Geometry geometry,
  Route route,
  @Nullable BookingInfo pickupBookingInfo,
  @Nullable BookingInfo dropOffBookingInfo
) implements Serializable {}
