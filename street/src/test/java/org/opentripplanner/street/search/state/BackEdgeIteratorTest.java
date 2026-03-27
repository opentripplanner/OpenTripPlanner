package org.opentripplanner.street.search.state;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.params.provider.Arguments.of;
import static org.opentripplanner.street.search.state.TestStateBuilder.ofBikeRental;
import static org.opentripplanner.street.search.state.TestStateBuilder.ofCycling;
import static org.opentripplanner.street.search.state.TestStateBuilder.ofDriving;
import static org.opentripplanner.street.search.state.TestStateBuilder.ofWalking;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class BackEdgeIteratorTest {

  private static List<Arguments> cases() {
    return List.of(
      of(ofWalking().build(), 0),
      of(ofWalking().streetEdge().streetEdge().streetEdge().build(), 3),
      of(ofCycling().streetEdge().streetEdge().streetEdge().build(), 3),
      of(ofWalking().streetEdge().elevator().streetEdge().build(), 6),
      of(ofDriving().streetEdge().streetEdge().build(), 2),
      of(ofBikeRental().pickUpFreeFloatingBike().streetEdge().build(), 4)
    );
  }

  @MethodSource("cases")
  @ParameterizedTest
  void emptyState(State state, int expectedSize) {
    var edges = ImmutableList.copyOf(state.listBackEdges());
    assertThat(edges).hasSize(expectedSize);
    assertThat(edges).doesNotContain(null);
  }
}
