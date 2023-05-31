package org.opentripplanner.routing.algorithm.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.model.plan.RelativeDirection;
import org.opentripplanner.model.plan.WalkStep;
import org.opentripplanner.routing.services.notes.StreetNotesService;
import org.opentripplanner.street.search.state.TestStateBuilder;

class StatesToWalkStepsMapperTest {
  @Test
  void absoluteDirection() {
    List<WalkStep> walkSteps = buildWalkSteps(TestStateBuilder.ofWalking().streetEdge().streetEdge());
    assertEquals(2, walkSteps.size());
    walkSteps.forEach(step -> assertNotNull(step.getAbsoluteDirection()));
  }

  @Test
  void elevator() {
    List<WalkStep> walkSteps = buildWalkSteps(TestStateBuilder.ofWalking().streetEdge().elevator().streetEdge());
    var elevatorStep = walkSteps.get(3);
    assertEquals(RelativeDirection.ELEVATOR, elevatorStep.getRelativeDirection());
    assertNull(elevatorStep.getAbsoluteDirection());
  }

  private static List<WalkStep> buildWalkSteps(TestStateBuilder builder) {
    var result = builder.build();
    var path = new GraphPath<>(result);
    var mapper = new StatesToWalkStepsMapper(path.states, null, new StreetNotesService(), 0);
    return mapper.generateWalkSteps();
  }

}