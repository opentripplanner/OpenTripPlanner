package org.opentripplanner.street.model.edge;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.street.model.StreetModelFactory.intersectionVertex;

import com.google.common.collect.Range;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.opentripplanner.street.geometry.TestCoordinates;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.StreetModelFactory;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;

public class StreetEdgeTraversalDistanceTest {

  /**
   * We test that length is really in the correct unit.
   */
  @ParameterizedTest
  @EnumSource(StreetMode.class)
  void lengthIsInMeters(StreetMode mode) {
    var v0 = intersectionVertex(TestCoordinates.BERLIN_TV_TOWER);
    var v1 = intersectionVertex(TestCoordinates.BERLIN_BRANDENBURG_GATE);
    var edge = StreetModelFactory.streetEdge(v0, v1);
    var range = Range.closed(2000d, 3000d);
    assertThat(edge.getDistanceMeters()).isIn(range);
    State s0 = new State(
      v0,
      StreetSearchRequest.copyOf(StreetSearchRequest.DEFAULT).withMode(mode).build()
    );
    State s1 = edge.traverse(s0)[0];
    assertThat(s1.getTraversalDistanceMeters()).isIn(range);
  }
}
