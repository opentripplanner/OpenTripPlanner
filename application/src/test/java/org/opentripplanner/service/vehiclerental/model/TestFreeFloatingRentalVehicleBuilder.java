package org.opentripplanner.service.vehiclerental.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.street.model.RentalFormFactor;
import org.opentripplanner.transit.model.basic.Distance;
import org.opentripplanner.transit.model.basic.Ratio;

public class TestFreeFloatingRentalVehicleBuilder {

  public static final String NETWORK_1 = "Network-1";
  public static final double DEFAULT_LATITUDE = 47.520;
  public static final double DEFAULT_LONGITUDE = 19.01;
  public static final double DEFAULT_CURRENT_FUEL_PERCENT = 0.5;
  public static final double DEFAULT_CURRENT_RANGE_METERS = 5500.7;
  private static final Instant DEFAULT_AVAILABLE_UNTIL = OffsetDateTime.of(
    LocalDateTime.of(LocalDate.of(2025, 5, 14), LocalTime.MIN),
    ZoneOffset.UTC
  ).toInstant();

  private double latitude = DEFAULT_LATITUDE;
  private double longitude = DEFAULT_LONGITUDE;
  private Ratio currentFuelPercent = Ratio.of(DEFAULT_CURRENT_FUEL_PERCENT);
  private Double currentRangeMeters = DEFAULT_CURRENT_RANGE_METERS;
  private VehicleRentalSystem system = null;
  private String network = NETWORK_1;
  private Instant availableUntil = DEFAULT_AVAILABLE_UNTIL;

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
    this.system = new VehicleRentalSystem(id, null, null, null, url);
    return this;
  }

  public TestFreeFloatingRentalVehicleBuilder withAvailableUntil(Instant availableUntil) {
    this.availableUntil = availableUntil;
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
    var stationName = "free-floating-" + vehicleType.formFactor().name().toLowerCase();
    return VehicleRentalVehicle.of()
      .withId(new FeedScopedId(this.network, stationName))
      .withName(new NonLocalizedString(stationName))
      .withLatitude(latitude)
      .withLongitude(longitude)
      .withVehicleType(vehicleType)
      .withSystem(system)
      .withFuel(
        RentalVehicleFuel.of()
          .withPercent(currentFuelPercent)
          .withRange(Distance.ofMetersBoxed(currentRangeMeters, ignore -> {}).orElse(null))
          .build()
      )
      .withAvailableUntil(availableUntil)
      .build();
  }

  private TestFreeFloatingRentalVehicleBuilder buildVehicleType(RentalFormFactor rentalFormFactor) {
    this.vehicleType = RentalVehicleType.of()
      .withId(
        new FeedScopedId(TestFreeFloatingRentalVehicleBuilder.NETWORK_1, rentalFormFactor.name())
      )
      .withName(I18NString.of(rentalFormFactor.name()))
      .withFormFactor(rentalFormFactor)
      .withPropulsionType(RentalVehicleType.PropulsionType.ELECTRIC)
      .withMaxRangeMeters(100000d)
      .build();
    return this;
  }
}
