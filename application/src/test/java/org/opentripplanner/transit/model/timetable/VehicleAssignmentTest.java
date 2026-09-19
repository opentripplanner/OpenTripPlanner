package org.opentripplanner.transit.model.timetable;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;

class VehicleAssignmentTest {

  private static final FeedScopedId VEHICLE = new FeedScopedId("F", "Vehicle");
  private static final String VEHICLE_TYPE = "F:VehicleType:1";

  @Test
  void isNullWhenNothingKnown() {
    assertNull(VehicleAssignment.ofNullable(null, null));
  }

  @Test
  void keepsTheKnownReferences() {
    assertThat(VehicleAssignment.ofNullable(VEHICLE, null)).isEqualTo(
      new VehicleAssignment(VEHICLE, null)
    );
    assertThat(VehicleAssignment.ofNullable(null, VEHICLE_TYPE)).isEqualTo(
      new VehicleAssignment(null, VEHICLE_TYPE)
    );
    assertThat(VehicleAssignment.ofNullable(VEHICLE, VEHICLE_TYPE)).isEqualTo(
      new VehicleAssignment(VEHICLE, VEHICLE_TYPE)
    );
  }
}
