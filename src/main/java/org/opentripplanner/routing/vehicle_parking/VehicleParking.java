package org.opentripplanner.routing.vehicle_parking;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.opentripplanner.common.RepeatingTimePeriod;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.util.I18NString;

/**
 * Vehicle parking locations, which may allow bicycle and/or car parking.
 *
 * All fields are immutable except for the availability, capacity which may be updated by updaters.
 * If any other properties change a new VehicleParking instance should be created.
 */
@Builder
@Getter
@EqualsAndHashCode
public class VehicleParking implements Serializable {

  /**
   * The id of the vehicle parking, prefixed by the source(=feedId) so that it is unique.
   */
  private final FeedScopedId id;

  private final I18NString name;

  /**
   * Note: x = Longitude, y = Latitude
   */
  private final double x, y;

  private final String detailsUrl;

  private final String imageUrl;

  /**
   * Source specific tags of the vehicle parking, which describe the available features. For example
   * park_and_ride, bike_lockers, or static_osm_data.
   */
  private final List<String> tags;

  // TODO: this would need to be parsed from the OSM format
  private final RepeatingTimePeriod openingHours;

  private final RepeatingTimePeriod feeHours;

  private final I18NString note;

  @Builder.Default
  private final VehicleParkingState state = VehicleParkingState.OPERATIONAL;

  @Getter(AccessLevel.NONE)
  private final boolean bicyclePlaces;

  @Getter(AccessLevel.NONE)
  private final boolean carPlaces;

  @Getter(AccessLevel.NONE)
  private final boolean wheelchairAccessibleCarPlaces;

  @EqualsAndHashCode.Exclude
  private VehiclePlaces capacity;

  @EqualsAndHashCode.Exclude
  private VehiclePlaces availability;

  private final List<VehicleParkingEntrance> entrances;

  public String toString() {
    return String.format(Locale.ROOT, "VehicleParking(%s at %.6f, %.6f)", name, y, x);
  }

  /**
   * The number of spaces by type. {@code null} if unknown.
   */
  @Data
  @Builder
  public static class VehiclePlaces implements Serializable {

    private final Integer bicycleSpaces;

    private final Integer carSpaces;

    private final Integer wheelchairAccessibleCarSpaces;
  }

  /**
   * The state of the vehicle parking. TEMPORARILY_CLOSED and CLOSED are distinct states so that
   * they may be represented differently to the user.
   */
  public enum VehicleParkingState {
    OPERATIONAL,
    TEMPORARILY_CLOSED,
    CLOSED
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

  public void updateVehiclePlaces(VehiclePlaces vehiclePlaces) {
    this.availability = vehiclePlaces;
    this.capacity = vehiclePlaces;
  }

  @Data
  @Builder
  @EqualsAndHashCode
  public static class VehicleParkingEntrance implements Serializable {
    private FeedScopedId entranceId;
    private double x, y;
    private I18NString name;
    // Used to explicitly specify the intersection to link to instead of using (x, y)
    @EqualsAndHashCode.Exclude
    private transient StreetVertex vertex;
    // If this entrance should be linked to car accessible streets
    private boolean carAccessible;
    // If this entrance should be linked to walk/bike accessible streets
    private boolean walkAccessible;

    void clearVertex() {
      vertex = null;
    }
  }

  /*
   * These methods are overwritten so that the saved list always an array list and also so that
   * there is a default value.
   */
  public static class VehicleParkingBuilder {
    private List<VehicleParkingEntrance> entrances = new ArrayList<>();
    private List<String> tags = new ArrayList<>();

    public VehicleParkingBuilder entrances(Collection<VehicleParkingEntrance> entrances) {
      this.entrances = new ArrayList<>(entrances);
      return this;
    }

    public VehicleParkingBuilder tags(Collection<String> tags) {
      this.tags = new ArrayList<>(tags);
      return this;
    }
  }
}
