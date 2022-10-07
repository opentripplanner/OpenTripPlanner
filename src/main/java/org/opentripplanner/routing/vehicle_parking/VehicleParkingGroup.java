package org.opentripplanner.routing.vehicle_parking;

import java.util.Locale;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.basic.I18NString;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * Group of vehicle parking locations.
 * <p>
 * All fields are immutable. If any other properties change a new VehicleParkingGroup instance
 * should be created.
 */
public class VehicleParkingGroup {

  private final FeedScopedId id;

  private final I18NString name;

  private final double x, y;

  VehicleParkingGroup(VehicleParkingGroupBuilder vehicleParkingGroupBuilder) {
    this.id = vehicleParkingGroupBuilder.id;
    this.name = vehicleParkingGroupBuilder.name;
    this.x = vehicleParkingGroupBuilder.x;
    this.y = vehicleParkingGroupBuilder.y;
  }

  public static VehicleParkingGroupBuilder builder() {
    return new VehicleParkingGroupBuilder();
  }

  /**
   * The id of this vehicle parking group, prefixed by the source(=feedId) so that it is unique.
   */
  public FeedScopedId id() {
    return id;
  }

  /**
   * The name of this vehicle parking group, which may be translated when displaying to the user.
   */
  @Nullable
  public I18NString name() {
    return name;
  }

  /**
   * Longitude
   */
  public double x() {
    return x;
  }

  /**
   * Latitude
   */
  public double y() {
    return y;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final VehicleParkingGroup that = (VehicleParkingGroup) o;
    return (
      Double.compare(that.x, x) == 0 &&
      Double.compare(that.y, y) == 0 &&
      Objects.equals(id, that.id) &&
      Objects.equals(name, that.name)
    );
  }

  public String toString() {
    return String.format(Locale.ROOT, "VehicleParkingGroup(%s at %.6f, %.6f)", name, y, x);
  }
}
