package org.opentripplanner.street.model.edge;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.transit.model.basic.Accessibility.NOT_POSSIBLE;
import static org.opentripplanner.transit.model.basic.Accessibility.NO_INFORMATION;
import static org.opentripplanner.transit.model.basic.Accessibility.POSSIBLE;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.preference.AccessibilityPreferences;
import org.opentripplanner.routing.api.request.preference.WheelchairPreferences;
import org.opentripplanner.street.model._data.StreetModelForTest;
import org.opentripplanner.street.model.vertex.TransitStopVertexBuilder;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.site.RegularStop;

class StreetTransitEntityLinkTest {

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
    assertTrue(State.isEmpty(afterTraversal));
  }

  @Test
  void allowAccessibleStop() {
    var afterTraversal = traverse(accessibleStop, true);

    assertFalse(State.isEmpty(afterTraversal));
  }

  @Test
  void unknownStop() {
    var afterTraversal = traverse(unknownStop, false);
    assertFalse(State.isEmpty(afterTraversal));

    var afterStrictTraversal = traverse(unknownStop, true);
    assertTrue(State.isEmpty(afterStrictTraversal));
  }

  private State[] traverse(RegularStop stop, boolean onlyAccessible) {
    var from = StreetModelForTest.intersectionVertex("A", 10, 10);
    var to = new TransitStopVertexBuilder()
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

    var edge = StreetTransitStopLink.createStreetTransitStopLink(from, to);
    return edge.traverse(new State(from, req.build()));
  }
}
