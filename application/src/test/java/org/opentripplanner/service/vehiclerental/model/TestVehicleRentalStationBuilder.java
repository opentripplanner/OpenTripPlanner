package org.opentripplanner.service.vehiclerental.model;

import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.framework.i18n.I18NString;
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
    this.system = VehicleRentalSystem.of().withSystemId(id).withUrl(url).build();
    return this;
  }

  public TestVehicleRentalStationBuilder withVehicleTypeBicycle(int numAvailable, int numSpaces) {
    return withVehicleType(
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
    return withVehicleType(
      RentalFormFactor.BICYCLE,
      RentalVehicleType.PropulsionType.ELECTRIC,
      numAvailable,
      numSpaces
    );
  }

  public TestVehicleRentalStationBuilder withVehicleTypeCar(int numAvailable, int numSpaces) {
    return withVehicleType(
      RentalFormFactor.CAR,
      RentalVehicleType.PropulsionType.ELECTRIC,
      numAvailable,
      numSpaces
    );
  }

  public TestVehicleRentalStationBuilder withVehicleType(
    RentalFormFactor rentalFormFactor,
    RentalVehicleType.PropulsionType propulsionType,
    int numAvailable,
    int numSpaces
  ) {
    RentalVehicleType vehicleType = RentalVehicleType.of()
      .withId(
        new FeedScopedId(
          TestVehicleRentalStationBuilder.NETWORK_1,
          String.format("%s-%s", rentalFormFactor.name(), propulsionType.name())
        )
      )
      .withName(I18NString.of(rentalFormFactor.name()))
      .withFormFactor(rentalFormFactor)
      .withPropulsionType(propulsionType)
      .withMaxRangeMeters(100000d)
      .build();
    this.vehicleTypesAvailable.put(vehicleType, numAvailable);
    this.vehicleSpacesAvailable.put(vehicleType, numSpaces);
    return this;
  }

  public VehicleRentalStation build() {
    var stationName = "FooStation";

    // If no vehicle types are specified, use the default type
    Map<RentalVehicleType, Integer> typesAvailable;
    Map<RentalVehicleType, Integer> spacesAvailableMap;
    if (vehicleTypesAvailable.isEmpty() || vehicleSpacesAvailable.isEmpty()) {
      typesAvailable = Map.of(RentalVehicleType.getDefaultType(NETWORK_1), vehicles);
      spacesAvailableMap = Map.of(RentalVehicleType.getDefaultType(NETWORK_1), spaces);
    } else {
      typesAvailable = vehicleTypesAvailable;
      spacesAvailableMap = vehicleSpacesAvailable;
    }

    return VehicleRentalStation.of()
      .withId(new FeedScopedId(NETWORK_1, stationName))
      .withName(new NonLocalizedString(stationName))
      .withLatitude(latitude)
      .withLongitude(longitude)
      .withVehiclesAvailable(vehicles)
      .withSpacesAvailable(spaces)
      .withVehicleTypesAvailable(typesAvailable)
      .withVehicleSpacesAvailable(spacesAvailableMap)
      .withOverloadingAllowed(overloadingAllowed)
      .withIsRenting(stationOn)
      .withIsReturning(stationOn)
      .withRealTimeData(true)
      .withSystem(system)
      .build();
  }
}
