package org.opentripplanner.service.vehicleparking.model;

import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.framework.geometry.WgsCoordinate;

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
