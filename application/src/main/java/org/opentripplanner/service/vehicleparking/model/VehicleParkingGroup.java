package org.opentripplanner.service.vehicleparking.model;

import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Group of vehicle parking locations.
 * <p>
 * All fields are immutable. If any other properties change a new VehicleParkingGroup instance
 * should be created.
 */
public class VehicleParkingGroup {

  private final FeedScopedId id;

  private final I18NString name;

  private final WgsCoordinate coordinate;

  VehicleParkingGroup(VehicleParkingGroupBuilder vehicleParkingGroupBuilder) {
    this.id = Objects.requireNonNull(vehicleParkingGroupBuilder.id);
    this.name = vehicleParkingGroupBuilder.name;
    this.coordinate = Objects.requireNonNull(vehicleParkingGroupBuilder.coordinate);
  }

  public static VehicleParkingGroupBuilder of(FeedScopedId id) {
    return new VehicleParkingGroupBuilder(id);
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
   * The coordinate of the vehicle parking group.
   */
  public WgsCoordinate coordinate() {
    return coordinate;
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
      Objects.equals(coordinate, that.coordinate) &&
      Objects.equals(id, that.id) &&
      Objects.equals(name, that.name)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, coordinate);
  }

  public String toString() {
    return ToStringBuilder.of(VehicleParkingGroup.class)
      .addStr("name", name.toString())
      .addObj("coordinate", coordinate)
      .toString();
  }
}
