package org.opentripplanner.service.vehiclerental.model;

import javax.annotation.Nonnull;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.street.model.RentalFormFactor;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class TestFreeFloatingRentalVehicleBuilder {

  public static final String NETWORK_1 = "Network-1";

  private RentalVehicleType vehicleType = RentalVehicleType.getDefaultType(NETWORK_1);

  public static TestFreeFloatingRentalVehicleBuilder of() {
    return new TestFreeFloatingRentalVehicleBuilder();
  }

  public TestFreeFloatingRentalVehicleBuilder withVehicleScooter() {
    return buildVehicleType(RentalFormFactor.SCOOTER);
  }

  public TestFreeFloatingRentalVehicleBuilder withVehicleBicycle() {
    return buildVehicleType(RentalFormFactor.BICYCLE);
  }

  public TestFreeFloatingRentalVehicleBuilder withVehicleCar() {
    return buildVehicleType(RentalFormFactor.CAR);
  }

  @Nonnull
  private TestFreeFloatingRentalVehicleBuilder buildVehicleType(RentalFormFactor rentalFormFactor) {
    this.vehicleType =
      new RentalVehicleType(
        new FeedScopedId(TestFreeFloatingRentalVehicleBuilder.NETWORK_1, rentalFormFactor.name()),
        rentalFormFactor.name(),
        rentalFormFactor,
        RentalVehicleType.PropulsionType.ELECTRIC,
        100000d
      );
    return this;
  }

  public VehicleRentalVehicle build() {
    var vehicle = new VehicleRentalVehicle();
    var stationName = "free-floating-" + vehicleType.formFactor.name().toLowerCase();
    vehicle.id = new FeedScopedId(NETWORK_1, stationName);
    vehicle.name = new NonLocalizedString(stationName);
    vehicle.latitude = 47.510;
    vehicle.longitude = 18.99;
    vehicle.vehicleType = vehicleType;
    return vehicle;
  }
}
