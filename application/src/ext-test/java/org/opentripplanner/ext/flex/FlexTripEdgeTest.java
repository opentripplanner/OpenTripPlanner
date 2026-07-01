package org.opentripplanner.ext.flex;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.flex.edgetype.FlexTripEdge;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPath;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.StreetModelForTest;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;

class FlexTripEdgeTest implements PlanTestConstants {

  private static final FlexParameters FLEX_PARAMETERS = new FlexParameters() {
    @Override
    public Duration maxTransferDuration() {
      return FlexParameters.defaultValues().maxTransferDuration();
    }

    @Override
    public Duration maxFlexTripDuration() {
      return FlexParameters.defaultValues().maxFlexTripDuration();
    }

    @Override
    public Duration maxAccessWalkDuration() {
      return FlexParameters.defaultValues().maxAccessWalkDuration();
    }

    @Override
    public Duration maxEgressWalkDuration() {
      return FlexParameters.defaultValues().maxEgressWalkDuration();
    }

    @Override
    public int boardCost() {
      return 999;
    }

    @Override
    public double reluctance() {
      return 5.0;
    }
  };

  private static final FlexTripEdge EDGE = new FlexTripEdge(
    StreetModelForTest.intersectionVertex(1, 1),
    StreetModelForTest.intersectionVertex(2, 2),
    A.stop.getId(),
    B.stop.getId(),
    null,
    1,
    2,
    LocalDate.of(2025, 1, 15),
    new FlexPath(1000, 600, () -> GeometryUtils.makeLineString(1, 1, 2, 2)),
    FLEX_PARAMETERS
  );

  @Test
  void traverseFlexTripEdge() {
    var initialState = new State(
      EDGE.getFromVertex(),
      StreetSearchRequest.of().withMode(StreetMode.FLEXIBLE).build()
    );

    var traverseResult = EDGE.traverse(initialState);
    assertEquals(1, traverseResult.length);
    var traversedState = traverseResult[0];
    assertEquals(TraverseMode.FLEX, traversedState.getBackMode());
    assertEquals(600, traversedState.getElapsedTimeSeconds());
    assertEquals(3999.0, traversedState.getWeight(), 1e-9);
  }
}
