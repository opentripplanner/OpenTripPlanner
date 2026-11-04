package org.opentripplanner.routing.algorithm.mapping;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.routing.algorithm.mapping.StatesToWalkStepsMapper.isOnSameStreet;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.model.plan.walkstep.WalkStep;
import org.opentripplanner.model.plan.walkstep.WalkStepBuilder;
import org.opentripplanner.service.streetdetails.internal.DefaultStreetDetailsRepository;
import org.opentripplanner.service.streetdetails.internal.DefaultStreetDetailsService;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.path.StreetPath;
import org.opentripplanner.street.search.state.TestStateBuilder;
import org.opentripplanner.transit.model.site.Entrance;

class StatesToWalkStepsMapperTest {

  @Test
  void absoluteDirection() {
    var states = TestStateBuilder.ofWalking().streetEdge().streetEdge();

    assertThat(toStrings(states))
      .containsExactly(
        "WalkStep{absoluteDirection: NORTHEAST, relativeDirection: DEPART, streetName: '1.0_1.0_2.0_2.0', distance: 157,225.68}",
        "WalkStep{absoluteDirection: NORTHEAST, relativeDirection: CONTINUE, streetName: '2.0_2.0_3.0_3.0', distance: 157,177.8}"
      )
      .inOrder();
  }

  @Test
  void elevator() {
    var states = TestStateBuilder.ofWalking().streetEdge().elevator().streetEdge();

    assertThat(toStrings(states))
      .containsExactly(
        "WalkStep{absoluteDirection: NORTHEAST, relativeDirection: DEPART, streetName: '1.0_1.0_2.0_2.0', distance: 157,225.68}",
        "WalkStep{absoluteDirection: NORTHEAST, relativeDirection: CONTINUE, streetName: '2.0_2.0_3.0_3.0', distance: 157,177.8}",
        "WalkStep{absoluteDirection: SOUTH, relativeDirection: HARD_RIGHT, streetName: 'ElevatorBoardEdge', distance: 0.0}",
        "WalkStep{relativeDirection: ELEVATOR, streetName: 'elevator', distance: 0.0}",
        "WalkStep{absoluteDirection: NORTHEAST, relativeDirection: HARD_LEFT, streetName: '3.0_3.0_4.0_4.0', distance: 157,106.02}"
      )
      .inOrder();
  }

  @Test
  void stairs() {
    var states = TestStateBuilder.ofWalking().streetEdge().stairsEdge().streetEdge();

    assertThat(toStrings(states))
      .containsExactly(
        "WalkStep{absoluteDirection: NORTHEAST, relativeDirection: DEPART, streetName: '1.0_1.0_2.0_2.0', distance: 157,225.68}",
        "WalkStep{absoluteDirection: NORTHEAST, relativeDirection: CONTINUE, streetName: '2.0_2.0_3.0_3.0', distance: 30.0}",
        "WalkStep{absoluteDirection: NORTHEAST, relativeDirection: CONTINUE, streetName: '3.0_3.0_4.0_4.0', distance: 157,106.02}"
      )
      .inOrder();
  }

  @Test
  void escalator() {
    var states = TestStateBuilder.ofWalking().streetEdge().escalatorEdge().streetEdge();

    assertThat(toStrings(states))
      .containsExactly(
        "WalkStep{absoluteDirection: NORTHEAST, relativeDirection: DEPART, streetName: '1.0_1.0_2.0_2.0', distance: 157,225.68}",
        "WalkStep{absoluteDirection: NORTHEAST, relativeDirection: CONTINUE, streetName: 'escalator', distance: 30.0}",
        "WalkStep{absoluteDirection: NORTHEAST, relativeDirection: CONTINUE, streetName: '3.0_3.0_4.0_4.0', distance: 157,106.02}"
      )
      .inOrder();
  }

  @Test
  void stationEntrance() {
    var states = TestStateBuilder.ofWalking()
      .streetEdge("name", 1)
      .entrance("name")
      .streetEdge()
      .areaEdge("name", 10);

    assertThat(toStrings(states))
      .containsExactly(
        "WalkStep{absoluteDirection: NORTHEAST, relativeDirection: DEPART, streetName: 'name', distance: 31.0}",
        "WalkStep{relativeDirection: ENTER_OR_EXIT_STATION, streetName: 'station entrance', distance: 0.0}",
        "WalkStep{absoluteDirection: NORTHEAST, relativeDirection: CONTINUE, streetName: 'osm:node:12345_4.0_4.0', distance: 157,106.02}",
        "WalkStep{absoluteDirection: NORTHEAST, relativeDirection: CONTINUE, streetName: 'name', distance: 157,010.41}"
      )
      .inOrder();
  }

  @Test
  void enterStation() {
    var states = TestStateBuilder.ofWalking().streetEdge().enterStation("Lichterfelde-Ost");

    assertThat(toStrings(states))
      .containsExactly(
        "WalkStep{absoluteDirection: NORTHEAST, relativeDirection: DEPART, streetName: '1.0_1.0_2.0_2.0', distance: 157,225.68}",
        "WalkStep{absoluteDirection: NORTHEAST, relativeDirection: ENTER_STATION, streetName: 'Lichterfelde-Ost', distance: 0.0}"
      )
      .inOrder();
  }

