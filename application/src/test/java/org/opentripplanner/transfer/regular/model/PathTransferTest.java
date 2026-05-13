package org.opentripplanner.transfer.regular.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;
import static org.opentripplanner.street.model.StreetModelForTest.intersectionVertex;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.geometry.Coordinates;
import org.opentripplanner.raptor.spi.RaptorCostConverter;
import org.opentripplanner.raptor.spi.RaptorTransfer;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.StreetModelForTest;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.transit.model.site.RegularStop;

class PathTransferTest {

  private static final IntersectionVertex BERLIN_V = intersectionVertex(Coordinates.BERLIN);
  private static final IntersectionVertex BRANDENBURG_GATE_V = intersectionVertex(
    Coordinates.BERLIN_BRANDENBURG_GATE
  );
  private static final IntersectionVertex BOSTON_V = intersectionVertex(Coordinates.BOSTON);
  private static final int MAX_RAPTOR_TRANSFER_C1 = RaptorCostConverter.toRaptorCost(
    PathTransfer.MAX_TRANSFER_COST
  );

  private static final RegularStop S1 = RegularStop.of(id("Stop1"), () -> 1).build();
  private static final RegularStop S2 = RegularStop.of(id("Stop2"), () -> 2).build();

  private static final EnumSet<StreetMode> WALK_ONLY = EnumSet.of(StreetMode.WALK);

  @Nested
  class WithEdges {

    @Test
    void limitMaxCost() {
      // very long edge from Berlin to Boston that has of course a huge cost to traverse
      var edge = StreetModelForTest.streetEdge(BERLIN_V, BOSTON_V);

      var veryLongTransfer = new PathTransfer(
        S1,
        S2,
        edge.getDistanceMeters(),
        List.of(edge),
        WALK_ONLY
      );
      assertTrue(veryLongTransfer.getDistanceMeters() > 1_000_000);
      // cost would be too high, so it should be capped to a maximum value
      assertMaxCost(veryLongTransfer.asRaptorTransfer(StreetSearchRequest.of().build()).get());
    }

    @Test
    void allowLowCost() {
      var edge = StreetModelForTest.streetEdge(BERLIN_V, BRANDENBURG_GATE_V);
      var transfer = new PathTransfer(S1, S2, edge.getDistanceMeters(), List.of(edge), WALK_ONLY);
      assertTrue(transfer.getDistanceMeters() < 4000);
      final Optional<DefaultRaptorTransfer> raptorTransfer = transfer.asRaptorTransfer(
        StreetSearchRequest.of().build()
      );
      // cost is below max limit and included as is in RAPTOR unchanged
      assertBelowMaxCost(raptorTransfer.get());
    }
  }

  @Nested
  class WithoutEdges {

    @Test
    void overflow() {
      var veryLongTransfer = new PathTransfer(S1, S2, Integer.MAX_VALUE, List.of(), WALK_ONLY);
      assertMaxCost(veryLongTransfer.asRaptorTransfer(StreetSearchRequest.of().build()).get());
    }

    @Test
    void negativeCost() {
      var veryLongTransfer = new PathTransfer(S1, S2, -5, List.of(), WALK_ONLY);
      assertMaxCost(veryLongTransfer.asRaptorTransfer(StreetSearchRequest.of().build()).get());
    }

    @Test
    void limitMaxCost() {
      var veryLongTransfer = new PathTransfer(S1, S2, 8_000_000, List.of(), WALK_ONLY);
      // cost would be too high, so it will be capped before passing to RAPTOR
      assertMaxCost(veryLongTransfer.asRaptorTransfer(StreetSearchRequest.of().build()).get());
    }

    @Test
    void allowLowCost() {
      var transfer = new PathTransfer(S1, S2, 200, List.of(), WALK_ONLY);
      final Optional<DefaultRaptorTransfer> raptorTransfer = transfer.asRaptorTransfer(
        StreetSearchRequest.of().build()
      );
      // cost is below max limit and should be included as is in RAPTOR
      assertBelowMaxCost(raptorTransfer.get());
    }
  }

  private static void assertMaxCost(RaptorTransfer transfer) {
    assertEquals(MAX_RAPTOR_TRANSFER_C1, transfer.c1());
  }

  private static void assertBelowMaxCost(RaptorTransfer transfer) {
    assertTrue(MAX_RAPTOR_TRANSFER_C1 > transfer.c1());
  }
}
