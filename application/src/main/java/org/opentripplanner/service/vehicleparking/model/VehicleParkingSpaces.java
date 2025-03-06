package org.opentripplanner.service.vehicleparking.model;

import java.io.Serializable;
import java.util.Objects;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * The number of spaces by type. {@code null} if unknown.
 */
public class VehicleParkingSpaces implements Serializable {

  /**
   * The number of bicycle spaces.
   */
  private final Integer bicycleSpaces;

  /**
   * The number of car spaces.
   */
  private final Integer carSpaces;

  /**
   * The number of wheelchair accessible (disabled) car spaces.
   */
  private final Integer wheelchairAccessibleCarSpaces;

  VehicleParkingSpaces(
    Integer bicycleSpaces,
    Integer carSpaces,
    Integer wheelchairAccessibleCarSpaces
  ) {
    this.bicycleSpaces = bicycleSpaces;
    this.carSpaces = carSpaces;
    this.wheelchairAccessibleCarSpaces = wheelchairAccessibleCarSpaces;
  }

  public static VehicleParkingSpacesBuilder builder() {
    return new VehicleParkingSpacesBuilder();
  }

  public Integer getBicycleSpaces() {
    return bicycleSpaces;
  }

  public Integer getCarSpaces() {
    return carSpaces;
  }

  public Integer getWheelchairAccessibleCarSpaces() {
    return wheelchairAccessibleCarSpaces;
  }

  @Override
  public int hashCode() {
    return Objects.hash(bicycleSpaces, carSpaces, wheelchairAccessibleCarSpaces);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final VehicleParkingSpaces that = (VehicleParkingSpaces) o;
    return (
      Objects.equals(bicycleSpaces, that.bicycleSpaces) &&
      Objects.equals(carSpaces, that.carSpaces) &&
      Objects.equals(wheelchairAccessibleCarSpaces, that.wheelchairAccessibleCarSpaces)
    );
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(VehicleParkingSpaces.class)
      .addNum("carSpaces", carSpaces)
      .addNum("wheelchairAccessibleCarSpaces", wheelchairAccessibleCarSpaces)
      .addNum("bicycleSpaces", bicycleSpaces)
      .toString();
  }

  public static class VehicleParkingSpacesBuilder {

    private Integer bicycleSpaces;
    private Integer carSpaces;
    private Integer wheelchairAccessibleCarSpaces;

    VehicleParkingSpacesBuilder() {}

    public VehicleParkingSpacesBuilder bicycleSpaces(Integer bicycleSpaces) {
      this.bicycleSpaces = bicycleSpaces;
      return this;
    }

    public VehicleParkingSpacesBuilder carSpaces(Integer carSpaces) {
      this.carSpaces = carSpaces;
      return this;
    }

    public VehicleParkingSpacesBuilder wheelchairAccessibleCarSpaces(
      Integer wheelchairAccessibleCarSpaces
    ) {
      this.wheelchairAccessibleCarSpaces = wheelchairAccessibleCarSpaces;
      return this;
    }

    public VehicleParkingSpaces build() {
      return new VehicleParkingSpaces(bicycleSpaces, carSpaces, wheelchairAccessibleCarSpaces);
    }
  }
}
