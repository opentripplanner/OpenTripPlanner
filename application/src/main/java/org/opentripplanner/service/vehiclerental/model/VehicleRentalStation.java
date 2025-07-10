package org.opentripplanner.service.vehiclerental.model;

import static java.util.Locale.ROOT;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.street.model.RentalFormFactor;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.collection.SetUtils;

/**
 * Implements the {@link VehicleRentalPlace} class which contains Javadoc.
 * <p>
 */
public final class VehicleRentalStation implements VehicleRentalPlace {

  public static final VehicleRentalStation DEFAULT = new VehicleRentalStation();

  // GBFS Static information
  private final FeedScopedId id;
  private final I18NString name;
  private final String shortName;
  private final double longitude;
  private final double latitude;
  private final String address;
  private final String crossStreet;
  private final String regionId;
  private final String postCode;
  private final Set<String> rentalMethods;
  private boolean isVirtualStation = false;
  private final Geometry stationArea;
  private final Integer capacity;
  private final Map<RentalVehicleType, Integer> vehicleTypeAreaCapacity;
  private final Map<RentalVehicleType, Integer> vehicleTypeDockCapacity;
  private boolean isValetStation = false;
  private final VehicleRentalSystem system;
  private final VehicleRentalStationUris rentalUris;

  // GBFS Dynamic information
  private final int vehiclesAvailable;
  private final int vehiclesDisabled;
  private final Map<RentalVehicleType, Integer> vehicleTypesAvailable;
  private final int spacesAvailable;
  private final int spacesDisabled;
  private final Map<RentalVehicleType, Integer> vehicleSpacesAvailable;

  private final boolean isInstalled;
  private final boolean isRenting;
  private final boolean isReturning;
  private final Instant lastReported;

  // OTP internal data
  private final boolean overloadingAllowed;
  private final boolean isArrivingInRentalVehicleAtDestinationAllowed;
  private final boolean realTimeData;

  public VehicleRentalStation() {
    this.id = null;
    this.name = null;
    this.shortName = null;
    this.longitude = 0.0;
    this.latitude = 0.0;
    this.address = null;
    this.crossStreet = null;
    this.regionId = null;
    this.postCode = null;
    this.rentalMethods = Set.of();
    this.isVirtualStation = false;
    this.stationArea = null;
    this.capacity = null;
    this.vehicleTypeAreaCapacity = Map.of();
    this.vehicleTypeDockCapacity = Map.of();
    this.isValetStation = false;
    this.system = null;
    this.rentalUris = null;
    this.vehiclesAvailable = 0;
    this.vehiclesDisabled = 0;
    this.vehicleTypesAvailable = Map.of();
    this.spacesAvailable = 0;
    this.spacesDisabled = 0;
    this.vehicleSpacesAvailable = Map.of();
    this.isInstalled = true;
    this.isRenting = true;
    this.isReturning = true;
    this.lastReported = null;
    this.overloadingAllowed = false;
    this.isArrivingInRentalVehicleAtDestinationAllowed = false;
    this.realTimeData = true;
  }

  private VehicleRentalStation(Builder builder) {
    this.id = builder.id;
    this.name = builder.name;
    this.shortName = builder.shortName;
    this.longitude = builder.longitude;
    this.latitude = builder.latitude;
    this.address = builder.address;
    this.crossStreet = builder.crossStreet;
    this.regionId = builder.regionId;
    this.postCode = builder.postCode;
    this.rentalMethods = builder.rentalMethods != null
      ? Set.copyOf(builder.rentalMethods)
      : Set.of();
    this.isVirtualStation = builder.isVirtualStation;
    this.stationArea = builder.stationArea;
    this.capacity = builder.capacity;
    this.vehicleTypeAreaCapacity = builder.vehicleTypeAreaCapacity != null
      ? Map.copyOf(builder.vehicleTypeAreaCapacity)
      : Map.of();
    this.vehicleTypeDockCapacity = builder.vehicleTypeDockCapacity != null
      ? Map.copyOf(builder.vehicleTypeDockCapacity)
      : Map.of();
    this.isValetStation = builder.isValetStation;
    this.system = builder.system;
    this.rentalUris = builder.rentalUris;
    this.vehiclesAvailable = builder.vehiclesAvailable;
    this.vehiclesDisabled = builder.vehiclesDisabled;
    this.vehicleTypesAvailable = builder.vehicleTypesAvailable != null
      ? Map.copyOf(builder.vehicleTypesAvailable)
      : Map.of();
    this.spacesAvailable = builder.spacesAvailable;
    this.spacesDisabled = builder.spacesDisabled;
    this.vehicleSpacesAvailable = builder.vehicleSpacesAvailable != null
      ? Map.copyOf(builder.vehicleSpacesAvailable)
      : Map.of();
    this.isInstalled = builder.isInstalled;
    this.isRenting = builder.isRenting;
    this.isReturning = builder.isReturning;
    this.lastReported = builder.lastReported;
    this.overloadingAllowed = builder.overloadingAllowed;
    this.isArrivingInRentalVehicleAtDestinationAllowed =
      builder.isArrivingInRentalVehicleAtDestinationAllowed;
    this.realTimeData = builder.realTimeData;
  }

