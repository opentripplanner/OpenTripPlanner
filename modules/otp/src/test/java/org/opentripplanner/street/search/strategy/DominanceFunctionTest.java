package org.opentripplanner.street.search.strategy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.street.model._data.StreetModelForTest.intersectionVertex;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.opentripplanner.astar.spi.DominanceFunction;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.street.model._data.StreetModelForTest;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.StateData;

public class DominanceFunctionTest {

  @Test
  public void testGeneralDominanceFunction() {
    DominanceFunction minimumWeightDominanceFunction = new DominanceFunctions.MinimumWeight();
    Vertex fromVertex = intersectionVertex(1, 1);
    Vertex toVertex = intersectionVertex(2, 2);

    // Test if domination works in the general case

    StreetSearchRequest streetSearchRequest = StreetSearchRequest.of().build();
    StateData stateData = StateData.getBaseCaseStateData(streetSearchRequest);
    State stateA = new State(fromVertex, Instant.EPOCH, stateData, streetSearchRequest);
    State stateB = new State(toVertex, Instant.EPOCH, stateData, streetSearchRequest);
    stateA.weight = 1;
    stateB.weight = 2;

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

    State outsideZone = new State(fromVertex, Instant.EPOCH, stateData, req);
    assertFalse(outsideZone.isInsideNoRentalDropOffArea());

    var edge = StreetModelForTest.streetEdge(fromVertex, toVertex);

    var editor = outsideZone.edit(edge);
    editor.enterNoRentalDropOffArea();
    var insideZone = editor.makeState();
    insideZone.weight = 1;

    assertFalse(dominanceF.betterOrEqualAndComparable(insideZone, outsideZone));
    assertFalse(dominanceF.betterOrEqualAndComparable(outsideZone, insideZone));
  }
}
