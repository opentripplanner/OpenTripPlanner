package org.opentripplanner.street.search.strategy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.street.model.StreetModelFactory.intersectionVertex;

import org.junit.jupiter.api.Test;
import org.opentripplanner.astar.spi.DominanceFunction;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.StreetModelFactory;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.StateData;

class DominanceFunctionTest {

  @Test
  public void testGeneralDominanceFunction() {
    DominanceFunction minimumWeightDominanceFunction = new DominanceFunctions.MinimumWeight();
    Vertex fromVertex = intersectionVertex(1, 1);
    Vertex toVertex = intersectionVertex(2, 2);

    // Test if domination works in the general case

    StreetSearchRequest streetSearchRequest = StreetSearchRequest.of().build();
    StateData stateData = StateData.getBaseCaseStateData(streetSearchRequest);
    State stateA = new State(streetSearchRequest, 1, fromVertex, null, null, stateData, 0, 0);
    State stateB = new State(streetSearchRequest, 2, toVertex, null, null, stateData, 0, 0);

    assertTrue(minimumWeightDominanceFunction.betterOrEqualAndComparable(stateA, stateB));
    assertFalse(minimumWeightDominanceFunction.betterOrEqualAndComparable(stateB, stateA));
  }

  // TODO: Make unit tests for rest of dominance functionality
  // TODO: Make functional tests for concepts covered by dominance with current algorithm
  // (Specific transfers, bike rental, park and ride, turn restrictions)

  @Test
  public void noDropOffZone() {
    var dominanceF = new DominanceFunctions.MinimumWeight();

    var fromVertex = intersectionVertex(1, 1);
    var toVertex = intersectionVertex(2, 2);

    var req = StreetSearchRequest.of().withMode(StreetMode.SCOOTER_RENTAL).build();

    StateData stateData = StateData.getBaseCaseStateData(req);

    State outsideZone = new State(req, 0, fromVertex, null, null, stateData, 0, 0);
    assertFalse(outsideZone.isInsideNoRentalDropOffArea());

    var edge = StreetModelFactory.streetEdge(fromVertex, toVertex);

    var editor = outsideZone.edit(edge);
    editor.enterNoRentalDropOffArea();
    var insideZone = editor.makeState();

    assertFalse(dominanceF.betterOrEqualAndComparable(insideZone, outsideZone));
    assertFalse(dominanceF.betterOrEqualAndComparable(outsideZone, insideZone));
  }
}
