package org.opentripplanner.street.geometry;

import static com.google.common.truth.Truth.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Envelope;

class HashGridSpatialIndexTest {

  /**
   * Bins of 1 x 1 centred on integer coordinates (keys are rounded): bin x=0 spans [-0.5, 0.5] and
   * bin x=1 spans [0.5, 1.5].
   */
  private final HashGridSpatialIndex<String> index = new HashGridSpatialIndex<>(1.0, 1.0);

  @BeforeEach
  void setUp() {
    index.insert(new Envelope(0.1, 0.2, 0.0, 0.0), "single");
    index.insert(new Envelope(0.2, 0.8, 0.0, 0.0), "spanning");
    index.insert(new Envelope(5.0, 5.0, 5.0, 5.0), "far");
  }

  @Test
  void queryReturnsEachItemOfTheTouchedBinsOnce() {
    assertThat(index.query(new Envelope(0.1, 0.8, 0.0, 0.0))).containsExactly("single", "spanning");
  }

  @Test
  void queryReturnsWholeBins() {
    // Neither item intersects the query envelope, but both are stored in the touched bin.
    assertThat(index.query(new Envelope(-0.4, -0.4, 0.0, 0.0))).containsExactly(
      "single",
      "spanning"
    );
  }

  @Test
  void queryAwayFromTheStoredItemsIsEmpty() {
    assertThat(index.query(new Envelope(3.0, 3.0, 3.0, 3.0))).isEmpty();
  }

  @Test
  void forEachCandidateVisitsAnItemOncePerBin() {
    List<String> visited = new ArrayList<>();
    index.forEachCandidate(new Envelope(0.1, 0.2, 0.0, 0.0), visited::add);
    assertThat(visited).containsExactly("single", "spanning").inOrder();

    visited.clear();
    index.forEachCandidate(new Envelope(0.1, 0.8, 0.0, 0.0), visited::add);
    assertThat(visited).containsExactly("single", "spanning", "spanning");
  }
}
