package org.opentripplanner.service.vehiclerental.model;

import java.util.Map;
import javax.annotation.Nonnull;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.street.model.RentalFormFactor;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.rutebanken.netex.model.LocationStructure;

public class TestVehicleRentalStationBuilder {

  public static final String NETWORK_1 = "Network-1";
  public static final double DEFAULT_LATITUDE = 47.510;
  public static final double DEFAULT_LONGITUDE = 18.99;

  private double latitude = DEFAULT_LATITUDE;
  private double longitude = DEFAULT_LONGITUDE;
  private int vehicles = 10;
  private int spaces = 10;
  private boolean overloadingAllowed = false;
  private boolean stationOn = false;
  private RentalVehicleType vehicleType = RentalVehicleType.getDefaultType(NETWORK_1);

  public static TestVehicleRentalStationBuilder of() {
    return new TestVehicleRentalStationBuilder();
  }

  public TestVehicleRentalStationBuilder withLatitude(double latitude) {
    this.latitude = latitude;
    return this;
  }

  public TestVehicleRentalStationBuilder withLongitude(double longitude) {
    this.longitude = longitude;
    return this;
  }

  public TestVehicleRentalStationBuilder withVehicles(int vehicles) {
    this.vehicles = vehicles;
    return this;
  }

  public TestVehicleRentalStationBuilder withSpaces(int spaces) {
    this.spaces = spaces;
    return this;
  }

  public TestVehicleRentalStationBuilder withOverloadingAllowed(boolean overloadingAllowed) {
    this.overloadingAllowed = overloadingAllowed;
    return this;
  }

  public TestVehicleRentalStationBuilder withStationOn(boolean stationOn) {
    this.stationOn = stationOn;
    return this;
  }

  public TestVehicleRentalStationBuilder withVehicleTypeBicycle() {
    return buildVehicleType(RentalFormFactor.BICYCLE);
  }

  public TestVehicleRentalStationBuilder withVehicleTypeCar() {
    return buildVehicleType(RentalFormFactor.CAR);
  }

  @Nonnull
  private TestVehicleRentalStationBuilder buildVehicleType(RentalFormFactor rentalFormFactor) {
    this.vehicleType =
      new RentalVehicleType(
        new FeedScopedId(TestVehicleRentalStationBuilder.NETWORK_1, rentalFormFactor.name()),
        rentalFormFactor.name(),
        rentalFormFactor,
        RentalVehicleType.PropulsionType.ELECTRIC,
        100000d
      );
    return this;
  }

  public VehicleRentalStation build() {
    var station = new VehicleRentalStation();
    var stationName = "FooStation";
    station.id = new FeedScopedId(NETWORK_1, stationName);
    station.name = new NonLocalizedString(stationName);
    station.latitude = latitude;
    station.longitude = longitude;
    station.vehiclesAvailable = vehicles;
    station.spacesAvailable = spaces;
    station.vehicleTypesAvailable = Map.of(vehicleType, vehicles);
    station.vehicleSpacesAvailable = Map.of(vehicleType, spaces);
    station.overloadingAllowed = overloadingAllowed;
    station.isRenting = stationOn;
    station.isReturning = stationOn;
    station.realTimeData = true;
    return station;
  }
}
