package org.opentripplanner.service.vehicleparking.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.model.calendar.openinghours.OHCalendar;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Vehicle parking locations, which may allow bicycle and/or car parking.
 * <p>
 * All fields are immutable except for the availability, capacity which may be updated by updaters.
 * If any other properties change a new VehicleParking instance should be created.
 */
public class VehicleParking implements Serializable {

  /**
   * The id of this vehicle parking, prefixed by the source(=feedId) so that it is unique.
   */
  private final FeedScopedId id;

  /**
   * The name of this vehicle parking, which may be translated when displaying to the user.
   */
  private final I18NString name;

  /**
   * The coordinate of the vehicle parking. It can be different than the coordinates for its
   * entrances.
   */
  private final WgsCoordinate coordinate;

  /**
   * URL which contains details of this vehicle parking.
   */
  private final String detailsUrl;

  /**
   * URL of an image which may be displayed to the user showing the vehicle parking.
   */
  private final String imageUrl;

  /**
   * Source specific tags of the vehicle parking, which describe the available features. For example
   * park_and_ride, bike_lockers, or static_osm_data.
   */
  private final Set<String> tags;

  /**
   * The opening hours of this vehicle parking, when it is possible to drop off / pickup a vehicle.
   * May be {@code null}.
   */
  private final OHCalendar openingHoursCalendar;

  /**
   * A short translatable note containing details of this vehicle parking.
   */
  private final I18NString note;

  /**
   * The state of this vehicle parking. Only ones in an OPERATIONAL state may be used for Park and
   * Ride.
   */
  private final VehicleParkingState state;

  /**
   * Does this vehicle parking have spaces (capacity) for bicycles.
   */
  private final boolean bicyclePlaces;

  /**
   * Does this vehicle parking have spaces (capacity) for cars.
   */
  private final boolean carPlaces;

  /**
   * Does this vehicle parking have wheelchair accessible (disabled) car spaces (capacity).
   */
  private final boolean wheelchairAccessibleCarPlaces;

  /**
   * The capacity (maximum available spaces) of this vehicle parking.
   */
  private final VehicleParkingSpaces capacity;
  /**
   * The entrances to enter and exit this vehicle parking.
   */
  private final List<VehicleParkingEntrance> entrances = new ArrayList<>();
  /**
   * The currently available spaces at this vehicle parking.
   * <p>
   * The volatile keyword is used to ensure safe publication by clearing CPU caches.
   */
  private volatile VehicleParkingSpaces availability;
  /**
   * The vehicle parking group this parking belongs to.
   */
  private final VehicleParkingGroup vehicleParkingGroup;

  VehicleParking(
    FeedScopedId id,
    I18NString name,
    WgsCoordinate coordinate,
    String detailsUrl,
    String imageUrl,
    Set<String> tags,
    OHCalendar openingHoursCalendar,
    I18NString note,
    VehicleParkingState state,
    boolean bicyclePlaces,
    boolean carPlaces,
    boolean wheelchairAccessibleCarPlaces,
    VehicleParkingSpaces capacity,
    VehicleParkingSpaces availability,
    VehicleParkingGroup vehicleParkingGroup
  ) {
    this.id = Objects.requireNonNull(
      id,
      "%s must have an ID".formatted(this.getClass().getSimpleName())
    );
    this.name = name;
    this.coordinate = Objects.requireNonNull(coordinate);
    this.detailsUrl = detailsUrl;
    this.imageUrl = imageUrl;
    this.tags = tags;
    this.openingHoursCalendar = openingHoursCalendar;
    this.note = note;
    this.state = state;
    this.bicyclePlaces = bicyclePlaces;
    this.carPlaces = carPlaces;
    this.wheelchairAccessibleCarPlaces = wheelchairAccessibleCarPlaces;
    this.capacity = capacity;
    this.availability = availability;
    this.vehicleParkingGroup = vehicleParkingGroup;
  }

  public static VehicleParkingBuilder builder() {
    return new VehicleParkingBuilder();
  }

  public FeedScopedId getId() {
    return id;
  }

  @Nullable
  public I18NString getName() {
    return name;
  }

  public WgsCoordinate getCoordinate() {
    return coordinate;
  }

