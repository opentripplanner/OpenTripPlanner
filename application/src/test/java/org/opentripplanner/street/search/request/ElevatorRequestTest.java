package org.opentripplanner.street.search.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.opentripplanner.street.search.request.ImmutableRequestAsserts.assertEqualsAndHashCode;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class ElevatorRequestTest {

  public static final int BOARD_COST = 100;
  public static final Duration BOARD_SLACK = Duration.ofSeconds(60);
  public static final Duration HOP_TIME = Duration.ofSeconds(120);
  public static final double RELUCTANCE = 2.5;
  private final ElevatorRequest subject = ElevatorRequest.of()
    .withBoardCost(BOARD_COST)
    .withBoardSlack(BOARD_SLACK)
    .withHopTime(HOP_TIME)
    .withReluctance(RELUCTANCE)
    .build();

  @Test
  void boardCost() {
    assertEquals(BOARD_COST, subject.boardCost());
  }

  @Test
  void boardSlack() {
    assertEquals(BOARD_SLACK, subject.boardSlack());
  }

  @Test
  void hopTime() {
    assertEquals(HOP_TIME, subject.hopTime());
  }

  @Test
  void reluctance() {
    assertEquals(RELUCTANCE, subject.reluctance());
  }

  @Test
  void testEqualsAndHashCode() {
    // Return same object if no value is set
    assertSame(ElevatorRequest.DEFAULT, ElevatorRequest.of().build());
    assertSame(subject, subject.copyOf().build());

    // Create a copy, make a change and set it back again to force creating a new object
    var other = subject.copyOf().withBoardSlack(Duration.ofSeconds(123)).build();
    var same = other.copyOf().withBoardSlack(BOARD_SLACK).build();
    assertEqualsAndHashCode(subject, other, same);
  }

  @Test
  void testToString() {
    assertEquals("ElevatorRequest{}", ElevatorRequest.DEFAULT.toString());
    assertEquals(
      "ElevatorRequest{boardCost: $100, boardSlack: 1m, hopTime: 2m, reluctance: 2.5}",
      subject.toString()
    );
  }
}
