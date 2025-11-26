package org.opentripplanner.service.vehiclerental.model;

import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.street.model.RentalFormFactor;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class RentalVehicleTypeFactory {

  public static RentalVehicleType vehicleType(RentalFormFactor formFactor) {
    return RentalVehicleType.of()
      .withId(new FeedScopedId("1", formFactor.name()))
      .withName(I18NString.of("bicycle"))
      .withFormFactor(formFactor)
      .withPropulsionType(RentalVehicleType.PropulsionType.HUMAN)
      .withMaxRangeMeters(1000d)
      .build();
  }
}
