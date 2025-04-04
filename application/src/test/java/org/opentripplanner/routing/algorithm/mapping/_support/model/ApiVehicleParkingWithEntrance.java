package org.opentripplanner.routing.algorithm.mapping._support.model;

import java.util.List;

/**
 * The details of a parking place along with the entrance used.
 */
@Deprecated
public class ApiVehicleParkingWithEntrance {

  /**
   * The id of the vehicle parking.
   */
  public final String id;

  /**
   * The name of the vehicle parking.
   */
  public final String name;

  /**
   * The id of the entrance.
   */
  public final String entranceId;

  /**
   * The name of the entrance.
   */
  public final String entranceName;

  /**
   * An optional url to view the details of this vehicle parking.
   */
  public final String detailsUrl;

  /**
   * An optional url of an image of this vehicle parking.
   */
  public final String imageUrl;

  /**
   * An optional note regarding this vehicle parking.
   */
  public final String note;

  /**
   * A list of attributes, features which this vehicle parking has.
   */
  public final List<String> tags;

  /**
   * True if there are bicycles spaces.
   */
  public final boolean hasBicyclePlaces;

  /**
   * Is any type of car parking possible?
   */
  public final boolean hasAnyCarPlaces;

  /**
   * True if there are spaces for normal cars.
   */
  public final boolean hasCarPlaces;

  /**
   * True if there are disabled car spaces.
   */
  public final boolean hasWheelchairAccessibleCarPlaces;

  /**
   * The capacity of the vehicle parking, if known. Maybe {@code null} if unknown.
   */
  public final ApiVehicleParkingSpaces capacity;

  /**
   * The number of available spaces. Only present if there is a real-time updater present. Maybe
   * {@code null} if unknown.
   */
  public final ApiVehicleParkingSpaces availability;

  /**
   * True if real-time information is used for checking availability.
   */
  public final boolean realtime;

  ApiVehicleParkingWithEntrance(
    String id,
    String name,
    String entranceId,
    String entranceName,
    String detailsUrl,
    String imageUrl,
    String note,
    List<String> tags,
    boolean hasBicyclePlaces,
    boolean hasAnyCarPlaces,
    boolean hasCarPlaces,
    boolean hasWheelchairAccessibleCarPlaces,
    ApiVehicleParkingSpaces capacity,
    ApiVehicleParkingSpaces availability,
    boolean realTime
  ) {
    this.id = id;
    this.name = name;
    this.entranceId = entranceId;
    this.entranceName = entranceName;
    this.detailsUrl = detailsUrl;
    this.imageUrl = imageUrl;
    this.note = note;
    this.tags = tags;
    this.hasBicyclePlaces = hasBicyclePlaces;
    this.hasAnyCarPlaces = hasAnyCarPlaces;
    this.hasCarPlaces = hasCarPlaces;
    this.hasWheelchairAccessibleCarPlaces = hasWheelchairAccessibleCarPlaces;
    this.capacity = capacity;
    this.availability = availability;
    this.realtime = realTime;
  }

  public static ApiVehicleParkingWithEntranceBuilder builder() {
    return new ApiVehicleParkingWithEntranceBuilder();
  }

  public static class ApiVehicleParkingWithEntranceBuilder {

    private String id;
    private String name;
    private String entranceId;
    private String entranceName;
    private String detailsUrl;
    private String imageUrl;
    private String note;
    private List<String> tags;
    private boolean hasBicyclePlaces;
    private boolean hasAnyCarPlaces;
    private boolean hasCarPlaces;
    private boolean hasWheelchairAccessibleCarPlaces;
    private ApiVehicleParkingSpaces capacity;
    private ApiVehicleParkingSpaces availability;
    private boolean realTime;

    ApiVehicleParkingWithEntranceBuilder() {}

    public ApiVehicleParkingWithEntranceBuilder id(String id) {
      this.id = id;
      return this;
    }

    public ApiVehicleParkingWithEntranceBuilder name(String name) {
      this.name = name;
      return this;
    }

    public ApiVehicleParkingWithEntranceBuilder entranceId(String entranceId) {
      this.entranceId = entranceId;
      return this;
    }

    public ApiVehicleParkingWithEntranceBuilder entranceName(String entranceName) {
      this.entranceName = entranceName;
      return this;
    }

    public ApiVehicleParkingWithEntranceBuilder detailsUrl(String detailsUrl) {
      this.detailsUrl = detailsUrl;
      return this;
    }

    public ApiVehicleParkingWithEntranceBuilder imageUrl(String imageUrl) {
      this.imageUrl = imageUrl;
      return this;
    }

    public ApiVehicleParkingWithEntranceBuilder note(String note) {
      this.note = note;
      return this;
    }

    public ApiVehicleParkingWithEntranceBuilder tags(List<String> tags) {
      this.tags = tags;
      return this;
    }

    public ApiVehicleParkingWithEntranceBuilder hasBicyclePlaces(boolean hasBicyclePlaces) {
      this.hasBicyclePlaces = hasBicyclePlaces;
      return this;
    }

    public ApiVehicleParkingWithEntranceBuilder hasAnyCarPlaces(boolean hasAnyCarPlaces) {
      this.hasAnyCarPlaces = hasAnyCarPlaces;
      return this;
    }

    public ApiVehicleParkingWithEntranceBuilder hasCarPlaces(boolean hasCarPlaces) {
      this.hasCarPlaces = hasCarPlaces;
      return this;
    }

    public ApiVehicleParkingWithEntranceBuilder hasWheelchairAccessibleCarPlaces(
      boolean hasWheelchairAccessibleCarPlaces
    ) {
      this.hasWheelchairAccessibleCarPlaces = hasWheelchairAccessibleCarPlaces;
      return this;
    }

    public ApiVehicleParkingWithEntranceBuilder capacity(ApiVehicleParkingSpaces capacity) {
      this.capacity = capacity;
      return this;
    }

    public ApiVehicleParkingWithEntranceBuilder availability(ApiVehicleParkingSpaces availability) {
      this.availability = availability;
      return this;
    }

    public ApiVehicleParkingWithEntranceBuilder realTime(boolean realTime) {
      this.realTime = realTime;
      return this;
    }

    public ApiVehicleParkingWithEntrance build() {
      return new ApiVehicleParkingWithEntrance(
        id,
        name,
        entranceId,
        entranceName,
        detailsUrl,
        imageUrl,
        note,
        tags,
        hasBicyclePlaces,
        hasAnyCarPlaces,
        hasCarPlaces,
        hasWheelchairAccessibleCarPlaces,
        capacity,
        availability,
        realTime
      );
    }
  }
}
