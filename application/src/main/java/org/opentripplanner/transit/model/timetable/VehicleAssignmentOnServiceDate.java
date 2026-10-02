package org.opentripplanner.transit.model.timetable;

import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * The vehicle assignment of a trip on a service date: the scheduled (planned) references, and the
 * vehicle reported by real-time data to operate it.
 */
public class VehicleAssignmentOnServiceDate {

  @Nullable
  private final FeedScopedId scheduledVehicleId;

  @Nullable
  private final String scheduledVehicleTypeId;

  @Nullable
  private final FeedScopedId realtimeVehicleId;

  public VehicleAssignmentOnServiceDate(
    @Nullable FeedScopedId scheduledVehicleId,
    @Nullable String scheduledVehicleTypeId,
    @Nullable FeedScopedId realtimeVehicleId
  ) {
    this.scheduledVehicleId = scheduledVehicleId;
    this.scheduledVehicleTypeId = scheduledVehicleTypeId;
    this.realtimeVehicleId = realtimeVehicleId;
  }

  /**
   * Combine the scheduled vehicle assignment with a vehicle id reported by real-time data. The
   * real-time vehicle id falls back to the scheduled one when no vehicle has been reported.
   * Returns {@code null} when no reference is known.
   */
  @Nullable
  public static VehicleAssignmentOnServiceDate of(
    @Nullable VehicleAssignment scheduled,
    @Nullable FeedScopedId reportedVehicleId
  ) {
    var scheduledVehicleId = scheduled == null ? null : scheduled.vehicleId();
    var scheduledVehicleTypeId = scheduled == null ? null : scheduled.vehicleTypeId();
    var realtimeVehicleId = reportedVehicleId != null ? reportedVehicleId : scheduledVehicleId;
    if (scheduledVehicleId == null && scheduledVehicleTypeId == null && realtimeVehicleId == null) {
      return null;
    }
    return new VehicleAssignmentOnServiceDate(
      scheduledVehicleId,
      scheduledVehicleTypeId,
      realtimeVehicleId
    );
  }

  /** The vehicle scheduled to operate the trip on the service date, from the planned data. */
  @Nullable
  public FeedScopedId scheduledVehicleId() {
    return scheduledVehicleId;
  }

  /** The type of vehicle scheduled to operate the trip on the service date. */
  @Nullable
  public String scheduledVehicleTypeId() {
    return scheduledVehicleTypeId;
  }

  /**
   * The vehicle reported by real-time data to operate the trip on the service date, falling back
   * to the scheduled vehicle when none has been reported.
   */
  @Nullable
  public FeedScopedId realtimeVehicleId() {
    return realtimeVehicleId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof VehicleAssignmentOnServiceDate other)) {
      return false;
    }
    return (
      Objects.equals(scheduledVehicleId, other.scheduledVehicleId) &&
      Objects.equals(scheduledVehicleTypeId, other.scheduledVehicleTypeId) &&
      Objects.equals(realtimeVehicleId, other.realtimeVehicleId)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(scheduledVehicleId, scheduledVehicleTypeId, realtimeVehicleId);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(VehicleAssignmentOnServiceDate.class)
      .addObj("scheduledVehicleId", scheduledVehicleId)
      .addStr("scheduledVehicleTypeId", scheduledVehicleTypeId)
      .addObj("realtimeVehicleId", realtimeVehicleId)
      .toString();
  }
}
