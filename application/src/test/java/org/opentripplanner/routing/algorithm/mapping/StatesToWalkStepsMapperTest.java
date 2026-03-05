package org.opentripplanner.routing.algorithm.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.model.plan.walkstep.RelativeDirection.ENTER_STATION;
import static org.opentripplanner.model.plan.walkstep.RelativeDirection.EXIT_STATION;
import static org.opentripplanner.model.plan.walkstep.RelativeDirection.FOLLOW_SIGNS;
import static org.opentripplanner.routing.algorithm.mapping.StatesToWalkStepsMapper.isOnSameStreet;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.model.plan.walkstep.RelativeDirection;
import org.opentripplanner.model.plan.walkstep.WalkStep;
import org.opentripplanner.model.plan.walkstep.WalkStepBuilder;
import org.opentripplanner.model.plan.walkstep.verticaltransportation.ElevatorUse;
import org.opentripplanner.model.plan.walkstep.verticaltransportation.EscalatorUse;
import org.opentripplanner.model.plan.walkstep.verticaltransportation.StairsUse;
import org.opentripplanner.service.streetdetails.internal.DefaultStreetDetailsRepository;
import org.opentripplanner.service.streetdetails.internal.DefaultStreetDetailsService;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.internal.notes.StreetNotesService;
import org.opentripplanner.street.search.state.TestStateBuilder;
import org.opentripplanner.transit.model.site.Entrance;

class StatesToWalkStepsMapperTest {

  private static final FeedScopedId ENTRANCE_ID = new FeedScopedId("F", "Lichterfelde-Ost");

  @Test
  void absoluteDirection() {
    var walkSteps = buildWalkSteps(TestStateBuilder.ofWalking().streetEdge().streetEdge());
    assertEquals(2, walkSteps.size());
    walkSteps.forEach(step -> assertTrue(step.getAbsoluteDirection().isPresent()));
  }

  @Test
  void elevator() {
    var walkSteps = buildWalkSteps(
      TestStateBuilder.ofWalking().streetEdge().elevator().streetEdge()
    );
    var elevatorStep = walkSteps.get(3);
    assertEquals(RelativeDirection.ELEVATOR, elevatorStep.getRelativeDirection());
    assertEquals(
      ElevatorUse.class.getSimpleName(),
      elevatorStep.verticalTransportationUse().get().getClass().getSimpleName()
    );
    assertTrue(elevatorStep.getAbsoluteDirection().isEmpty());
  }

  @Test
  void stairs() {
    var walkSteps = buildWalkSteps(
      TestStateBuilder.ofWalking().streetEdge().stairsEdge().streetEdge()
    );
    assertEquals(RelativeDirection.DEPART, walkSteps.get(0).getRelativeDirection());
    assertEquals(
      StairsUse.class.getSimpleName(),
      walkSteps.get(1).verticalTransportationUse().get().getClass().getSimpleName()
    );
    assertEquals(RelativeDirection.CONTINUE, walkSteps.get(2).getRelativeDirection());
  }

  @Test
  void escalator() {
    var walkSteps = buildWalkSteps(
      TestStateBuilder.ofWalking().streetEdge().escalatorEdge().streetEdge()
    );
    assertEquals(RelativeDirection.DEPART, walkSteps.get(0).getRelativeDirection());
    assertEquals(
      EscalatorUse.class.getSimpleName(),
      walkSteps.get(1).verticalTransportationUse().get().getClass().getSimpleName()
    );
    assertEquals(RelativeDirection.CONTINUE, walkSteps.get(2).getRelativeDirection());
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
    assertEquals(3, walkSteps.size());
    assertEquals(RelativeDirection.DEPART, walkSteps.get(0).getRelativeDirection());
    assertEquals(RelativeDirection.ENTER_OR_EXIT_STATION, walkSteps.get(1).getRelativeDirection());
    assertEquals(RelativeDirection.CONTINUE, walkSteps.get(2).getRelativeDirection());
  }

  @Test
  void enterStation() {
    final TestStateBuilder builder = TestStateBuilder.ofWalking()
      .streetEdge()
      .enterStation("Lichterfelde-Ost");
    var walkSteps = buildWalkSteps(builder);
    assertEquals(2, walkSteps.size());
    var enter = walkSteps.get(1);
    assertEquals(ENTRANCE_ID, enter.entrance().get().getId());
    assertEquals(ENTER_STATION, enter.getRelativeDirection());
  }

  @Test
  void exitStation() {
    final TestStateBuilder builder = TestStateBuilder.ofWalking()
      .streetEdge()
      .exitStation("Lichterfelde-Ost");
    var walkSteps = buildWalkSteps(builder);
    assertEquals(3, walkSteps.size());
    var exit = walkSteps.get(2);
    assertEquals(ENTRANCE_ID, exit.entrance().get().getId());
    assertEquals(EXIT_STATION, exit.getRelativeDirection());
  }

  @Test
  void signpostedPathway() {
    final String sign = "follow signs to platform 1";
    final TestStateBuilder builder = TestStateBuilder.ofWalking().streetEdge().pathway(sign);
    var walkSteps = buildWalkSteps(builder);
    assertEquals(2, walkSteps.size());
    var step = walkSteps.get(1);
    assertEquals(FOLLOW_SIGNS, step.getRelativeDirection());
    assertEquals(sign, step.getDirectionText().toString());
  }

  private static List<WalkStep> buildWalkSteps(TestStateBuilder builder) {
    var result = builder.build();
    var path = new GraphPath<>(result);
    var mapper = new StatesToWalkStepsMapper(
      path.states,
      null,
      new StreetNotesService(),
      new DefaultStreetDetailsService(new DefaultStreetDetailsRepository()),
      id -> Entrance.of(id).withCoordinate(WgsCoordinate.GREENWICH).build(),
      0
    );
    return mapper.generateWalkSteps();
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
