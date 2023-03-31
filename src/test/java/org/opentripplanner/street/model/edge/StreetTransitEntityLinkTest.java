package org.opentripplanner.street.model.edge;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opentripplanner.transit.model.basic.Accessibility.NOT_POSSIBLE;
import static org.opentripplanner.transit.model.basic.Accessibility.NO_INFORMATION;
import static org.opentripplanner.transit.model.basic.Accessibility.POSSIBLE;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.preference.AccessibilityPreferences;
import org.opentripplanner.routing.api.request.preference.WheelchairPreferences;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.vertex.SimpleVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertexBuilder;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.site.RegularStop;

class StreetTransitEntityLinkTest {

  private static final Graph graph = new Graph(new Deduplicator());

  RegularStop inaccessibleStop = TransitModelForTest.stopForTest(
    "A:inaccessible",
    "wheelchair inaccessible stop",
    10.001,
    10.001,
    null,
    NOT_POSSIBLE
  );
  RegularStop accessibleStop = TransitModelForTest.stopForTest(
    "A:accessible",
    "wheelchair accessible stop",
    10.001,
    10.001,
    null,
    POSSIBLE
  );

  RegularStop unknownStop = TransitModelForTest.stopForTest(
    "A:unknown",
    "unknown",
    10.001,
    10.001,
    null,
    NO_INFORMATION
  );

  @Test
  void disallowInaccessibleStop() {
    var afterTraversal = traverse(inaccessibleStop, true);
    assertNull(afterTraversal);
  }

  @Test
  void allowAccessibleStop() {
    State afterTraversal = traverse(accessibleStop, true);

    assertNotNull(afterTraversal);
  }

  @Test
  void unknownStop() {
    var afterTraversal = traverse(unknownStop, false);
    assertNotNull(afterTraversal);

    var afterStrictTraversal = traverse(unknownStop, true);
    assertNull(afterStrictTraversal);
  }

  private State traverse(RegularStop stop, boolean onlyAccessible) {
    var from = new SimpleVertex(graph, "A", 10, 10);
    var to = new TransitStopVertexBuilder()
      .withGraph(graph)
      .withStop(stop)
      .withModes(Set.of(TransitMode.RAIL))
      .build();

    var req = StreetSearchRequest.of().withMode(StreetMode.BIKE);
    AccessibilityPreferences feature;
    if (onlyAccessible) {
      feature = AccessibilityPreferences.ofOnlyAccessible();
    } else {
      feature = AccessibilityPreferences.ofCost(100, 100);
    }
    req.withWheelchair(true);
    req.withPreferences(p ->
      p.withWheelchair(
        WheelchairPreferences
          .of()
          .withTrip(feature)
          .withStop(feature)
          .withElevator(feature)
          .withInaccessibleStreetReluctance(25)
          .withMaxSlope(0.045)
          .withSlopeExceededReluctance(10)
          .withStairsReluctance(25)
          .build()
      )
    );

    var edge = new StreetTransitStopLink(from, to);
    return edge.traverse(new State(from, req.build()));
  }
}
