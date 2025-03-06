package org.opentripplanner.service.vehicleparking.model;

import java.io.Serializable;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.tostring.ToStringBuilder;

public class VehicleParkingEntrance implements Serializable {

  private final VehicleParking vehicleParking;

  private final FeedScopedId entranceId;

  private final WgsCoordinate coordinate;

  private final I18NString name;
  // If this entrance should be linked to car accessible streets
  private final boolean carAccessible;
  // If this entrance should be linked to walk/bike accessible streets
  private final boolean walkAccessible;
  // Used to explicitly specify the intersection to link to instead of using (x, y)
  private transient StreetVertex vertex;

  VehicleParkingEntrance(
    VehicleParking vehicleParking,
    FeedScopedId entranceId,
    WgsCoordinate coordinate,
    I18NString name,
    StreetVertex vertex,
    boolean carAccessible,
    boolean walkAccessible
  ) {
    this.vehicleParking = vehicleParking;
    this.entranceId = entranceId;
    this.coordinate = coordinate;
    this.name = name;
    this.vertex = vertex;
    this.carAccessible = carAccessible;
    this.walkAccessible = walkAccessible;
  }

  public static VehicleParkingEntranceBuilder builder() {
    return new VehicleParkingEntranceBuilder();
  }

  public VehicleParking getVehicleParking() {
    return vehicleParking;
  }

  public FeedScopedId getEntranceId() {
    return entranceId;
  }

  public WgsCoordinate getCoordinate() {
    return coordinate;
  }

  @Nullable
  public I18NString getName() {
    return name;
  }

  public StreetVertex getVertex() {
    return vertex;
  }

  public boolean isCarAccessible() {
    return carAccessible;
  }

  public boolean isWalkAccessible() {
    return walkAccessible;
  }

  @Override
  public int hashCode() {
    return Objects.hash(entranceId, coordinate, name, carAccessible, walkAccessible);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final VehicleParkingEntrance that = (VehicleParkingEntrance) o;
    return (
      Objects.equals(coordinate, that.coordinate) &&
      carAccessible == that.carAccessible &&
      walkAccessible == that.walkAccessible &&
      Objects.equals(entranceId, that.entranceId) &&
      Objects.equals(name, that.name)
    );
  }

  public String toString() {
    return ToStringBuilder.of(VehicleParkingEntrance.class)
      .addObj("entranceId", entranceId)
      .addObj("name", name)
      .addObj("coordinate", coordinate)
      .addBool("carAccessible", carAccessible)
      .addBool("walkAccessible", walkAccessible)
      .toString();
  }

  void clearVertex() {
    vertex = null;
  }

  public static class VehicleParkingEntranceBuilder {

    private VehicleParking vehicleParking;
    private FeedScopedId entranceId;
    private WgsCoordinate coordinate;
    private I18NString name;
    private StreetVertex vertex;
    private boolean carAccessible;
    private boolean walkAccessible;

    VehicleParkingEntranceBuilder() {}

    public VehicleParkingEntranceBuilder vehicleParking(VehicleParking vehicleParking) {
      this.vehicleParking = vehicleParking;
      return this;
    }

    public VehicleParkingEntranceBuilder entranceId(FeedScopedId entranceId) {
      this.entranceId = entranceId;
      return this;
    }

    public VehicleParkingEntranceBuilder coordinate(WgsCoordinate coordinate) {
      this.coordinate = coordinate;
      return this;
    }

    public VehicleParkingEntranceBuilder name(I18NString name) {
      this.name = name;
      return this;
    }

    public VehicleParkingEntranceBuilder vertex(StreetVertex vertex) {
      this.vertex = vertex;
      return this;
    }

    public VehicleParkingEntranceBuilder carAccessible(boolean carAccessible) {
      this.carAccessible = carAccessible;
      return this;
    }

    public VehicleParkingEntranceBuilder walkAccessible(boolean walkAccessible) {
      this.walkAccessible = walkAccessible;
      return this;
    }

    public VehicleParkingEntrance build() {
      return new VehicleParkingEntrance(
        vehicleParking,
        entranceId,
        coordinate,
        name,
        vertex,
        carAccessible,
        walkAccessible
      );
    }
  }
}
