package org.opentripplanner.routing.vehicle_parking;

import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class VehicleParkingGroupBuilder {

  FeedScopedId id;
  I18NString name;
  WgsCoordinate coordinate;

  VehicleParkingGroupBuilder(FeedScopedId id) {
    this.id = id;
  }

  /**
   * The name of this vehicle parking group, which may be translated when displaying to the user.
   */
  public VehicleParkingGroupBuilder withName(I18NString name) {
    this.name = name;
    return this;
  }

  /**
   * The coordinate of the vehicle parking group
   */
  public VehicleParkingGroupBuilder withCoordinate(WgsCoordinate coordinate) {
    this.coordinate = coordinate;
    return this;
  }

  public VehicleParkingGroup build() {
    return new VehicleParkingGroup(this);
  }
}
