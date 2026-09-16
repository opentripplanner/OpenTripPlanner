package org.opentripplanner.transit.model.timetable;

import java.io.Serializable;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * References to the vehicle assigned to operate a journey.
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

  /**
   * Combine the planned vehicle assignment with a vehicle id from a real-time update. The real-time
   * vehicle takes precedence over the planned one.
   * Returns {@code null} when neither a vehicle nor a vehicle type is known.
   */
  @Nullable
  public static VehicleAssignment ofPlannedAndRealTime(
    @Nullable VehicleAssignment planned,
    @Nullable FeedScopedId realTimeVehicleId
  ) {
    var vehicleId =
      realTimeVehicleId != null ? realTimeVehicleId : planned == null ? null : planned.vehicleId();
    var vehicleTypeId = planned == null ? null : planned.vehicleTypeId();
    return ofNullable(vehicleId, vehicleTypeId);
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
