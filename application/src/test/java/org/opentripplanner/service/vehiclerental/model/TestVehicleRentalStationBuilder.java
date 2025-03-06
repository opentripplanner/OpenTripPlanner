package org.opentripplanner.service.vehiclerental.model;

import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.street.model.RentalFormFactor;
import org.opentripplanner.transit.model.framework.FeedScopedId;

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
  private VehicleRentalSystem system = null;
  private final Map<RentalVehicleType, Integer> vehicleTypesAvailable = new HashMap<>();
  private final Map<RentalVehicleType, Integer> vehicleSpacesAvailable = new HashMap<>();

  public static TestVehicleRentalStationBuilder of() {
    return new TestVehicleRentalStationBuilder();
  }

  public TestVehicleRentalStationBuilder withCoordinates(double latitude, double longitude) {
    this.latitude = latitude;
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

  public TestVehicleRentalStationBuilder withSystem(String id, String url) {
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

  public TestVehicleRentalStationBuilder withVehicleTypeBicycle(int numAvailable, int numSpaces) {
    return buildVehicleType(
      RentalFormFactor.BICYCLE,
      RentalVehicleType.PropulsionType.HUMAN,
      numAvailable,
      numSpaces
    );
  }

  public TestVehicleRentalStationBuilder withVehicleTypeElectricBicycle(
    int numAvailable,
    int numSpaces
  ) {
    return buildVehicleType(
      RentalFormFactor.BICYCLE,
      RentalVehicleType.PropulsionType.ELECTRIC,
      numAvailable,
      numSpaces
    );
  }

  public TestVehicleRentalStationBuilder withVehicleTypeCar(int numAvailable, int numSpaces) {
    return buildVehicleType(
      RentalFormFactor.CAR,
      RentalVehicleType.PropulsionType.ELECTRIC,
      numAvailable,
      numSpaces
    );
  }

  private TestVehicleRentalStationBuilder buildVehicleType(
    RentalFormFactor rentalFormFactor,
    RentalVehicleType.PropulsionType propulsionType,
    int numAvailable,
    int numSpaces
  ) {
    RentalVehicleType vehicleType = new RentalVehicleType(
      new FeedScopedId(
        TestVehicleRentalStationBuilder.NETWORK_1,
        String.format("%s-%s", rentalFormFactor.name(), propulsionType.name())
      ),
      rentalFormFactor.name(),
      rentalFormFactor,
      propulsionType,
      100000d
    );
    this.vehicleTypesAvailable.put(vehicleType, numAvailable);
    this.vehicleSpacesAvailable.put(vehicleType, numSpaces);
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

    // If no vehicle types are specified, use the default type
    if (vehicleTypesAvailable.isEmpty() || vehicleSpacesAvailable.isEmpty()) {
      station.vehicleTypesAvailable = Map.of(RentalVehicleType.getDefaultType(NETWORK_1), vehicles);
      station.vehicleSpacesAvailable = Map.of(RentalVehicleType.getDefaultType(NETWORK_1), spaces);
    } else {
      station.vehicleTypesAvailable = vehicleTypesAvailable;
      station.vehicleSpacesAvailable = vehicleSpacesAvailable;
    }

    station.overloadingAllowed = overloadingAllowed;
    station.isRenting = stationOn;
    station.isReturning = stationOn;
    station.realTimeData = true;
    station.system = system;
    return station;
  }
}