  public static Builder of() {
    return DEFAULT.copyOf();
  }

  public Builder copyOf() {
    return new Builder(this);
  }

  @Nullable
  public FeedScopedId id() {
    return id;
  }

  @Nullable
  public I18NString name() {
    return name;
  }

  @Nullable
  public String shortName() {
    return shortName;
  }

  public double longitude() {
    return longitude;
  }

  public double latitude() {
    return latitude;
  }

  @Nullable
  public String address() {
    return address;
  }

  @Nullable
  public String crossStreet() {
    return crossStreet;
  }

  @Nullable
  public String regionId() {
    return regionId;
  }

  @Nullable
  public String postCode() {
    return postCode;
  }

  public Set<String> rentalMethods() {
    return rentalMethods;
  }

  public boolean isVirtualStation() {
    return isVirtualStation;
  }

  @Nullable
  public Geometry stationArea() {
    return stationArea;
  }

  @Nullable
  public Integer capacity() {
    return capacity;
  }

  public Map<RentalVehicleType, Integer> vehicleTypeAreaCapacity() {
    return vehicleTypeAreaCapacity;
  }

  public Map<RentalVehicleType, Integer> vehicleTypeDockCapacity() {
    return vehicleTypeDockCapacity;
  }

  public boolean isValetStation() {
    return isValetStation;
  }

  @Nullable
  public VehicleRentalSystem system() {
    return system;
  }

  @Nullable
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

  public boolean isInstalled() {
    return isInstalled;
  }

  public boolean isRenting() {
    return isRenting;
  }

  public boolean isReturning() {
    return isReturning;
  }

  @Nullable
  public Instant lastReported() {
    return lastReported;
  }

  public boolean realTimeData() {
    return realTimeData;
  }

  @Override
  public String stationId() {
    return this.id().getId();
  }

  @Override
  public String network() {
    return this.id().getFeedId();
  }

  @Override
  public boolean isAllowDropoff() {
    return isReturning;
  }

  @Override
  public boolean isAllowPickup() {
    return isRenting;
  }

  public boolean allowPickupNow() {
    return isRenting && vehiclesAvailable > 0;
  }

  public boolean allowDropoffNow() {
    return isReturning && (spacesAvailable > 0 || overloadingAllowed);
  }

  @Override
  public boolean isFloatingVehicle() {
    return false;
  }

  @Override
  public boolean isCarStation() {
    return Stream.concat(
      vehicleTypesAvailable.keySet().stream(),
      vehicleSpacesAvailable.keySet().stream()
    ).anyMatch(rentalVehicleType -> rentalVehicleType.formFactor().equals(RentalFormFactor.CAR));
  }

  @Override
  public Set<RentalFormFactor> availablePickupFormFactors(boolean includeRealtimeAvailability) {
    return vehicleTypesAvailable
      .entrySet()
      .stream()
      .filter(e -> !includeRealtimeAvailability || e.getValue() > 0)
      .map(e -> e.getKey().formFactor())
      .collect(Collectors.toSet());
  }