  public String getDetailsUrl() {
    return detailsUrl;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public Set<String> getTags() {
    return tags;
  }

  public OHCalendar getOpeningHours() {
    return openingHoursCalendar;
  }

  public I18NString getNote() {
    return note;
  }

  public VehicleParkingState getState() {
    return state;
  }

  public VehicleParkingSpaces getCapacity() {
    return capacity;
  }

  public VehicleParkingSpaces getAvailability() {
    return availability;
  }

  public List<VehicleParkingEntrance> getEntrances() {
    return entrances;
  }

  @Nullable
  public VehicleParkingGroup getVehicleParkingGroup() {
    return vehicleParkingGroup;
  }

  public boolean hasBicyclePlaces() {
    return bicyclePlaces;
  }

  public boolean hasAnyCarPlaces() {
    return hasCarPlaces() || hasWheelchairAccessibleCarPlaces();
  }

  public boolean hasCarPlaces() {
    return carPlaces;
  }

  public boolean hasWheelchairAccessibleCarPlaces() {
    return wheelchairAccessibleCarPlaces;
  }

  public boolean hasRealTimeData() {
    return availability != null;
  }

  public boolean hasSpacesAvailable(TraverseMode traverseMode, boolean wheelchairAccessible) {
    switch (traverseMode) {
      case BICYCLE:
        return bicyclePlaces;
      case CAR:
        if (wheelchairAccessible) {
          return wheelchairAccessibleCarPlaces;
        } else {
          return carPlaces;
        }
      default:
        return false;
    }
  }

  public boolean hasRealTimeDataForMode(
    TraverseMode traverseMode,
    boolean wheelchairAccessibleCarPlaces
  ) {
    if (availability == null) {
      return false;
    }

    return switch (traverseMode) {
      case BICYCLE -> availability.getBicycleSpaces() != null;
      case CAR -> {
        var places = wheelchairAccessibleCarPlaces
          ? availability.getWheelchairAccessibleCarSpaces()
          : availability.getCarSpaces();
        yield places != null;
      }
      default -> false;
    };
  }

  /**
   * The only mutable method in this class: it allows to update the available parking spaces during
   * real-time updates.
   * Since the entity is used both by writer threads (real-time updates) and reader threads
   * (A* routing), the variable holding the information is marked as volatile.
   */
  public void updateAvailability(VehicleParkingSpaces vehicleParkingSpaces) {
    this.availability = vehicleParkingSpaces;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      id,
      name,
      coordinate,
      detailsUrl,
      imageUrl,
      tags,
      openingHoursCalendar,
      note,
      state,
      bicyclePlaces,
      carPlaces,
      wheelchairAccessibleCarPlaces,
      capacity,
      entrances,
      vehicleParkingGroup
    );
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final VehicleParking that = (VehicleParking) o;
    return (
      Objects.equals(coordinate, that.coordinate) &&
      bicyclePlaces == that.bicyclePlaces &&
      carPlaces == that.carPlaces &&
      wheelchairAccessibleCarPlaces == that.wheelchairAccessibleCarPlaces &&
      state == that.state &&
      Objects.equals(id, that.id) &&
      Objects.equals(name, that.name) &&
      Objects.equals(detailsUrl, that.detailsUrl) &&
      Objects.equals(imageUrl, that.imageUrl) &&
      Objects.equals(tags, that.tags) &&
      Objects.equals(openingHoursCalendar, that.openingHoursCalendar) &&
      Objects.equals(note, that.note) &&
      Objects.equals(capacity, that.capacity) &&
      Objects.equals(entrances, that.entrances) &&
      Objects.equals(vehicleParkingGroup, that.vehicleParkingGroup)
    );
  }

  public String toString() {
    return ToStringBuilder.of(VehicleParking.class)
      .addStr("id", id.toString())
      .addStr("name", name.toString())
      .addObj("coordinate", coordinate)
      .toString();
  }

  private void addEntrance(VehicleParkingEntranceCreator creator) {
    var entrance = creator
      .updateValues(VehicleParkingEntrance.builder().vehicleParking(this))
      .build();

    entrances.add(entrance);
  }

