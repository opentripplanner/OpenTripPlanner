package org.opentripplanner.routing.api.request.preference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.routing.api.request.preference.ImmutablePreferencesAsserts.assertEqualsAndHashCode;

import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.core.intersection_model.DrivingDirection;
import org.opentripplanner.routing.core.intersection_model.IntersectionTraversalModel;

class StreetPreferencesTest {

  private static final double TURN_RELUCTANCE = 2.0;
  private static final DrivingDirection DRIVING_DIRECTION = DrivingDirection.LEFT;
  private static final int ELEVATOR_BOARD_TIME = (int) Duration.ofMinutes(2).toSeconds();
  private static final IntersectionTraversalModel INTERSECTION_TRAVERSAL_MODEL =
    IntersectionTraversalModel.NORWAY;
  private static final Duration MAX_ACCESS_EGRESS = Duration.ofMinutes(5);
  private static final Duration MAX_DIRECT = Duration.ofMinutes(10);

  private final StreetPreferences subject = new StreetPreferences();

  {
    subject.setTurnReluctance(TURN_RELUCTANCE);
    subject.setDrivingDirection(DRIVING_DIRECTION);
    subject.withElevator(it -> it.withBoardTime(ELEVATOR_BOARD_TIME));
    subject.setIntersectionTraversalModel(INTERSECTION_TRAVERSAL_MODEL);
    subject.initMaxAccessEgressDuration(MAX_ACCESS_EGRESS, Map.of());
    subject.initMaxDirectDuration(MAX_DIRECT, Map.of());
  }

  @Test
  void elevator() {
    assertEquals(ELEVATOR_BOARD_TIME, subject.elevator().boardTime());
  }

  @Test
  void intersectionTraversalModel() {
    assertEquals(INTERSECTION_TRAVERSAL_MODEL, subject.intersectionTraversalModel());
  }

  @Test
  void maxAccessEgressDuration() {
    assertEquals(MAX_ACCESS_EGRESS, subject.maxAccessEgressDuration().defaultValue());
  }

  @Test
  void maxDirectDuration() {
    assertEquals(MAX_DIRECT, subject.maxDirectDuration().defaultValue());
  }

  @Test
  void drivingDirection() {
    assertEquals(DRIVING_DIRECTION, subject.drivingDirection());
  }

  @Test
  void turnReluctance() {
    assertEquals(TURN_RELUCTANCE, subject.turnReluctance());
  }

  /*
  @Test
  void testOfAndCopyOf() {
    // Return same object if no value is set
    assertSame(StreetPreferences.DEFAULT, StreetPreferences.of().build());
    assertSame(subject, subject.copyOf().build());
  }
  */

  @Test
  void testEqualsAndHashCode() {
    // Create a copy, make a change and set it back again to force creating a new object
    var other = subject.clone();
    other.setTurnReluctance(34.0);
    var copy = other.clone();
    copy.setTurnReluctance(TURN_RELUCTANCE);
    assertEqualsAndHashCode(StreetPreferences.DEFAULT, subject, other, copy);
  }

  @Test
  void testToString() {
    assertEquals("StreetPreferences{}", StreetPreferences.DEFAULT.toString());
    assertEquals(
      "StreetPreferences{" +
      "drivingDirection: LEFT, " +
      "turnReluctance: 2.0, " +
      "elevator: ElevatorPreferences{boardTime: 2m}, " +
      "intersectionTraversalModel: NORWAY, " +
      "maxAccessEgressDuration: DurationForStreetMode{default:5m}, " +
      "maxDirectDuration: DurationForStreetMode{default:10m}" +
      "}",
      subject.toString()
    );
  }
}
