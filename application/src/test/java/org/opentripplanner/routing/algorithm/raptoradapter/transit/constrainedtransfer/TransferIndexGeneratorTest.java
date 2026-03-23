package org.opentripplanner.routing.algorithm.raptoradapter.transit.constrainedtransfer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.opentripplanner.routing.algorithm.raptoradapter.transit.request.TestTransitCaseData.STOP_A;
import static org.opentripplanner.routing.algorithm.raptoradapter.transit.request.TestTransitCaseData.STOP_B;
import static org.opentripplanner.routing.algorithm.raptoradapter.transit.request.TestTransitCaseData.STOP_C;
import static org.opentripplanner.routing.algorithm.raptoradapter.transit.request.TestTransitCaseData.STOP_D;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.TestRouteData;
import org.opentripplanner.transfer.constrained.model.ConstrainedTransfer;
import org.opentripplanner.transfer.constrained.model.StopTransferPoint;
import org.opentripplanner.transfer.constrained.model.TransferConstraint;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.network.TripPattern;

class TransferIndexGeneratorTest {

  private static final FeedScopedId ID = new FeedScopedId("F", "TX1");
  private static final TransferConstraint GUARANTEED = TransferConstraint.of().guaranteed().build();
  private static final StopTransferPoint STOP_B_TX = new StopTransferPoint(STOP_B);
  private static final StopTransferPoint STOP_C_TX = new StopTransferPoint(STOP_C);

  private TestRouteData route1;
  private TestRouteData route2;
  private TripPattern pattern1;
  private TripPattern pattern2;

  @BeforeEach
  void setup() {
    route1 = TestRouteData.of(
      "R1",
      TransitMode.RAIL,
      List.of(STOP_A, STOP_B, STOP_C),
      "10:00 10:10 10:20",
      "10:05 10:15 10:25"
    );
    route2 = TestRouteData.of(
      "R2",
      TransitMode.BUS,
      List.of(STOP_B, STOP_C, STOP_D),
      "10:15 10:30 10:40",
      "10:20 10:35 10:45"
    );
    pattern1 = route1.getTripPattern();
    pattern2 = route2.getTripPattern();
  }

  @Test
  void generateTransfersCachesResultWhenNothingChanges() {
    var tx = new ConstrainedTransfer(ID, STOP_B_TX, STOP_B_TX, GUARANTEED);
    var subject = new TransferIndexGenerator(List.of(tx), List.of(pattern1, pattern2));

    var first = subject.generateTransfers();
    var second = subject.generateTransfers();

    assertSame(first, second, "Should return cached instance when nothing changed");
  }

  @Test
  void generateTransfersRegeneratesWhenNewRealtimeTripAdded() {
    var tx = new ConstrainedTransfer(ID, STOP_B_TX, STOP_B_TX, GUARANTEED);
    var subject = new TransferIndexGenerator(List.of(tx), List.of(pattern1, pattern2));

    var first = subject.generateTransfers();

    // Create a new real-time trip pattern with a different stop pattern (added stop)
    var rtRoute = TestRouteData.of("R3", TransitMode.BUS, List.of(STOP_B, STOP_C), "10:25 10:40");
    var rtPattern = rtRoute.getTripPattern();
    subject.addRealtimeTrip(rtPattern, rtPattern.scheduledTripsAsStream().toList());

    var second = subject.generateTransfers();

    assertNotSame(first, second, "Should regenerate when a new pattern is added");
  }

  @Test
  void addRealtimeTripWithExistingPatternDoesNotInvalidateCache() {
    var tx = new ConstrainedTransfer(ID, STOP_B_TX, STOP_B_TX, GUARANTEED);
    var subject = new TransferIndexGenerator(List.of(tx), List.of(pattern1, pattern2));

    var first = subject.generateTransfers();

    // Re-add the same pattern/trip combination that was already indexed
    subject.addRealtimeTrip(pattern1, pattern1.scheduledTripsAsStream().toList());

    var second = subject.generateTransfers();

    assertSame(first, second, "Should return cached instance when no new associations were added");
  }

  @Test
  void generateTransfersProducesCorrectResultsOnFirstCall() {
    var tx = new ConstrainedTransfer(ID, STOP_C_TX, STOP_C_TX, GUARANTEED);
    var subject = new TransferIndexGenerator(List.of(tx), List.of(pattern1, pattern2));

    var result = subject.generateTransfers();

    // Pattern2 boards at STOP_C (position 1), so forward transfers should exist there
    var toPattern2 = result.toStop(pattern2.getRoutingTripPattern().patternIndex());
    assertNotNull(toPattern2, "Should have forward transfers for pattern2");

    // Pattern1 alights at STOP_C (position 2), so reverse transfers should exist there
    var fromPattern1 = result.fromStop(pattern1.getRoutingTripPattern().patternIndex());
    assertNotNull(fromPattern1, "Should have reverse transfers for pattern1");
  }
}