  @Override
  public Set<RentalFormFactor> availableDropoffFormFactors(boolean includeRealtimeAvailability) {
    return vehicleSpacesAvailable
      .entrySet()
      .stream()
      .filter(e -> !includeRealtimeAvailability || overloadingAllowed || e.getValue() > 0)
      .map(e -> e.getKey().formFactor())
      .collect(Collectors.toSet());
  }

  @Override
  public boolean isArrivingInRentalVehicleAtDestinationAllowed() {
    return isArrivingInRentalVehicleAtDestinationAllowed;
  }

  @Override
  public boolean overloadingAllowed() {
    return overloadingAllowed;
  }

  @Override
  public boolean isRealTimeData() {
    return realTimeData;
  }

  @Override
  public VehicleRentalSystem vehicleRentalSystem() {
    return system;
  }

  @Override
  public String toString() {
    return String.format(
      ROOT,
      "Vehicle rental station %s at %.6f, %.6f",
      name,
      latitude,
      longitude
    );
  }

  public Set<RentalFormFactor> formFactors() {
    return SetUtils.combine(availableDropoffFormFactors(false), availablePickupFormFactors(false));
  }

  /**
   * @return Counts of available vehicles by type as well as the total number of available vehicles.
   */
  public RentalVehicleEntityCounts vehicleTypeCounts() {
    return new RentalVehicleEntityCounts(
      vehiclesAvailable,
      vehicleRentalTypeMapToList(vehicleTypesAvailable)
    );
  }

  /**
   * @return Counts of available vehicle spaces by type as well as the total number of available
   * vehicle spaces.
   */
  public RentalVehicleEntityCounts vehicleSpaceCounts() {
    return new RentalVehicleEntityCounts(
      spacesAvailable,
      vehicleRentalTypeMapToList(vehicleSpacesAvailable)
    );
  }

  private List<RentalVehicleTypeCount> vehicleRentalTypeMapToList(
    Map<RentalVehicleType, Integer> vehicleTypeMap
  ) {
    return vehicleTypeMap
      .entrySet()
      .stream()
      .map(vtc -> new RentalVehicleTypeCount(vtc.getKey(), vtc.getValue()))
      // we sort to have reproducible results in tests
      .sorted(Comparator.comparing(count -> count.vehicleType().id().toString()))
      .toList();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    VehicleRentalStation that = (VehicleRentalStation) o;
    return (
      Double.compare(that.longitude, longitude) == 0 &&
      Double.compare(that.latitude, latitude) == 0 &&
      isVirtualStation == that.isVirtualStation &&
      isValetStation == that.isValetStation &&
      vehiclesAvailable == that.vehiclesAvailable &&
      vehiclesDisabled == that.vehiclesDisabled &&
      spacesAvailable == that.spacesAvailable &&
      spacesDisabled == that.spacesDisabled &&
      isInstalled == that.isInstalled &&
      isRenting == that.isRenting &&
      isReturning == that.isReturning &&
      overloadingAllowed == that.overloadingAllowed &&
      isArrivingInRentalVehicleAtDestinationAllowed ==
      that.isArrivingInRentalVehicleAtDestinationAllowed &&
      realTimeData == that.realTimeData &&
      Objects.equals(id, that.id) &&
      Objects.equals(name, that.name) &&
      Objects.equals(shortName, that.shortName) &&
      Objects.equals(address, that.address) &&
      Objects.equals(crossStreet, that.crossStreet) &&
      Objects.equals(regionId, that.regionId) &&
      Objects.equals(postCode, that.postCode) &&
      Objects.equals(rentalMethods, that.rentalMethods) &&
      Objects.equals(stationArea, that.stationArea) &&
      Objects.equals(capacity, that.capacity) &&
      Objects.equals(vehicleTypeAreaCapacity, that.vehicleTypeAreaCapacity) &&
      Objects.equals(vehicleTypeDockCapacity, that.vehicleTypeDockCapacity) &&
      Objects.equals(system, that.system) &&
      Objects.equals(rentalUris, that.rentalUris) &&
      Objects.equals(vehicleTypesAvailable, that.vehicleTypesAvailable) &&
      Objects.equals(vehicleSpacesAvailable, that.vehicleSpacesAvailable) &&
      Objects.equals(lastReported, that.lastReported)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      id,
      name,
      shortName,
      longitude,
      latitude,
      address,
      crossStreet,
      regionId,
      postCode,
      rentalMethods,
      isVirtualStation,
      stationArea,
      capacity,
      vehicleTypeAreaCapacity,
      vehicleTypeDockCapacity,
      isValetStation,
      system,
      rentalUris,
      vehiclesAvailable,
      vehiclesDisabled,
      vehicleTypesAvailable,
      spacesAvailable,
      spacesDisabled,
      vehicleSpacesAvailable,
      isInstalled,
      isRenting,
      isReturning,
      lastReported,
      overloadingAllowed,
      isArrivingInRentalVehicleAtDestinationAllowed,
      realTimeData
    );
  }

