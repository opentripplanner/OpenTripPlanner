package org.opentripplanner.routing.algorithm.mapping._support.model;

@Deprecated
public class ApiVehicleParkingSpaces {

  /**
   * The number of bicycle spaces. Maybe {@code null} if unknown.
   */
  public Integer bicycleSpaces;

  /**
   * The number of normal car spaces. Maybe {@code null} if unknown.
   */
  public Integer carSpaces;

  /**
   * The number of wheelchair accessible (disabled) car spaces. Maybe {@code null} if unknown.
   */
  public Integer wheelchairAccessibleCarSpaces;

  ApiVehicleParkingSpaces(
    Integer bicycleSpaces,
    Integer carSpaces,
    Integer wheelchairAccessibleCarSpaces
  ) {
    this.bicycleSpaces = bicycleSpaces;
    this.carSpaces = carSpaces;
    this.wheelchairAccessibleCarSpaces = wheelchairAccessibleCarSpaces;
  }

  public static ApiVehicleParkingSpacesBuilder builder() {
    return new ApiVehicleParkingSpacesBuilder();
  }

  public static class ApiVehicleParkingSpacesBuilder {

    private Integer bicycleSpaces;
    private Integer carSpaces;
    private Integer wheelchairAccessibleCarSpaces;

    ApiVehicleParkingSpacesBuilder() {}

    public ApiVehicleParkingSpacesBuilder bicycleSpaces(Integer bicycleSpaces) {
      this.bicycleSpaces = bicycleSpaces;
      return this;
    }

    public ApiVehicleParkingSpacesBuilder carSpaces(Integer carSpaces) {
      this.carSpaces = carSpaces;
      return this;
    }

    public ApiVehicleParkingSpacesBuilder wheelchairAccessibleCarSpaces(
      Integer wheelchairAccessibleCarSpaces
    ) {
      this.wheelchairAccessibleCarSpaces = wheelchairAccessibleCarSpaces;
      return this;
    }

    public ApiVehicleParkingSpaces build() {
      return new ApiVehicleParkingSpaces(bicycleSpaces, carSpaces, wheelchairAccessibleCarSpaces);
    }
  }
}
