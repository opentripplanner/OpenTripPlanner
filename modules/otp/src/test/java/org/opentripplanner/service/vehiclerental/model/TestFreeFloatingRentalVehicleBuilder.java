package org.opentripplanner.service.vehiclerental.model;

import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.street.model.RentalFormFactor;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class TestFreeFloatingRentalVehicleBuilder {

  public static final String NETWORK_1 = "Network-1";
  public static final double DEFAULT_LATITUDE = 47.520;
  public static final double DEFAULT_LONGITUDE = 19.01;

  private double latitude = DEFAULT_LATITUDE;
  private double longitude = DEFAULT_LONGITUDE;
  private VehicleRentalSystem system = null;

  private RentalVehicleType vehicleType = RentalVehicleType.getDefaultType(NETWORK_1);

  public static TestFreeFloatingRentalVehicleBuilder of() {
    return new TestFreeFloatingRentalVehicleBuilder();
  }

  public TestFreeFloatingRentalVehicleBuilder withLatitude(double latitude) {
    this.latitude = latitude;
    return this;
  }

  public TestFreeFloatingRentalVehicleBuilder withLongitude(double longitude) {
    this.longitude = longitude;
    return this;
  }

  public TestFreeFloatingRentalVehicleBuilder withSystem(String id, String url) {
    this.system =
      new VehicleRentalSystem(
        id,
        null,
        null,
        null,
        null,
        url,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null
      );
    return this;
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
    vehicle.latitude = latitude;
    vehicle.longitude = longitude;
    vehicle.vehicleType = vehicleType;
    vehicle.system = system;
    return vehicle;
  }
}
