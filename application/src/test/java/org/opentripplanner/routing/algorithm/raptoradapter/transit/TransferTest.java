package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.street.model._data.StreetModelForTest.intersectionVertex;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.geometry.Coordinates;
import org.opentripplanner.raptor.api.model.RaptorCostConverter;
import org.opentripplanner.raptor.api.model.RaptorTransfer;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.street.model._data.StreetModelForTest;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.search.request.StreetSearchRequest;

class TransferTest {

  private static final IntersectionVertex BERLIN_V = intersectionVertex(Coordinates.BERLIN);
  private static final IntersectionVertex BRANDENBURG_GATE_V = intersectionVertex(
    Coordinates.BERLIN_BRANDENBURG_GATE
  );
  private static final IntersectionVertex BOSTON_V = intersectionVertex(Coordinates.BOSTON);
  private static final int MAX_RAPTOR_TRANSFER_C1 = RaptorCostConverter.toRaptorCost(
    Transfer.MAX_TRANSFER_COST
  );

  @Nested
  class WithEdges {

    @Test
    void limitMaxCost() {
      // very long edge from Berlin to Boston that has of course a huge cost to traverse
      var edge = StreetModelForTest.streetEdge(BERLIN_V, BOSTON_V);

      var veryLongTransfer = new Transfer(0, List.of(edge), EnumSet.of(StreetMode.WALK));
      assertTrue(veryLongTransfer.getDistanceMeters() > 1_000_000);
      // cost would be too high, so it should be capped to a maximum value
      assertMaxCost(veryLongTransfer.asRaptorTransfer(StreetSearchRequest.of().build()).get());
    }

    @Test
    void allowLowCost() {
      var edge = StreetModelForTest.streetEdge(BERLIN_V, BRANDENBURG_GATE_V);
      var transfer = new Transfer(0, List.of(edge), EnumSet.of(StreetMode.WALK));
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
      var veryLongTransfer = new Transfer(0, Integer.MAX_VALUE, EnumSet.of(StreetMode.WALK));
      assertMaxCost(veryLongTransfer.asRaptorTransfer(StreetSearchRequest.of().build()).get());
    }

    @Test
    void negativeCost() {
      var veryLongTransfer = new Transfer(0, -5, EnumSet.of(StreetMode.WALK));
      assertMaxCost(veryLongTransfer.asRaptorTransfer(StreetSearchRequest.of().build()).get());
    }

    @Test
    void limitMaxCost() {
      var veryLongTransfer = new Transfer(0, 8_000_000, EnumSet.of(StreetMode.WALK));
      // cost would be too high, so it will be capped before passing to RAPTOR
      assertMaxCost(veryLongTransfer.asRaptorTransfer(StreetSearchRequest.of().build()).get());
    }

    @Test
    void allowLowCost() {
      var transfer = new Transfer(0, 200, EnumSet.of(StreetMode.WALK));
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