  @FunctionalInterface
  public interface VehicleParkingEntranceCreator {
    VehicleParkingEntrance.VehicleParkingEntranceBuilder updateValues(
      VehicleParkingEntrance.VehicleParkingEntranceBuilder builder
    );
  }

  @SuppressWarnings("unused")
  public static class VehicleParkingBuilder {

    private final List<VehicleParkingEntranceCreator> entranceCreators = new ArrayList<>();
    private Set<String> tags = Set.of();
    private OHCalendar openingHoursCalendar;
    private FeedScopedId id;
    private I18NString name;
    private WgsCoordinate coordinate;
    private String detailsUrl;
    private String imageUrl;
    private I18NString note;
    private VehicleParkingState state$value;
    private boolean state$set;
    private boolean bicyclePlaces;
    private boolean carPlaces;
    private boolean wheelchairAccessibleCarPlaces;
    private VehicleParkingSpaces capacity;
    private VehicleParkingSpaces availability;
    private VehicleParkingGroup vehicleParkingGroup;

    VehicleParkingBuilder() {}

    public VehicleParkingBuilder tags(Collection<String> tags) {
      this.tags = Set.copyOf(tags);
      return this;
    }

    public VehicleParkingBuilder openingHoursCalendar(OHCalendar openingHoursCalendar) {
      this.openingHoursCalendar = openingHoursCalendar;
      return this;
    }

    public VehicleParkingBuilder entrances(Collection<VehicleParkingEntranceCreator> creators) {
      this.entranceCreators.addAll(creators);
      return this;
    }

    public VehicleParkingBuilder entrance(VehicleParkingEntranceCreator creator) {
      this.entranceCreators.add(creator);
      return this;
    }

    public VehicleParkingBuilder id(FeedScopedId id) {
      this.id = id;
      return this;
    }

    public VehicleParkingBuilder name(I18NString name) {
      this.name = name;
      return this;
    }

    public VehicleParkingBuilder coordinate(WgsCoordinate coordinate) {
      this.coordinate = coordinate;
      return this;
    }

    public VehicleParkingBuilder detailsUrl(String detailsUrl) {
      this.detailsUrl = detailsUrl;
      return this;
    }

    public VehicleParkingBuilder imageUrl(String imageUrl) {
      this.imageUrl = imageUrl;
      return this;
    }

    public VehicleParkingBuilder note(I18NString note) {
      this.note = note;
      return this;
    }

    public VehicleParkingBuilder state(VehicleParkingState state) {
      this.state$value = state;
      this.state$set = true;
      return this;
    }

    public VehicleParkingBuilder bicyclePlaces(boolean bicyclePlaces) {
      this.bicyclePlaces = bicyclePlaces;
      return this;
    }

    public VehicleParkingBuilder carPlaces(boolean carPlaces) {
      this.carPlaces = carPlaces;
      return this;
    }

    public VehicleParkingBuilder wheelchairAccessibleCarPlaces(
      boolean wheelchairAccessibleCarPlaces
    ) {
      this.wheelchairAccessibleCarPlaces = wheelchairAccessibleCarPlaces;
      return this;
    }

    public VehicleParkingBuilder capacity(VehicleParkingSpaces capacity) {
      this.capacity = capacity;
      return this;
    }

    public VehicleParkingBuilder availability(VehicleParkingSpaces availability) {
      this.availability = availability;
      return this;
    }

    public VehicleParkingBuilder vehicleParkingGroup(VehicleParkingGroup vehicleParkingGroup) {
      this.vehicleParkingGroup = vehicleParkingGroup;
      return this;
    }

    public VehicleParking build() {
      VehicleParkingState state$value = this.state$value;
      if (!this.state$set) {
        state$value = VehicleParkingState.OPERATIONAL;
      }

      var vehicleParking = new VehicleParking(
        id,
        name,
        coordinate,
        detailsUrl,
        imageUrl,
        tags,
        openingHoursCalendar,
        note,
        state$value,
        bicyclePlaces,
        carPlaces,
        wheelchairAccessibleCarPlaces,
        capacity,
        availability,
        vehicleParkingGroup
      );
      this.entranceCreators.forEach(vehicleParking::addEntrance);
      return vehicleParking;
    }
  }
}
