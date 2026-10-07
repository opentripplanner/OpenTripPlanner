package org.opentripplanner.street.model.path.step;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.street.model.path.step.StatesToStreetStepsMapper.isOnSameStreet;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.street.model.path.StreetPath;
import org.opentripplanner.street.search.state.TestStateBuilder;

class StatesToStreetStepsMapperTest {

  @Test
  void absoluteDirection() {
    var states = TestStateBuilder.ofWalking().streetEdge().streetEdge();

    assertThat(toStrings(states))
      .containsExactly(
        "StreetStep{absoluteDirection: NORTHEAST, relativeDirection: DEPART, directionText: '1.0_1.0_2.0_2.0', distance: 100.0}",
        "StreetStep{absoluteDirection: NORTHEAST, relativeDirection: CONTINUE, directionText: '2.0_2.0_3.0_3.0', distance: 100.0}"
      )
      .inOrder();
  }

  @Test
  void elevator() {
    var states = TestStateBuilder.ofWalking().streetEdge().elevator().streetEdge();

    assertThat(toStrings(states))
      .containsExactly(
        "StreetStep{absoluteDirection: NORTHEAST, relativeDirection: DEPART, directionText: '1.0_1.0_2.0_2.0', distance: 100.0}",
        "StreetStep{absoluteDirection: NORTHEAST, relativeDirection: CONTINUE, directionText: '2.0_2.0_3.0_3.0', distance: 157,177.8}",
        "StreetStep{absoluteDirection: SOUTH, relativeDirection: HARD_RIGHT, directionText: 'ElevatorBoardEdge', distance: 0.0}",
        "StreetStep{relativeDirection: ELEVATOR, directionText: 'elevator', distance: 0.0, verticalTransportation: 'Elevator'}",
        "StreetStep{absoluteDirection: NORTHEAST, relativeDirection: HARD_LEFT, directionText: '3.0_3.0_4.0_4.0', distance: 100.0}"
      )
      .inOrder();
  }

  @Test
  void stairs() {
    var states = TestStateBuilder.ofWalking().streetEdge().stairsEdge().streetEdge();

    assertThat(toStrings(states))
      .containsExactly(
        "StreetStep{absoluteDirection: NORTHEAST, relativeDirection: DEPART, directionText: '1.0_1.0_2.0_2.0', distance: 100.0}",
        "StreetStep{absoluteDirection: NORTHEAST, relativeDirection: CONTINUE, directionText: '2.0_2.0_3.0_3.0', distance: 100.0, verticalTransportation: 'Stairs'}",
        "StreetStep{absoluteDirection: NORTHEAST, relativeDirection: CONTINUE, directionText: '3.0_3.0_4.0_4.0', distance: 100.0}"
      )
      .inOrder();
  }

  @Test
  void escalator() {
    var states = TestStateBuilder.ofWalking().streetEdge().escalatorEdge().streetEdge();

    assertThat(toStrings(states))
      .containsExactly(
        "StreetStep{absoluteDirection: NORTHEAST, relativeDirection: DEPART, directionText: '1.0_1.0_2.0_2.0', distance: 100.0}",
        "StreetStep{absoluteDirection: NORTHEAST, relativeDirection: CONTINUE, directionText: 'escalator', distance: 30.0, verticalTransportation: 'Escalator'}",
        "StreetStep{absoluteDirection: NORTHEAST, relativeDirection: CONTINUE, directionText: '3.0_3.0_4.0_4.0', distance: 100.0}"
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
        "StreetStep{absoluteDirection: NORTHEAST, relativeDirection: DEPART, directionText: 'name', distance: 31.0}",
        "StreetStep{relativeDirection: ENTER_OR_EXIT_STATION, directionText: 'station entrance', distance: 0.0, entranceId: osm:12345}",
        "StreetStep{absoluteDirection: NORTHEAST, relativeDirection: CONTINUE, directionText: 'osm:node:12345_4.0_4.0', distance: 100.0}",
        "StreetStep{absoluteDirection: NORTHEAST, relativeDirection: CONTINUE, directionText: 'name', distance: 157,010.4}"
      )
      .inOrder();
  }

