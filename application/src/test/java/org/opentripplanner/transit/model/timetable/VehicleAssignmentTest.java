package org.opentripplanner.transit.model.timetable;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;

class VehicleAssignmentTest {

  private static final FeedScopedId PLANNED_VEHICLE = new FeedScopedId("F", "PlannedVehicle");
  private static final FeedScopedId REAL_TIME_VEHICLE = new FeedScopedId("F", "RealTimeVehicle");
  private static final String VEHICLE_TYPE = "F:VehicleType:1";

  @Test
  void isNullWhenNothingKnown() {
    assertNull(VehicleAssignment.ofNullable(null, null));
    assertNull(VehicleAssignment.ofPlannedAndRealTime(null, null));
  }

  @Test
  void realTimeVehicleTakesPrecedenceOverPlanned() {
    var planned = new VehicleAssignment(PLANNED_VEHICLE, VEHICLE_TYPE);

    assertThat(VehicleAssignment.ofPlannedAndRealTime(planned, REAL_TIME_VEHICLE)).isEqualTo(
      new VehicleAssignment(REAL_TIME_VEHICLE, VEHICLE_TYPE)
    );
  }

  @Test
  void plannedVehicleKeptWhenNoRealTimeUpdate() {
    var planned = new VehicleAssignment(PLANNED_VEHICLE, VEHICLE_TYPE);

    assertThat(VehicleAssignment.ofPlannedAndRealTime(planned, null)).isEqualTo(planned);
  }

  @Test
  void realTimeVehicleWithoutPlannedAssignment() {
    assertThat(VehicleAssignment.ofPlannedAndRealTime(null, REAL_TIME_VEHICLE)).isEqualTo(
      new VehicleAssignment(REAL_TIME_VEHICLE, null)
    );
  }

  @Test
  void vehicleTypeIsAlwaysThePlannedOne() {
    var planned = new VehicleAssignment(null, VEHICLE_TYPE);

    assertThat(VehicleAssignment.ofPlannedAndRealTime(planned, REAL_TIME_VEHICLE)).isEqualTo(
      new VehicleAssignment(REAL_TIME_VEHICLE, VEHICLE_TYPE)
    );
  }
}
