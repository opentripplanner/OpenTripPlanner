package org.opentripplanner.routing.vehicle_parking;

import org.opentripplanner.transit.model.basic.I18NString;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class VehicleParkingGroupBuilder {

  FeedScopedId id;
  I18NString name;
  double x;
  double y;

  VehicleParkingGroupBuilder() {}

  /**
   * The id of this vehicle parking group, prefixed by the source(=feedId) so that it is unique.
   */
  public VehicleParkingGroupBuilder withId(FeedScopedId id) {
    this.id = id;
    return this;
  }

  /**
   * The name of this vehicle parking group, which may be translated when displaying to the user.
   */
  public VehicleParkingGroupBuilder withName(I18NString name) {
    this.name = name;
    return this;
  }

  /**
   * Longitude
   */
  public VehicleParkingGroupBuilder withX(double x) {
    this.x = x;
    return this;
  }

  /**
   * Latitude
   */
  public VehicleParkingGroupBuilder withY(double y) {
    this.y = y;
    return this;
  }

  public VehicleParkingGroup build() {
    return new VehicleParkingGroup(this);
  }
}
