package org.opentripplanner.street.model.path.step;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.street.model.path.step.RelativeDirection.ENTER_STATION;
import static org.opentripplanner.street.model.path.step.RelativeDirection.EXIT_STATION;
import static org.opentripplanner.street.model.path.step.RelativeDirection.FOLLOW_SIGNS;
import static org.opentripplanner.street.model.path.step.StatesToStreetStepsMapper.isOnSameStreet;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.street.model.path.StreetPath;
import org.opentripplanner.street.search.state.TestStateBuilder;

class StatesToStreetStepsMapperTest {

  private static final FeedScopedId ENTRANCE_ID = new FeedScopedId("street", "Lichterfelde-Ost");

  @Test
  void absoluteDirection() {
    var walkSteps = buildWalkSteps(TestStateBuilder.ofWalking().streetEdge().streetEdge());
    assertEquals(2, walkSteps.size());
    walkSteps.forEach(step -> assertTrue(step.absoluteDirection() != null));
  }

  @Test
  void elevator() {
    var walkSteps = buildWalkSteps(
      TestStateBuilder.ofWalking().streetEdge().elevator().streetEdge()
    );
    var elevatorStep = walkSteps.get(3);
    assertEquals(RelativeDirection.ELEVATOR, elevatorStep.relativeDirection());
    assertInstanceOf(VerticalTransportation.Elevator.class, elevatorStep.verticalTransportation());
    assertTrue(elevatorStep.absoluteDirection() == null);
  }

  @Test
  void stairs() {
    var walkSteps = buildWalkSteps(
      TestStateBuilder.ofWalking().streetEdge().stairsEdge().streetEdge()
    );
    assertEquals(RelativeDirection.DEPART, walkSteps.get(0).relativeDirection());
    assertInstanceOf(
      VerticalTransportation.Stairs.class,
      walkSteps.get(1).verticalTransportation()
    );
    assertEquals(RelativeDirection.CONTINUE, walkSteps.get(2).relativeDirection());
  }

  @Test
  void escalator() {
    var walkSteps = buildWalkSteps(
      TestStateBuilder.ofWalking().streetEdge().escalatorEdge().streetEdge()
    );
    assertEquals(RelativeDirection.DEPART, walkSteps.get(0).relativeDirection());
    assertInstanceOf(
      VerticalTransportation.Escalator.class,
      walkSteps.get(1).verticalTransportation()
    );
    assertEquals(RelativeDirection.CONTINUE, walkSteps.get(2).relativeDirection());
  }

  @Test
  void stationEntrance() {
    var walkSteps = buildWalkSteps(
      TestStateBuilder.ofWalking()
        .streetEdge("name", 1)
        .entrance("name")
        .streetEdge()
        .areaEdge("name", 10)
    );
    assertEquals(4, walkSteps.size());
    assertEquals(RelativeDirection.DEPART, walkSteps.get(0).relativeDirection());
    assertEquals(RelativeDirection.ENTER_OR_EXIT_STATION, walkSteps.get(1).relativeDirection());
    assertEquals(RelativeDirection.CONTINUE, walkSteps.get(2).relativeDirection());
    assertEquals(RelativeDirection.CONTINUE, walkSteps.get(3).relativeDirection());
  }

  @Test
  void enterStation() {
    final TestStateBuilder builder = TestStateBuilder.ofWalking()
      .streetEdge()
      .enterStation("Lichterfelde-Ost");
    var walkSteps = buildWalkSteps(builder);
    assertEquals(2, walkSteps.size());
    var enter = walkSteps.get(1);
    assertEquals(ENTRANCE_ID, ((StepEntrance.TransitEntrance) enter.entrance()).id());
    assertEquals(ENTER_STATION, enter.relativeDirection());
  }

  @Test
  void exitStation() {
    final TestStateBuilder builder = TestStateBuilder.ofWalking()
      .streetEdge()
      .exitStation("Lichterfelde-Ost");
    var walkSteps = buildWalkSteps(builder);
    assertEquals(3, walkSteps.size());
    var exit = walkSteps.get(2);
    assertEquals(ENTRANCE_ID, ((StepEntrance.TransitEntrance) exit.entrance()).id());
    assertEquals(EXIT_STATION, exit.relativeDirection());
  }

  @Test
  void exitStationWithEntranceAsLastStep() {
    var walkSteps = buildWalkSteps(
      TestStateBuilder.ofWalking().streetEdge("name", 1).entrance("name")
    );
    assertEquals(2, walkSteps.size());
    assertEquals(RelativeDirection.DEPART, walkSteps.get(0).relativeDirection());
    assertEquals(RelativeDirection.ENTER_OR_EXIT_STATION, walkSteps.get(1).relativeDirection());
  }

  @Test
  void escalatorAndEntrance() {
    var walkSteps = buildWalkSteps(
      TestStateBuilder.ofWalking().streetEdge("name", 1).escalatorEdgeAndStationEntrance()
    );
    assertEquals(3, walkSteps.size());
    assertEquals(RelativeDirection.DEPART, walkSteps.get(0).relativeDirection());
    assertInstanceOf(
      VerticalTransportation.Escalator.class,
      walkSteps.get(1).verticalTransportation()
    );
    assertEquals(RelativeDirection.ENTER_OR_EXIT_STATION, walkSteps.get(2).relativeDirection());
  }

  @Test
  void signpostedPathway() {
    final String sign = "follow signs to platform 1";
    final TestStateBuilder builder = TestStateBuilder.ofWalking().streetEdge().pathway(sign);
    var walkSteps = buildWalkSteps(builder);
    assertEquals(2, walkSteps.size());
    var step = walkSteps.get(1);
    assertEquals(FOLLOW_SIGNS, step.relativeDirection());
    assertEquals(sign, step.directionText().toString());
  }

  private static List<StreetStep> buildWalkSteps(TestStateBuilder builder) {
    var result = builder.build();
    var path = new StreetPath(result);
    var mapper = new StatesToStreetStepsMapper(path.states(), null, 0);
    return mapper.generateSteps();
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