  public static class Builder {

    private final VehicleRentalStation original;
    private FeedScopedId id;
    private I18NString name;
    private String shortName;
    private double longitude;
    private double latitude;
    private String address;
    private String crossStreet;
    private String regionId;
    private String postCode;
    private Set<String> rentalMethods;
    private boolean isVirtualStation;
    private Geometry stationArea;
    private Integer capacity;
    private Map<RentalVehicleType, Integer> vehicleTypeAreaCapacity;
    private Map<RentalVehicleType, Integer> vehicleTypeDockCapacity;
    private boolean isValetStation;
    private VehicleRentalSystem system;
    private VehicleRentalStationUris rentalUris;
    private int vehiclesAvailable;
    private int vehiclesDisabled;
    private Map<RentalVehicleType, Integer> vehicleTypesAvailable;
    private int spacesAvailable;
    private int spacesDisabled;
    private Map<RentalVehicleType, Integer> vehicleSpacesAvailable;
    private boolean isInstalled;
    private boolean isRenting;
    private boolean isReturning;
    private Instant lastReported;
    private boolean overloadingAllowed;
    private boolean isArrivingInRentalVehicleAtDestinationAllowed;
    private boolean realTimeData;

    private Builder(VehicleRentalStation original) {
      this.original = original;
      this.id = original.id;
      this.name = original.name;
      this.shortName = original.shortName;
      this.longitude = original.longitude;
      this.latitude = original.latitude;
      this.address = original.address;
      this.crossStreet = original.crossStreet;
      this.regionId = original.regionId;
      this.postCode = original.postCode;
      this.rentalMethods = original.rentalMethods;
      this.isVirtualStation = original.isVirtualStation;
      this.stationArea = original.stationArea;
      this.capacity = original.capacity;
      this.vehicleTypeAreaCapacity = original.vehicleTypeAreaCapacity;
      this.vehicleTypeDockCapacity = original.vehicleTypeDockCapacity;
      this.isValetStation = original.isValetStation;
      this.system = original.system;
      this.rentalUris = original.rentalUris;
      this.vehiclesAvailable = original.vehiclesAvailable;
      this.vehiclesDisabled = original.vehiclesDisabled;
      this.vehicleTypesAvailable = original.vehicleTypesAvailable;
      this.spacesAvailable = original.spacesAvailable;
      this.spacesDisabled = original.spacesDisabled;
      this.vehicleSpacesAvailable = original.vehicleSpacesAvailable;
      this.isInstalled = original.isInstalled;
      this.isRenting = original.isRenting;
      this.isReturning = original.isReturning;
      this.lastReported = original.lastReported;
      this.overloadingAllowed = original.overloadingAllowed;
      this.isArrivingInRentalVehicleAtDestinationAllowed =
        original.isArrivingInRentalVehicleAtDestinationAllowed;
      this.realTimeData = original.realTimeData;
    }

    public VehicleRentalStation original() {
      return original;
    }

    public Builder withId(@Nullable FeedScopedId id) {
      this.id = id;
      return this;
    }

    public Builder withName(@Nullable I18NString name) {
      this.name = name;
      return this;
    }

    public Builder withShortName(@Nullable String shortName) {
      this.shortName = shortName;
      return this;
    }

    public Builder withLongitude(double longitude) {
      this.longitude = longitude;
      return this;
    }

    public Builder withLatitude(double latitude) {
      this.latitude = latitude;
      return this;
    }

    public Builder withAddress(@Nullable String address) {
      this.address = address;
      return this;
    }

