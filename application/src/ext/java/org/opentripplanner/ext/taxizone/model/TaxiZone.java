package org.opentripplanner.ext.taxizone.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Objects;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.geometry.WgsCoordinate;
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

  private transient PreparedGeometry preparedGeometry;

  public TaxiZone(
    Geometry geometry,
    Route route,
    @Nullable BookingInfo pickupBookingInfo,
    @Nullable BookingInfo dropOffBookingInfo
  ) {
    this.geometry = Objects.requireNonNull(geometry);
    this.route = Objects.requireNonNull(route);
    this.pickupBookingInfo = pickupBookingInfo;
    this.dropOffBookingInfo = dropOffBookingInfo;
    this.preparedGeometry = PreparedGeometryFactory.prepare(geometry);
  }

  public Geometry geometry() {
    return geometry;
  }

  /**
   * Returns {@code true} if this zone's geometry contains {@code coordinate}.
   * <p>
   * Uses the cached {@link PreparedGeometry}, since prepared geometries are faster for repeated
   * contains/intersects operations than plain {@link Geometry}.
   */
  public boolean contains(WgsCoordinate coordinate) {
    Point point = GeometryUtils.getGeometryFactory().createPoint(coordinate.asJtsCoordinate());
    return preparedGeometry.contains(point);
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
      Objects.equals(dropOffBookingInfo, taxiZone.dropOffBookingInfo)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(geometry, route, pickupBookingInfo, dropOffBookingInfo);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(TaxiZone.class)
      .addObj("geometry", geometry)
      .addObj("route", route)
      .addObj("pickupBookingInfo", pickupBookingInfo)
      .addObj("dropOffBookingInfo", dropOffBookingInfo)
      .toString();
  }

  /**
   * Rebuilds the transient {@link #preparedGeometry} cache after deserialization, since
   * {@link PreparedGeometry} is not {@link Serializable}.
   */
  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    this.preparedGeometry = PreparedGeometryFactory.prepare(geometry);
  }
}
