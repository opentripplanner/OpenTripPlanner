package org.opentripplanner.transit.model.timetable;

import java.io.Serializable;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * References to the vehicle and vehicle type aimed to operate a trip, as given in the planned
 * data.
 */
public class VehicleAssignment implements Serializable {

  @Nullable
  private final FeedScopedId vehicleId;

  @Nullable
  private final String vehicleTypeId;

  public VehicleAssignment(@Nullable FeedScopedId vehicleId, @Nullable String vehicleTypeId) {
    this.vehicleId = vehicleId;
    this.vehicleTypeId = vehicleTypeId;
  }

  /**
   * Return the vehicle assignment for the given references, or {@code null} when neither a vehicle
   * nor a vehicle type is known.
   */
  @Nullable
  public static VehicleAssignment ofNullable(
    @Nullable FeedScopedId vehicleId,
    @Nullable String vehicleTypeId
  ) {
    return vehicleId == null && vehicleTypeId == null
      ? null
      : new VehicleAssignment(vehicleId, vehicleTypeId);
  }

  /** The vehicle itself. */
  @Nullable
  public FeedScopedId vehicleId() {
    return vehicleId;
  }

  /** The type of vehicle. */
  @Nullable
  public String vehicleTypeId() {
    return vehicleTypeId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof VehicleAssignment other)) {
      return false;
    }
    return (
      Objects.equals(vehicleId, other.vehicleId) &&
      Objects.equals(vehicleTypeId, other.vehicleTypeId)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(vehicleId, vehicleTypeId);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(VehicleAssignment.class)
      .addObj("vehicleId", vehicleId)
      .addStr("vehicleTypeId", vehicleTypeId)
      .toString();
  }
}
