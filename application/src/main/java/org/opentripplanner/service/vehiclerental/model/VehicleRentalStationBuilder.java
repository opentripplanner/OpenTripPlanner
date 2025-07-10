package org.opentripplanner.service.vehiclerental.model;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class VehicleRentalStationBuilder {

  private final VehicleRentalStation original;
  private FeedScopedId id;
  private I18NString name;
  private String shortName;
  private Double longitude;
  private Double latitude;
  private String address;
  private String crossStreet;
  private String regionId;
  private String postCode;
  private Set<String> rentalMethods = Set.of();
  private Boolean isVirtualStation;
  private Geometry stationArea;
  private Integer capacity;
  private Map<RentalVehicleType, Integer> vehicleTypeAreaCapacity = Map.of();
  private Map<RentalVehicleType, Integer> vehicleTypeDockCapacity = Map.of();
  private Boolean isValetStation;
  private VehicleRentalSystem system;
  private VehicleRentalStationUris rentalUris;
  private Integer vehiclesAvailable;
  private Integer vehiclesDisabled;
  private Map<RentalVehicleType, Integer> vehicleTypesAvailable = Map.of();
  private Integer spacesAvailable;
  private Integer spacesDisabled;
  private Map<RentalVehicleType, Integer> vehicleSpacesAvailable = Map.of();
  private Boolean isInstalled;
  private Boolean isRenting;
  private Boolean isReturning;
  private Instant lastReported;
  private Boolean overloadingAllowed;
  private Boolean isArrivingInRentalVehicleAtDestinationAllowed;
  private Boolean realTimeData;

  VehicleRentalStationBuilder(VehicleRentalStation original) {
    this.original = original;
  }

  public VehicleRentalStation original() {
    return original;
  }

  public FeedScopedId id() {
    return id == null ? original.id() : id;
  }

  public I18NString name() {
    return name == null ? original.name() : name;
  }

  public String shortName() {
    return shortName == null ? original.shortName() : shortName;
  }

  public double longitude() {
    return longitude == null ? original.longitude() : longitude;
  }

  public double latitude() {
    return latitude == null ? original.latitude() : latitude;
  }

  public String address() {
    return address == null ? original.address() : address;
  }

  public String crossStreet() {
    return crossStreet == null ? original.crossStreet() : crossStreet;
  }

  public String regionId() {
    return regionId == null ? original.regionId() : regionId;
  }

  public String postCode() {
    return postCode == null ? original.postCode() : postCode;
  }

  public Set<String> rentalMethods() {
    return rentalMethods.isEmpty() ? original.rentalMethods() : rentalMethods;
  }

  public boolean isVirtualStation() {
    return isVirtualStation == null ? original.isVirtualStation() : isVirtualStation;
  }

  public Geometry stationArea() {
    return stationArea == null ? original.stationArea() : stationArea;
  }

  public Integer capacity() {
    return capacity == null ? original.capacity() : capacity;
  }

  public Map<RentalVehicleType, Integer> vehicleTypeAreaCapacity() {
    return vehicleTypeAreaCapacity.isEmpty()
      ? original.vehicleTypeAreaCapacity()
      : vehicleTypeAreaCapacity;
  }

  public Map<RentalVehicleType, Integer> vehicleTypeDockCapacity() {
    return vehicleTypeDockCapacity.isEmpty()
      ? original.vehicleTypeDockCapacity()
      : vehicleTypeDockCapacity;
  }

  public boolean isValetStation() {
    return isValetStation == null ? original.isValetStation() : isValetStation;
  }

  public VehicleRentalSystem system() {
    return system == null ? original.system() : system;
  }

  public VehicleRentalStationUris rentalUris() {
    return rentalUris == null ? original.rentalUris() : rentalUris;
  }

  public int vehiclesAvailable() {
    return vehiclesAvailable == null ? original.vehiclesAvailable() : vehiclesAvailable;
  }

  public int vehiclesDisabled() {
    return vehiclesDisabled == null ? original.vehiclesDisabled() : vehiclesDisabled;
  }

  public Map<RentalVehicleType, Integer> vehicleTypesAvailable() {
    return vehicleTypesAvailable.isEmpty()
      ? original.vehicleTypesAvailable()
      : vehicleTypesAvailable;
  }

  public int spacesAvailable() {
    return spacesAvailable == null ? original.spacesAvailable() : spacesAvailable;
  }

  public int spacesDisabled() {
    return spacesDisabled == null ? original.spacesDisabled() : spacesDisabled;
  }

  public Map<RentalVehicleType, Integer> vehicleSpacesAvailable() {
    return vehicleSpacesAvailable.isEmpty()
      ? original.vehicleSpacesAvailable()
      : vehicleSpacesAvailable;
  }

  public boolean isInstalled() {
    return isInstalled == null ? original.isInstalled() : isInstalled;
  }

  public boolean isRenting() {
    return isRenting == null ? original.isRenting() : isRenting;
  }

  public boolean isReturning() {
    return isReturning == null ? original.isReturning() : isReturning;
  }

  public Instant lastReported() {
    return lastReported == null ? original.lastReported() : lastReported;
  }

  public boolean isOverloadingAllowed() {
    return overloadingAllowed == null ? original.overloadingAllowed() : overloadingAllowed;
  }

  public boolean isArrivingInRentalVehicleAtDestinationAllowed() {
    return isArrivingInRentalVehicleAtDestinationAllowed == null
      ? original.isArrivingInRentalVehicleAtDestinationAllowed()
      : isArrivingInRentalVehicleAtDestinationAllowed;
  }

  public boolean isRealTimeData() {
    return realTimeData == null ? original.isRealTimeData() : realTimeData;
  }

  public VehicleRentalStationBuilder withId(@Nullable FeedScopedId id) {
    this.id = id;
    return this;
  }

  public VehicleRentalStationBuilder withName(@Nullable I18NString name) {
    this.name = name;
    return this;
  }

  public VehicleRentalStationBuilder withShortName(@Nullable String shortName) {
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

  public VehicleRentalStationBuilder withAddress(@Nullable String address) {
    this.address = address;
    return this;
  }

  public VehicleRentalStationBuilder withCrossStreet(@Nullable String crossStreet) {
    this.crossStreet = crossStreet;
    return this;
  }

  public VehicleRentalStationBuilder withRegionId(@Nullable String regionId) {
    this.regionId = regionId;
    return this;
  }

  public VehicleRentalStationBuilder withPostCode(@Nullable String postCode) {
    this.postCode = postCode;
    return this;
  }

  public VehicleRentalStationBuilder withRentalMethods(@Nullable Set<String> rentalMethods) {
    this.rentalMethods = rentalMethods;
    return this;
  }

  public VehicleRentalStationBuilder withIsVirtualStation(boolean isVirtualStation) {
    this.isVirtualStation = isVirtualStation;
    return this;
  }

  public VehicleRentalStationBuilder withStationArea(@Nullable Geometry stationArea) {
    this.stationArea = stationArea;
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

  public VehicleRentalStationBuilder withIsValetStation(boolean isValetStation) {
    this.isValetStation = isValetStation;
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
    this.isInstalled = isInstalled;
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
    this.lastReported = lastReported;
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