  @Test
  void enterStation() {
    var states = TestStateBuilder.ofWalking().streetEdge().enterStation("Lichterfelde-Ost");

    assertThat(toStrings(states))
      .containsExactly(
        "StreetStep{absoluteDirection: NORTHEAST, relativeDirection: DEPART, directionText: '1.0_1.0_2.0_2.0', distance: 100.0}",
        "StreetStep{absoluteDirection: NORTHEAST, relativeDirection: ENTER_STATION, directionText: 'Lichterfelde-Ost', distance: 0.0, entranceId: street:Lichterfelde-Ost}"
      )
      .inOrder();
  }

  @Test
  void exitStation() {
    var states = TestStateBuilder.ofWalking().streetEdge().exitStation("Lichterfelde-Ost");

    assertThat(toStrings(states))
      .containsExactly(
        "StreetStep{absoluteDirection: NORTHEAST, relativeDirection: DEPART, directionText: '1.0_1.0_2.0_2.0', distance: 100.0}",
        "StreetStep{absoluteDirection: NORTHEAST, relativeDirection: CONTINUE, directionText: 'pathway', distance: 0.0}",
        "StreetStep{absoluteDirection: NORTHEAST, relativeDirection: EXIT_STATION, directionText: 'Lichterfelde-Ost', distance: 0.0, entranceId: street:Lichterfelde-Ost}"
      )
      .inOrder();
  }

  @Test
  void exitStationWithEntranceAsLastStep() {
    var states = TestStateBuilder.ofWalking().streetEdge("name", 1).entrance("name");

    assertThat(toStrings(states))
      .containsExactly(
        "StreetStep{absoluteDirection: NORTHEAST, relativeDirection: DEPART, directionText: 'name', distance: 31.0}",
        "StreetStep{relativeDirection: ENTER_OR_EXIT_STATION, directionText: 'station entrance', distance: 0.0, entranceId: osm:12345}"
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
        "StreetStep{absoluteDirection: NORTHEAST, relativeDirection: DEPART, directionText: 'name', distance: 1.0}",
        "StreetStep{absoluteDirection: NORTHEAST, relativeDirection: CONTINUE, directionText: 'escalator', distance: 30.0, verticalTransportation: 'Escalator'}",
        "StreetStep{relativeDirection: ENTER_OR_EXIT_STATION, directionText: 'station entrance', distance: 0.0, entranceId: osm:12345}"
      )
      .inOrder();
  }

  @Test
  void signpostedPathway() {
    var states = TestStateBuilder.ofWalking().streetEdge().pathway("follow signs to platform 1");

    assertThat(toStrings(states))
      .containsExactly(
        "StreetStep{absoluteDirection: NORTHEAST, relativeDirection: DEPART, directionText: '1.0_1.0_2.0_2.0', distance: 100.0}",
        "StreetStep{absoluteDirection: NORTHEAST, relativeDirection: FOLLOW_SIGNS, directionText: 'follow signs to platform 1', distance: 100.0}"
      )
      .inOrder();
  }

  private static List<StreetStep> buildWalkSteps(TestStateBuilder builder) {
    var result = builder.build();
    var path = new StreetPath(result);
    var mapper = new StatesToStreetStepsMapper(path.states(), null, 0);
    return mapper.generateSteps();
  }

  private static List<String> toStrings(TestStateBuilder builder) {
    return buildWalkSteps(builder).stream().map(StreetStep::toString).toList();
  }

  @ParameterizedTest
  @MethodSource("createIsOnSameStreetCases")
  void testIsOnSameStreet(List<String> streets, boolean expected, String message) {
    List<StreetStepBuilder> steps = streets
      .stream()
      .map(s ->
        s != null
          ? StreetStep.builder()
              .withCrossing(s.startsWith("crossing over ") || s.equals("derived name"))
              .withNameIsDerived(s.equals("derived name"))
              .withDirectionText(I18NString.of(s))
          : StreetStep.builder()
      )
      .toList();

    int lastIndex = steps.size() - 1;
    StreetStepBuilder threeBack = steps.get(lastIndex - 2);
    StreetStepBuilder twoBack = steps.get(lastIndex - 1);
    StreetStepBuilder lastStep = steps.get(lastIndex);

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
