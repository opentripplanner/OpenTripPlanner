package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.street.model._data.StreetModelForTest.intersectionVertex;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.geometry.Coordinates;
import org.opentripplanner.raptor.api.model.RaptorTransfer;
import org.opentripplanner.street.model._data.StreetModelForTest;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.search.request.StreetSearchRequest;

class TransferTest {

  private static final IntersectionVertex BERLIN_V = intersectionVertex(Coordinates.BERLIN);
  private static final IntersectionVertex BRANDENBURG_GATE_V = intersectionVertex(
    Coordinates.BERLIN_BRANDENBURG_GATE
  );
  private static final IntersectionVertex KONGSBERG_V = intersectionVertex(
    Coordinates.KONGSBERG_PLATFORM_1
  );

  @Nested
  class WithEdges {

    @Test
    void limitMaxCost() {
      // very long edge from Berlin to Kongsberg, Norway that has of course a huge cost to traverse
      var edge = StreetModelForTest.streetEdge(BERLIN_V, KONGSBERG_V);

      var veryLongTransfer = new Transfer(0, List.of(edge));
      assertTrue(veryLongTransfer.getDistanceMeters() > 800_000);
      // cost would be too high, so it should not be included in RAPTOR search
      assertTrue(veryLongTransfer.asRaptorTransfer(StreetSearchRequest.of().build()).isEmpty());
    }

    @Test
    void allowLowCost() {
      var edge = StreetModelForTest.streetEdge(BERLIN_V, BRANDENBURG_GATE_V);
      var transfer = new Transfer(0, List.of(edge));
      assertTrue(transfer.getDistanceMeters() < 4000);
      final Optional<RaptorTransfer> raptorTransfer = transfer.asRaptorTransfer(
        StreetSearchRequest.of().build()
      );
      // cost is below max limit and should be included in RAPTOR
      assertTrue(raptorTransfer.isPresent());
    }
  }

  @Nested
  class WithoutEdges {

    @Test
    void limitMaxCost() {
      var veryLongTransfer = new Transfer(0, 800_000);
      // cost would be too high, so it should not be included in RAPTOR search
      assertTrue(veryLongTransfer.asRaptorTransfer(StreetSearchRequest.of().build()).isEmpty());
    }

    @Test
    void allowLowCost() {
      var transfer = new Transfer(0, 200);
      final Optional<RaptorTransfer> raptorTransfer = transfer.asRaptorTransfer(
        StreetSearchRequest.of().build()
      );
      // cost is below max limit and should be included in RAPTOR
      assertTrue(raptorTransfer.isPresent());
    }
  }
}
