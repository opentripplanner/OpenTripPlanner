package org.opentripplanner.service.vehiclerental.model;

import java.time.Instant;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class VehicleRentalStationBuilder {

  private final VehicleRentalStation original;
  private FeedScopedId id;
  private I18NString name;
  private I18NString shortName;
  private Double longitude;
  private Double latitude;
  private Integer capacity;
  private Map<RentalVehicleType, Integer> vehicleTypeAreaCapacity = Map.of();
  private Map<RentalVehicleType, Integer> vehicleTypeDockCapacity = Map.of();
  private VehicleRentalSystem system;
  private VehicleRentalStationUris rentalUris;
  private Integer vehiclesAvailable;
  private Integer vehiclesDisabled;
  private Map<RentalVehicleType, Integer> vehicleTypesAvailable = Map.of();
  private Integer spacesAvailable;
  private Integer spacesDisabled;
  private Map<RentalVehicleType, Integer> vehicleSpacesAvailable = Map.of();
  private Boolean isRenting;
  private Boolean isReturning;
  private Boolean overloadingAllowed;
  private Boolean isArrivingInRentalVehicleAtDestinationAllowed;
  private Boolean realTimeData;

  VehicleRentalStationBuilder(VehicleRentalStation original) {
    this.original = original;
    this.id = original.id();
    this.name = original.name();
    this.longitude = original.longitude();
    this.latitude = original.latitude();
    this.capacity = original.capacity();
    this.vehicleTypeAreaCapacity = original.vehicleTypeAreaCapacity();
    this.vehicleTypeDockCapacity = original.vehicleTypeDockCapacity();
    this.system = original.system();
    this.rentalUris = original.rentalUris();
    this.vehiclesAvailable = original.vehiclesAvailable();
    this.vehiclesDisabled = original.vehiclesDisabled();
    this.vehicleTypesAvailable = original.vehicleTypesAvailable();
    this.spacesAvailable = original.spacesAvailable();
    this.spacesDisabled = original.spacesDisabled();
    this.vehicleSpacesAvailable = original.vehicleSpacesAvailable();
    this.isRenting = original.isRenting();
    this.isReturning = original.isReturning();
    this.overloadingAllowed = original.overloadingAllowed();
    this.isArrivingInRentalVehicleAtDestinationAllowed =
      original.isArrivingInRentalVehicleAtDestinationAllowed();
    this.realTimeData = original.isRealTimeData();
  }

  public FeedScopedId id() {
    return id;
  }

  public I18NString name() {
    return name;
  }

  public I18NString shortName() {
    return shortName;
  }

  public double longitude() {
    return longitude;
  }

  public double latitude() {
    return latitude;
  }

  public Integer capacity() {
    return capacity;
  }

  public Map<RentalVehicleType, Integer> vehicleTypeAreaCapacity() {
    return vehicleTypeAreaCapacity;
  }

  public Map<RentalVehicleType, Integer> vehicleTypeDockCapacity() {
    return vehicleTypeDockCapacity;
  }

  public VehicleRentalSystem system() {
    return system;
  }

  public VehicleRentalStationUris rentalUris() {
    return rentalUris;
  }

  public int vehiclesAvailable() {
    return vehiclesAvailable;
  }

  public int vehiclesDisabled() {
    return vehiclesDisabled;
  }

  public Map<RentalVehicleType, Integer> vehicleTypesAvailable() {
    return vehicleTypesAvailable;
  }

  public int spacesAvailable() {
    return spacesAvailable;
  }

  public int spacesDisabled() {
    return spacesDisabled;
  }

  public Map<RentalVehicleType, Integer> vehicleSpacesAvailable() {
    return vehicleSpacesAvailable;
  }

  public boolean isRenting() {
    return isRenting;
  }

  public boolean isReturning() {
    return isReturning;
  }

  public boolean isOverloadingAllowed() {
    return overloadingAllowed;
  }

  public boolean isArrivingInRentalVehicleAtDestinationAllowed() {
    return isArrivingInRentalVehicleAtDestinationAllowed;
  }

  public boolean isRealTimeData() {
    return realTimeData;
  }

  public VehicleRentalStationBuilder withId(@Nullable FeedScopedId id) {
    this.id = id;
    return this;
  }

  public VehicleRentalStationBuilder withName(@Nullable I18NString name) {
    this.name = name;
    return this;
  }

  public VehicleRentalStationBuilder withShortName(@Nullable I18NString shortName) {
    this.shortName = shortName;
    return this;
  }

  public VehicleRentalStationBuilder withLongitude(double longitude) {
    this.longitude = longitude;
    return this;
  }

  public VehicleRentalStationBuilder withLatitude(double latitude) {
    this.latitude = latitude;
    return this;
  }

  public VehicleRentalStationBuilder withCapacity(@Nullable Integer capacity) {
    this.capacity = capacity;
    return this;
  }

  public VehicleRentalStationBuilder withVehicleTypeAreaCapacity(
    @Nullable Map<RentalVehicleType, Integer> vehicleTypeAreaCapacity
  ) {
    this.vehicleTypeAreaCapacity = vehicleTypeAreaCapacity;
    return this;
  }

  public VehicleRentalStationBuilder withVehicleTypeDockCapacity(
    @Nullable Map<RentalVehicleType, Integer> vehicleTypeDockCapacity
  ) {
    this.vehicleTypeDockCapacity = vehicleTypeDockCapacity;
    return this;
  }

  public VehicleRentalStationBuilder withSystem(@Nullable VehicleRentalSystem system) {
    this.system = system;
    return this;
  }

  public VehicleRentalStationBuilder withRentalUris(@Nullable VehicleRentalStationUris rentalUris) {
    this.rentalUris = rentalUris;
    return this;
  }

  public VehicleRentalStationBuilder withVehiclesAvailable(int vehiclesAvailable) {
    this.vehiclesAvailable = vehiclesAvailable;
    return this;
  }

  public VehicleRentalStationBuilder withVehiclesDisabled(int vehiclesDisabled) {
    this.vehiclesDisabled = vehiclesDisabled;
    return this;
  }

  public VehicleRentalStationBuilder withVehicleTypesAvailable(
    @Nullable Map<RentalVehicleType, Integer> vehicleTypesAvailable
  ) {
    this.vehicleTypesAvailable = vehicleTypesAvailable;
    return this;
  }

  public VehicleRentalStationBuilder withSpacesAvailable(int spacesAvailable) {
    this.spacesAvailable = spacesAvailable;
    return this;
  }

  public VehicleRentalStationBuilder withSpacesDisabled(int spacesDisabled) {
    this.spacesDisabled = spacesDisabled;
    return this;
  }

  public VehicleRentalStationBuilder withVehicleSpacesAvailable(
    @Nullable Map<RentalVehicleType, Integer> vehicleSpacesAvailable
  ) {
    this.vehicleSpacesAvailable = vehicleSpacesAvailable;
    return this;
  }

  public VehicleRentalStationBuilder withIsInstalled(boolean isInstalled) {
    return this;
  }

  public VehicleRentalStationBuilder withIsRenting(boolean isRenting) {
    this.isRenting = isRenting;
    return this;
  }

  public VehicleRentalStationBuilder withIsReturning(boolean isReturning) {
    this.isReturning = isReturning;
    return this;
  }

  public VehicleRentalStationBuilder withLastReported(@Nullable Instant lastReported) {
    return this;
  }

  public VehicleRentalStationBuilder withOverloadingAllowed(boolean overloadingAllowed) {
    this.overloadingAllowed = overloadingAllowed;
    return this;
  }

  public VehicleRentalStationBuilder withIsArrivingInRentalVehicleAtDestinationAllowed(
    boolean isArrivingInRentalVehicleAtDestinationAllowed
  ) {
    this.isArrivingInRentalVehicleAtDestinationAllowed =
      isArrivingInRentalVehicleAtDestinationAllowed;
    return this;
  }

  public VehicleRentalStationBuilder withRealTimeData(boolean realTimeData) {
    this.realTimeData = realTimeData;
    return this;
  }

  public VehicleRentalStationBuilder apply(Consumer<VehicleRentalStationBuilder> body) {
    body.accept(this);
    return this;
  }

  public VehicleRentalStation build() {
    var value = new VehicleRentalStation(this);
    return original.equals(value) ? original : value;
  }
}