    public Builder withCrossStreet(@Nullable String crossStreet) {
      this.crossStreet = crossStreet;
      return this;
    }

    public Builder withRegionId(@Nullable String regionId) {
      this.regionId = regionId;
      return this;
    }

    public Builder withPostCode(@Nullable String postCode) {
      this.postCode = postCode;
      return this;
    }

    public Builder withRentalMethods(@Nullable Set<String> rentalMethods) {
      this.rentalMethods = rentalMethods;
      return this;
    }

    public Builder withIsVirtualStation(boolean isVirtualStation) {
      this.isVirtualStation = isVirtualStation;
      return this;
    }

    public Builder withStationArea(@Nullable Geometry stationArea) {
      this.stationArea = stationArea;
      return this;
    }

    public Builder withCapacity(@Nullable Integer capacity) {
      this.capacity = capacity;
      return this;
    }

    public Builder withVehicleTypeAreaCapacity(
      @Nullable Map<RentalVehicleType, Integer> vehicleTypeAreaCapacity
    ) {
      this.vehicleTypeAreaCapacity = vehicleTypeAreaCapacity;
      return this;
    }

    public Builder withVehicleTypeDockCapacity(
      @Nullable Map<RentalVehicleType, Integer> vehicleTypeDockCapacity
    ) {
      this.vehicleTypeDockCapacity = vehicleTypeDockCapacity;
      return this;
    }

    public Builder withIsValetStation(boolean isValetStation) {
      this.isValetStation = isValetStation;
      return this;
    }

    public Builder withSystem(@Nullable VehicleRentalSystem system) {
      this.system = system;
      return this;
    }

    public Builder withRentalUris(@Nullable VehicleRentalStationUris rentalUris) {
      this.rentalUris = rentalUris;
      return this;
    }

    public Builder withVehiclesAvailable(int vehiclesAvailable) {
      this.vehiclesAvailable = vehiclesAvailable;
      return this;
    }

    public Builder withVehiclesDisabled(int vehiclesDisabled) {
      this.vehiclesDisabled = vehiclesDisabled;
      return this;
    }

    public Builder withVehicleTypesAvailable(
      @Nullable Map<RentalVehicleType, Integer> vehicleTypesAvailable
    ) {
      this.vehicleTypesAvailable = vehicleTypesAvailable;
      return this;
    }

    public Builder withSpacesAvailable(int spacesAvailable) {
      this.spacesAvailable = spacesAvailable;
      return this;
    }

    public Builder withSpacesDisabled(int spacesDisabled) {
      this.spacesDisabled = spacesDisabled;
      return this;
    }

    public Builder withVehicleSpacesAvailable(
      @Nullable Map<RentalVehicleType, Integer> vehicleSpacesAvailable
    ) {
      this.vehicleSpacesAvailable = vehicleSpacesAvailable;
      return this;
    }

    public Builder withIsInstalled(boolean isInstalled) {
      this.isInstalled = isInstalled;
      return this;
    }

    public Builder withIsRenting(boolean isRenting) {
      this.isRenting = isRenting;
      return this;
    }

    public Builder withIsReturning(boolean isReturning) {
      this.isReturning = isReturning;
      return this;
    }

    public Builder withLastReported(@Nullable Instant lastReported) {
      this.lastReported = lastReported;
      return this;
    }

    public Builder withOverloadingAllowed(boolean overloadingAllowed) {
      this.overloadingAllowed = overloadingAllowed;
      return this;
    }

    public Builder withIsArrivingInRentalVehicleAtDestinationAllowed(
      boolean isArrivingInRentalVehicleAtDestinationAllowed
    ) {
      this.isArrivingInRentalVehicleAtDestinationAllowed =
        isArrivingInRentalVehicleAtDestinationAllowed;
      return this;
    }

    public Builder withRealTimeData(boolean realTimeData) {
      this.realTimeData = realTimeData;
      return this;
    }

    public Builder apply(Consumer<Builder> body) {
      body.accept(this);
      return this;
    }

    public VehicleRentalStation build() {
      var value = new VehicleRentalStation(this);
      return original.equals(value) ? original : value;
    }
  }
}
