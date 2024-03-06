package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.framework.time.DurationUtils;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.DefaultAccessEgress;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.framework.TimeAndCostPenalty;
import org.opentripplanner.routing.api.request.framework.TimeAndCostPenaltyForEnum;
import org.opentripplanner.routing.api.request.framework.TimePenalty;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.TestStateBuilder;

class AccessEgressPenaltyDecoratorTest {

  private static final int DURATION_CAR_RENTAL = 45;
  private static final int DURATION_WALKING = 135;
  private static final Duration D10m = DurationUtils.duration("10m");
  private static final DefaultAccessEgress WALK = ofWalking(DURATION_WALKING);
  private static final DefaultAccessEgress CAR_RENTAL = ofCarRental(DURATION_CAR_RENTAL);
  private static final TimeAndCostPenalty PENALTY = new TimeAndCostPenalty(
    TimePenalty.of(D10m, 1.5),
    2.0
  );

  // We use the penalty to calculate the expected value, this is not pure, but the
  // TimeAndCostPenalty is unit-tested elsewhere.
  private static final DefaultAccessEgress EXP_WALK_W_PENALTY = WALK.withPenalty(
    PENALTY.calculate(DURATION_WALKING)
  );
  private static final DefaultAccessEgress EXP_CAR_RENTAL_W_PENALTY = CAR_RENTAL.withPenalty(
    PENALTY.calculate(DURATION_CAR_RENTAL)
  );

  @BeforeAll
  static void verifyTestSetup() {
    assertEquals("Walk 2m15s C₁238_035 w/penalty(13m23s $1606) ~ 1", EXP_WALK_W_PENALTY.toString());
    assertEquals(
      "Walk 45s C₁237_887 w/penalty(11m8s $1336) ~ 1",
      EXP_CAR_RENTAL_W_PENALTY.toString()
    );
  }

  private static List<Arguments> decorateCarRentalTestCase() {
    return List.of(
      Arguments.of(List.of(), List.of()),
      Arguments.of(List.of(WALK), List.of(WALK)),
      Arguments.of(List.of(EXP_CAR_RENTAL_W_PENALTY), List.of(CAR_RENTAL)),
      Arguments.of(List.of(WALK, EXP_CAR_RENTAL_W_PENALTY), List.of(WALK, CAR_RENTAL))
    );
  }

  @ParameterizedTest
  @MethodSource("decorateCarRentalTestCase")
  void decorateCarRentalTest(List<DefaultAccessEgress> expected, List<DefaultAccessEgress> input) {
    var subject = new AccessEgressPenaltyDecorator(
      StreetMode.CAR_RENTAL,
      StreetMode.WALK,
      TimeAndCostPenaltyForEnum.of(StreetMode.class).with(StreetMode.CAR_RENTAL, PENALTY).build()
    );

    // Only access is decorated, since egress mode is WALK
    assertEquals(expected, subject.decorateAccess(input));
    assertEquals(input, subject.decorateEgress(input));
  }

  private static List<Arguments> decorateWalkTestCase() {
    return List.of(
      Arguments.of(List.of(), List.of()),
      Arguments.of(List.of(EXP_WALK_W_PENALTY), List.of(WALK))
    );
  }

  @ParameterizedTest
  @MethodSource("decorateWalkTestCase")
  void decorateWalkTest(List<DefaultAccessEgress> expected, List<DefaultAccessEgress> input) {
    var subject = new AccessEgressPenaltyDecorator(
      StreetMode.CAR_RENTAL,
      StreetMode.WALK,
      TimeAndCostPenaltyForEnum.of(StreetMode.class).with(StreetMode.WALK, PENALTY).build()
    );

    // Only egress is decorated, since access mode is not WALKING (but CAR_RENTAL)
    assertEquals(expected, subject.decorateAccess(input));
    assertEquals(expected, subject.decorateEgress(input));
  }

  @Test
  void doNotDecorateAnyIfNoPenaltyIsSet() {
    // Set penalty on BIKE, should not have an effect on the decoration
    var input = List.of(WALK, CAR_RENTAL);

    var subject = new AccessEgressPenaltyDecorator(
      StreetMode.CAR_RENTAL,
      StreetMode.WALK,
      TimeAndCostPenaltyForEnum.of(StreetMode.class).with(StreetMode.BIKE, PENALTY).build()
    );

    assertSame(input, subject.decorateAccess(input));
    assertSame(input, subject.decorateEgress(input));
  }

  @Test
  void filterEgress() {}

  private static DefaultAccessEgress ofCarRental(int duration) {
    return ofAccessEgress(
      duration,
      TestStateBuilder.ofCarRental().streetEdge().pickUpCarFromStation().build()
    );
  }

  private static DefaultAccessEgress ofWalking(int durationInSeconds) {
    return ofAccessEgress(durationInSeconds, TestStateBuilder.ofWalking().streetEdge().build());
  }

  private static DefaultAccessEgress ofAccessEgress(int duration, State state) {
    // We do NOT need to override #withPenalty(...), because all fields including
    // 'durationInSeconds' is copied over using the getters.

    return new DefaultAccessEgress(1, state) {
      @Override
      public int durationInSeconds() {
        return duration;
      }
    };
  }
}
