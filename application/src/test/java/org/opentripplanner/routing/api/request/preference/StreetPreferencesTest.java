package org.opentripplanner.routing.api.request.preference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.opentripplanner.routing.api.request.preference.ImmutablePreferencesAsserts.assertEqualsAndHashCode;

import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.street.search.intersection_model.DrivingDirection;
import org.opentripplanner.street.search.intersection_model.IntersectionTraversalModel;

class StreetPreferencesTest {

  private static final double TURN_RELUCTANCE = 2.0;
  private static final Duration MAX_ACCESS_EGRESS = Duration.ofMinutes(5);
  private static final Duration MAX_DIRECT = Duration.ofMinutes(10);
  private static final Duration ROUTING_TIMEOUT = Duration.ofSeconds(3);
  private static final DrivingDirection DRIVING_DIRECTION = DrivingDirection.LEFT;
  private static final int ELEVATOR_BOARD_TIME = (int) Duration.ofMinutes(2).toSeconds();
  private static final IntersectionTraversalModel INTERSECTION_TRAVERSAL_MODEL =
    IntersectionTraversalModel.CONSTANT;

  private final StreetPreferences subject = StreetPreferences.of()
    .withDrivingDirection(DRIVING_DIRECTION)
    .withTurnReluctance(TURN_RELUCTANCE)
    .withElevator(it -> it.withBoardTime(ELEVATOR_BOARD_TIME))
    .withIntersectionTraversalModel(INTERSECTION_TRAVERSAL_MODEL)
    .withAccessEgress(it -> it.withMaxDuration(MAX_ACCESS_EGRESS, Map.of()))
    .withMaxDirectDuration(MAX_DIRECT, Map.of())
    .withRoutingTimeout(ROUTING_TIMEOUT)
    .build();

  @Test
  void elevator() {
    assertEquals(ELEVATOR_BOARD_TIME, subject.elevator().boardTime());
  }

  @Test
  void intersectionTraversalModel() {
    assertEquals(INTERSECTION_TRAVERSAL_MODEL, subject.intersectionTraversalModel());
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

  @Test
  void routingTimeout() {
    assertEquals(ROUTING_TIMEOUT, subject.routingTimeout());
  }

  @Test
  void testOfAndCopyOf() {
    // Return same object if no value is set
    assertSame(StreetPreferences.DEFAULT, StreetPreferences.of().build());
    assertSame(subject, subject.copyOf().build());
  }

  @Test
  void testEqualsAndHashCode() {
    // Create a copy, make a change and set it back again to force creating a new object
    var other = subject.copyOf().withTurnReluctance(34.0).build();
    var copy = other.copyOf().withTurnReluctance(TURN_RELUCTANCE).build();
    assertEqualsAndHashCode(subject, other, copy);
  }

  @Test
  void testToString() {
    assertEquals("StreetPreferences{}", StreetPreferences.DEFAULT.toString());
    assertEquals(
      "StreetPreferences{" +
      "turnReluctance: 2.0, " +
      "drivingDirection: LEFT, " +
      "routingTimeout: 3s, " +
      "elevator: ElevatorPreferences{boardTime: 2m}, " +
      "intersectionTraversalModel: CONSTANT, " +
      "accessEgress: AccessEgressPreferences{" +
      "maxDuration: DurationForStreetMode{default:5m}" +
      "}, " +
      "maxDirectDuration: DurationForStreetMode{default:10m}" +
      "}",
      subject.toString()
    );
  }
}
