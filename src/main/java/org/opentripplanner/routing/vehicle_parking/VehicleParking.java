package org.opentripplanner.routing.vehicle_parking;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.util.I18NString;

/**
 * Vehicle parking locations, which may allow bicycle and/or car parking.
 *
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
   * Note: x = Longitude, y = Latitude
   */
  private final double x, y;

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
   * A short translatable note containing details of this vehicle parking.
   */
  private final I18NString note;

  /**
   * The state of this vehicle parking. Only ones in an OPERATIONAL state may be used for Park and Ride.
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
   * The currently available spaces at this vehicle parking.
   */
  private VehicleParkingSpaces availability;

  /**
   * The entrances to enter and exit this vehicle parking.
   */
  private final List<VehicleParkingEntrance> entrances = new ArrayList<>();

  VehicleParking(
          FeedScopedId id,
          I18NString name,
          double x,
          double y,
          String detailsUrl,
          String imageUrl,
          Set<String> tags,
          I18NString note,
          VehicleParkingState state,
          boolean bicyclePlaces,
          boolean carPlaces,
          boolean wheelchairAccessibleCarPlaces,
          VehicleParkingSpaces capacity,
          VehicleParkingSpaces availability
  ) {
    this.id = id;
    this.name = name;
    this.x = x;
    this.y = y;
    this.detailsUrl = detailsUrl;
    this.imageUrl = imageUrl;
    this.tags = tags;
    this.note = note;
    this.state = state;
    this.bicyclePlaces = bicyclePlaces;
    this.carPlaces = carPlaces;
    this.wheelchairAccessibleCarPlaces = wheelchairAccessibleCarPlaces;
    this.capacity = capacity;
    this.availability = availability;
  }

  public FeedScopedId getId() {
    return id;
  }

  public I18NString getName() {
    return name;
  }

  public double getX() {
    return x;
  }

  public double getY() {
    return y;
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

  public boolean hasSpacesAvailable(TraverseMode traverseMode, boolean wheelchairAccessible, boolean useAvailability) {
    switch (traverseMode) {
      case BICYCLE:
        if (useAvailability && hasRealTimeDataForMode(TraverseMode.BICYCLE, false)) {
          return availability.getBicycleSpaces() > 0;
        } else {
          return bicyclePlaces;
        }
      case CAR:
        if (wheelchairAccessible) {
          if (useAvailability && hasRealTimeDataForMode(TraverseMode.CAR, true)) {
            return availability.getWheelchairAccessibleCarSpaces() > 0;
          } else {
            return wheelchairAccessibleCarPlaces;
          }
        } else {
          if (useAvailability && hasRealTimeDataForMode(TraverseMode.CAR, false)) {
            return availability.getCarSpaces() > 0;
          } else {
            return carPlaces;
          }
        }
      default:
        return false;
    }
  }

  public boolean hasRealTimeDataForMode(TraverseMode traverseMode, boolean wheelchairAccessibleCarPlaces) {
    if (availability == null) {
      return false;
    }

    switch (traverseMode) {
      case BICYCLE:
        return availability.getBicycleSpaces() != null;
      case CAR:
        var places = wheelchairAccessibleCarPlaces
                ? availability.getWheelchairAccessibleCarSpaces()
                : availability.getCarSpaces();
        return places != null;
      default:
        return false;
    }
  }

  public void updateAvailability(VehicleParkingSpaces vehicleParkingSpaces) {
    this.availability = vehicleParkingSpaces;
  }

  private void addEntrance(VehicleParkingEntranceCreator creator) {
    var entrance = creator.updateValues(VehicleParkingEntrance.builder()
            .vehicleParking(this))
            .build();

    entrances.add(entrance);
  }

  public String toString() {
    return String.format(Locale.ROOT, "VehicleParking(%s at %.6f, %.6f)", name, y, x);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {return true;}
    if (o == null || getClass() != o.getClass()) {return false;}
    final VehicleParking that = (VehicleParking) o;
    return Double.compare(that.x, x) == 0
            && Double.compare(that.y, y) == 0
            && bicyclePlaces == that.bicyclePlaces
            && carPlaces == that.carPlaces
            && wheelchairAccessibleCarPlaces == that.wheelchairAccessibleCarPlaces
            && state == that.state
            && Objects.equals(id, that.id)
            && Objects.equals(name, that.name)
            && Objects.equals(detailsUrl, that.detailsUrl)
            && Objects.equals(imageUrl, that.imageUrl)
            && Objects.equals(tags, that.tags)
            && Objects.equals(note, that.note)
            && Objects.equals(capacity, that.capacity)
            && Objects.equals(entrances, that.entrances);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
            id, name, x, y, detailsUrl, imageUrl, tags, note, state, bicyclePlaces,
            carPlaces, wheelchairAccessibleCarPlaces, capacity, entrances
    );
  }

  public static VehicleParkingBuilder builder() {
    return new VehicleParkingBuilder();
  }

  @FunctionalInterface
  public interface VehicleParkingEntranceCreator {
    VehicleParkingEntrance.VehicleParkingEntranceBuilder updateValues(VehicleParkingEntrance.VehicleParkingEntranceBuilder builder);
  }

  @SuppressWarnings("unused")
  public static class VehicleParkingBuilder {
    private Set<String> tags = Set.of();
    private final List<VehicleParkingEntranceCreator> entranceCreators = new ArrayList<>();
    private FeedScopedId id;
    private I18NString name;
    private double x;
    private double y;
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

    VehicleParkingBuilder() {}

    public VehicleParkingBuilder tags(Collection<String> tags) {
      this.tags = new HashSet<>(tags);
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

    public VehicleParkingBuilder x(double x) {
      this.x = x;
      return this;
    }

    public VehicleParkingBuilder y(double y) {
      this.y = y;
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

    public VehicleParkingBuilder wheelchairAccessibleCarPlaces(boolean wheelchairAccessibleCarPlaces) {
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

    public VehicleParking build() {
      VehicleParkingState state$value = this.state$value;
      if (!this.state$set) {
        state$value = VehicleParkingState.OPERATIONAL;
      }

      var vehicleParking = new VehicleParking(
              id, name, x, y, detailsUrl, imageUrl, tags, note, state$value,
              bicyclePlaces, carPlaces, wheelchairAccessibleCarPlaces, capacity, availability
      );
      this.entranceCreators.forEach(vehicleParking::addEntrance);
      return vehicleParking;
    }
  }
}
