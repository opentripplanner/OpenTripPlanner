package org.opentripplanner.ext.taxizone.model;

import java.io.Serializable;
import java.util.Objects;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.core.model.time.LocalDateRange;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.timetable.booking.BookingInfo;
import org.opentripplanner.utils.tostring.ToStringBuilder;

public final class TaxiZone implements Serializable {

  private final Geometry geometry;
  private final Route route;

  @Nullable
  private final BookingInfo pickupBookingInfo;

  @Nullable
  private final BookingInfo dropOffBookingInfo;

  private final LocalDateRange serviceDateRange;

  public TaxiZone(
    Geometry geometry,
    Route route,
    @Nullable BookingInfo pickupBookingInfo,
    @Nullable BookingInfo dropOffBookingInfo,
    LocalDateRange serviceDateRange
  ) {
    this.geometry = geometry;
    this.route = route;
    this.pickupBookingInfo = pickupBookingInfo;
    this.dropOffBookingInfo = dropOffBookingInfo;
    this.serviceDateRange = serviceDateRange;
  }

  public Geometry geometry() {
    return geometry;
  }

  public Route route() {
    return route;
  }

  @Nullable
  public BookingInfo pickupBookingInfo() {
    return pickupBookingInfo;
  }

  @Nullable
  public BookingInfo dropOffBookingInfo() {
    return dropOffBookingInfo;
  }

  public LocalDateRange serviceDateRange() {
    return serviceDateRange;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    TaxiZone taxiZone = (TaxiZone) obj;
    return (
      Objects.equals(geometry, taxiZone.geometry) &&
      Objects.equals(route, taxiZone.route) &&
      Objects.equals(pickupBookingInfo, taxiZone.pickupBookingInfo) &&
      Objects.equals(dropOffBookingInfo, taxiZone.dropOffBookingInfo) &&
      Objects.equals(serviceDateRange, taxiZone.serviceDateRange)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      geometry,
      route,
      pickupBookingInfo,
      dropOffBookingInfo,
      serviceDateRange
    );
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(TaxiZone.class)
      .addObj("geometry", geometry)
      .addObj("route", route)
      .addObj("pickupBookingInfo", pickupBookingInfo)
      .addObj("dropOffBookingInfo", dropOffBookingInfo)
      .addObj("serviceDateRange", serviceDateRange)
      .toString();
  }
}
