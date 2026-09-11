package org.opentripplanner.transit.model.timetable;

import java.io.Serializable;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;

/**
 * References to the vehicle expected to operate a journey, and to the type of that
 * vehicle. Both references are independent, and either may be present without the other.
 *
 * @param vehicleId     the vehicle itself.
 * @param vehicleTypeId the type of vehicle.
 */
public record VehicleAssignment(
  @Nullable FeedScopedId vehicleId,
  @Nullable String vehicleTypeId
) implements Serializable {
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
   * Combine the planned vehicle assignment with the vehicle from a real-time update. The real-time
   * vehicle takes precedence over the planned one; the vehicle type is always the planned one.
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
}