  @Test
  void exitStation() {
    var states = TestStateBuilder.ofWalking().streetEdge().exitStation("Lichterfelde-Ost");

    assertThat(toStrings(states))
      .containsExactly(
        "WalkStep{absoluteDirection: NORTHEAST, relativeDirection: DEPART, streetName: '1.0_1.0_2.0_2.0', distance: 157,225.68}",
        "WalkStep{absoluteDirection: NORTHEAST, relativeDirection: CONTINUE, streetName: 'pathway', distance: 0.0}",
        "WalkStep{absoluteDirection: NORTHEAST, relativeDirection: EXIT_STATION, streetName: 'Lichterfelde-Ost', distance: 0.0}"
      )
      .inOrder();
  }

  @Test
  void exitStationWithEntranceAsLastStep() {
    var states = TestStateBuilder.ofWalking().streetEdge("name", 1).entrance("name");

    assertThat(toStrings(states))
      .containsExactly(
        "WalkStep{absoluteDirection: NORTHEAST, relativeDirection: DEPART, streetName: 'name', distance: 31.0}",
        "WalkStep{relativeDirection: ENTER_OR_EXIT_STATION, streetName: 'station entrance', distance: 0.0}"
      )
      .inOrder();
  }

  @Test
  void escalatorAndEntrance() {
    var states = TestStateBuilder.ofWalking()
      .streetEdge("name", 1)
      .escalatorEdgeAndStationEntrance();

    assertThat(toStrings(states))
      .containsExactly(
        "WalkStep{absoluteDirection: NORTHEAST, relativeDirection: DEPART, streetName: 'name', distance: 1.0}",
        "WalkStep{absoluteDirection: NORTHEAST, relativeDirection: CONTINUE, streetName: 'escalator', distance: 30.0}",
        "WalkStep{relativeDirection: ENTER_OR_EXIT_STATION, streetName: 'station entrance', distance: 0.0}"
      )
      .inOrder();
  }

  @Test
  void signpostedPathway() {
    var states = TestStateBuilder.ofWalking().streetEdge().pathway("follow signs to platform 1");

    assertThat(toStrings(states))
      .containsExactly(
        "WalkStep{absoluteDirection: NORTHEAST, relativeDirection: DEPART, streetName: '1.0_1.0_2.0_2.0', distance: 157,225.68}",
        "WalkStep{absoluteDirection: NORTHEAST, relativeDirection: FOLLOW_SIGNS, streetName: 'follow signs to platform 1', distance: 100.0}"
      )
      .inOrder();
  }

  private static List<WalkStep> buildWalkSteps(TestStateBuilder builder) {
    var result = builder.build();
    var path = new StreetPath(result);
    var mapper = new StatesToWalkStepsMapper(
      path.states(),
      null,
      new DefaultStreetDetailsService(new DefaultStreetDetailsRepository()),
      id -> Entrance.of(id).withCoordinate(WgsCoordinate.GREENWICH).build(),
      0
    );
    return mapper.generateWalkSteps();
  }

  private static List<String> toStrings(TestStateBuilder builder) {
    return buildWalkSteps(builder).stream().map(WalkStep::toString).toList();
  }

  @ParameterizedTest
  @MethodSource("createIsOnSameStreetCases")
  void testIsOnSameStreet(List<String> streets, boolean expected, String message) {
    List<WalkStepBuilder> steps = streets
      .stream()
      .map(s ->
        s != null
          ? WalkStep.builder()
              .withCrossing(s.startsWith("crossing over ") || s.equals("derived name"))
              .withNameIsDerived(s.equals("derived name"))
              .withDirectionText(I18NString.of(s))
          : WalkStep.builder()
      )
      .toList();

    int lastIndex = steps.size() - 1;
    WalkStepBuilder threeBack = steps.get(lastIndex - 2);
    WalkStepBuilder twoBack = steps.get(lastIndex - 1);
    WalkStepBuilder lastStep = steps.get(lastIndex);

    assertEquals(expected, isOnSameStreet(lastStep, twoBack, threeBack), message);
  }

  static Stream<Arguments> createIsOnSameStreetCases() {
    return Stream.of(
      Arguments.of(
        List.of("Street1", "Street2", "Street3"),
        false,
        "Three different streets in a row are not the same street."
      ),
      Arguments.of(
        List.of("Street1", "Street2", "Street1"),
        true,
        "A street interrupted by another street is the same street."
      ),
      Arguments.of(
        List.of("Street1", "crossing over Street2", "Street1"),
        false,
        "A crossing is treated as not the same street."
      ),
      Arguments.of(
        List.of("crossing over turn lane", "Street1", "crossing over turn lane"),
        false,
        "Multiple crossings are not the same street."
      ),
      Arguments.of(
        List.of("Street1", "derived name", "Street1"),
        true,
        "A street interrupted by a crossing with a derived name (using a default namer) is the same street."
      )
    );
  }
}
