package org.opentripplanner.ext.taxi.model;

import java.io.Serializable;
import java.util.Objects;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.timetable.booking.BookingInfo;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * A {@link Route} served by a taxi provider, with its service area geometry and booking
 * info. Equality is based on {@link #route} alone, assuming a one-to-one mapping between a
 * route and its {@code TaxiRoute} (enforced by the graph builder).
 */
public final class TaxiRoute implements Serializable {

  private final Route route;
  private final Geometry geometry;

  @Nullable
  private final BookingInfo pickupBookingInfo;

  @Nullable
  private final BookingInfo dropOffBookingInfo;

  public TaxiRoute(
    Route route,
    Geometry geometry,
    @Nullable BookingInfo pickupBookingInfo,
    @Nullable BookingInfo dropOffBookingInfo
  ) {
    this.route = Objects.requireNonNull(route);
    this.geometry = Objects.requireNonNull(geometry);
    this.pickupBookingInfo = pickupBookingInfo;
    this.dropOffBookingInfo = dropOffBookingInfo;
  }

  public Route route() {
    return route;
  }

  public Geometry geometry() {
    return geometry;
  }

  @Nullable
  public BookingInfo pickupBookingInfo() {
    return pickupBookingInfo;
  }

  @Nullable
  public BookingInfo dropOffBookingInfo() {
    return dropOffBookingInfo;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    TaxiRoute taxiRoute = (TaxiRoute) obj;
    return route.equals(taxiRoute.route);
  }

  @Override
  public int hashCode() {
    return route.hashCode();
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(TaxiRoute.class)
      .addObj("route", route)
      .addObj("geometry", geometry)
      .addObj("pickupBookingInfo", pickupBookingInfo)
      .addObj("dropOffBookingInfo", dropOffBookingInfo)
      .toString();
  }
}
