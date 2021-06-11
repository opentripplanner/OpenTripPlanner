package org.opentripplanner.routing.vehicle_parking;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.util.I18NString;

/**
 * Vehicle parking locations, which may allow bicycle and/or car parking.
 *
 * All fields are immutable except for the availability, capacity which may be updated by updaters.
 * If any other properties change a new VehicleParking instance should be created.
 */
@Builder(buildMethodName = "buildInternal")
@Getter
@EqualsAndHashCode
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
  private final List<String> tags;

  /**
   * A short translatable note containing details of this vehicle parking.
   */
  private final I18NString note;

  /**
   * The state of this vehicle parking. Only ones in an OPERATIONAL state may be used for Park and Ride.
   */
  @Builder.Default
  private final VehicleParkingState state = VehicleParkingState.OPERATIONAL;

  /**
   * Does this vehicle parking have spaces (capacity) for bicycles.
   */
  @Getter(AccessLevel.NONE)
  private final boolean bicyclePlaces;

  /**
   * Does this vehicle parking have spaces (capacity) for cars.
   */
  @Getter(AccessLevel.NONE)
  private final boolean carPlaces;

  /**
   * Does this vehicle parking have disabled (wheelchair accessible) car spaces (capacity).
   */
  @Getter(AccessLevel.NONE)
  private final boolean wheelchairAccessibleCarPlaces;

  /**
   * The capacity (maximum available spaces) of this vehicle parking.
   */
  private final VehicleParkingSpaces capacity;

  /**
   * The currently available spaces at this vehicle parking.
   */
  @EqualsAndHashCode.Exclude
  private VehicleParkingSpaces availability;

  @Builder.Default
  private final List<VehicleParkingEntrance> entrances = new ArrayList<>();

  public boolean hasBicyclePlaces() {
    return bicyclePlaces;
  }

  public boolean hasAnyCarPlaces() {
    return hasCarPlaces() || hasWheelchairAccessibledCarPlaces();
  }

  public boolean hasCarPlaces() {
    return carPlaces;
  }

  public boolean hasWheelchairAccessibledCarPlaces() {
    return wheelchairAccessibleCarPlaces;
  }

  public boolean hasRealTimeData() {
    return availability != null;
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

  @FunctionalInterface
  public interface VehicleParkingEntranceCreator {
    VehicleParkingEntrance.VehicleParkingEntranceBuilder updateValues(VehicleParkingEntrance.VehicleParkingEntranceBuilder builder);
  }

  /*
   * These methods are overwritten so that the saved list is always an array list for serialization.
   */
  @SuppressWarnings("unused")
  public static class VehicleParkingBuilder {
    private List<String> tags = new ArrayList<>();
    private final List<VehicleParkingEntranceCreator> entranceCreators = new ArrayList<>();

    public VehicleParkingBuilder tags(Collection<String> tags) {
      this.tags = new ArrayList<>(tags);
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

    public VehicleParking build() {
        VehicleParking vehicleParking = this.buildInternal();
        this.entranceCreators.forEach(vehicleParking::addEntrance);
        return vehicleParking;
    }
  }
}
