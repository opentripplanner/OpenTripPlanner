package org.opentripplanner.service.vehiclerental.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import javax.annotation.Nullable;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.street.model.RentalFormFactor;
import org.opentripplanner.transit.model.basic.Distance;
import org.opentripplanner.transit.model.basic.Ratio;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class TestFreeFloatingRentalVehicleBuilder {

  public static final String NETWORK_1 = "Network-1";
  public static final double DEFAULT_LATITUDE = 47.520;
  public static final double DEFAULT_LONGITUDE = 19.01;
  public static final double DEFAULT_CURRENT_FUEL_PERCENT = 0.5;
  public static final double DEFAULT_CURRENT_RANGE_METERS = 5500.7;

  private double latitude = DEFAULT_LATITUDE;
  private double longitude = DEFAULT_LONGITUDE;
  private Ratio currentFuelPercent = Ratio.of(DEFAULT_CURRENT_FUEL_PERCENT);
  private Double currentRangeMeters = DEFAULT_CURRENT_RANGE_METERS;
  private VehicleRentalSystem system = null;
  private String network = NETWORK_1;
  private static final OffsetDateTime DEFAULT_AVAILABLE_UNTIL = OffsetDateTime.of(
    LocalDateTime.of(LocalDate.of(2025, 1, 1), LocalTime.MIN),
    ZoneOffset.UTC
  );

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

  public TestFreeFloatingRentalVehicleBuilder withCurrentFuelPercent(
    @Nullable Double currentFuelPercent
  ) {
    if (currentFuelPercent == null) {
      this.currentFuelPercent = null;
    } else {
      this.currentFuelPercent = Ratio.ofBoxed(currentFuelPercent, ignore -> {}).orElse(null);
    }
    return this;
  }

  public TestFreeFloatingRentalVehicleBuilder withCurrentRangeMeters(Double currentRangeMeters) {
    this.currentRangeMeters = currentRangeMeters;
    return this;
  }

  public TestFreeFloatingRentalVehicleBuilder withNetwork(String network) {
    this.network = network;
    return this;
  }

  public TestFreeFloatingRentalVehicleBuilder withSystem(String id, String url) {
    this.system = new VehicleRentalSystem(
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

  public VehicleRentalVehicle build() {
    var vehicle = new VehicleRentalVehicle();
    var stationName = "free-floating-" + vehicleType.formFactor.name().toLowerCase();
    vehicle.id = new FeedScopedId(this.network, stationName);
    vehicle.name = new NonLocalizedString(stationName);
    vehicle.latitude = latitude;
    vehicle.longitude = longitude;
    vehicle.vehicleType = vehicleType;
    vehicle.system = system;
    vehicle.fuel = new RentalVehicleFuel(
      currentFuelPercent,
      Distance.ofMetersBoxed(currentRangeMeters, ignore -> {}).orElse(null)
    );
    vehicle.availableUntil = DEFAULT_AVAILABLE_UNTIL;
    return vehicle;
  }

  private TestFreeFloatingRentalVehicleBuilder buildVehicleType(RentalFormFactor rentalFormFactor) {
    this.vehicleType = new RentalVehicleType(
      new FeedScopedId(TestFreeFloatingRentalVehicleBuilder.NETWORK_1, rentalFormFactor.name()),
      rentalFormFactor.name(),
      rentalFormFactor,
      RentalVehicleType.PropulsionType.ELECTRIC,
      100000d
    );
    return this;
  }
}
