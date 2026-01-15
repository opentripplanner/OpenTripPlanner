package org.opentripplanner.graph_builder.module.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.graph_builder.module.transfer.PathTransferToString.pathToString;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.graph_builder.module.TransferParameters;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.request.StreetRequest;

/**
 * This test uses the following graph/network for testing the DirectTransfer generation. The
 * focus is on the filtering of the transfers, not on testing that the NearBySearch return the
 * correct set of nearby stops.
 * <p>
 * <img src=DirectTransferGeneratorTest.drawio.png />
 */
class DirectTransferGeneratorTest {

  private static final RouteRequest REQUEST_WITH_WALK_TRANSFER = RouteRequest.defaultValue();
  private static final RouteRequest REQUEST_WITH_BIKE_TRANSFER = RouteRequest.of()
    .withJourney(jb -> jb.withTransfer(new StreetRequest(StreetMode.BIKE)))
    .buildDefault();
  private static final TransferParameters TX_BIKES_ALLOWED_1H = new TransferParameters(
    null,
    null,
    Duration.parse("PT1H"),
    true
  );

  @Test
  public void testStraightLineTransfersWithNoPatterns() {
    // There is no trip-patterns to transfer between; Hence empty.
    var repository = DirectTransferGeneratorTestData.of()
      .withTransferRequests(REQUEST_WITH_WALK_TRANSFER)
      .build();
    assertEquals("<Empty>", pathToString(repository.listPathTransfers()));
  }

  @Test
  public void testStraightLineTransfersWithoutPatternsPruning() {
    OTPFeature.ConsiderPatternsForDirectTransfers.testOff(() -> {
      var repository = DirectTransferGeneratorTestData.of()
        .withTransferRequests(REQUEST_WITH_WALK_TRANSFER)
        .build();
      // S0 <-> S23 is too fare, not found in neardby search
      assertEquals(
        """
         S0 - S11, 1668m
         S0 - S12, 3892m
         S0 - S21, 1829m
         S0 - S22, 3964m
        S11 -  S0, 1668m
        S11 - S12, 2224m
        S11 - S13, 4448m
        S11 - S21, 751m
        S11 - S22, 2347m
        S11 - S23, 4511m
        S12 -  S0, 3892m
        S12 - S11, 2224m
        S12 - S13, 2224m
        S12 - S21, 2347m
        S12 - S22, 751m
        S12 - S23, 2347m
        S13 - S11, 4448m
        S13 - S12, 2224m
        S13 - S21, 4511m
        S13 - S22, 2347m
        S13 - S23, 751m
        S21 -  S0, 1829m
        S21 - S11, 751m
        S21 - S12, 2347m
        S21 - S13, 4511m
        S21 - S22, 2224m
        S21 - S23, 4448m
        S22 -  S0, 3964m
        S22 - S11, 2347m
        S22 - S12, 751m
        S22 - S13, 2347m
        S22 - S21, 2224m
        S22 - S23, 2224m
        S23 - S11, 4511m
        S23 - S12, 2347m
        S23 - S13, 751m
        S23 - S21, 4448m
        S23 - S22, 2224m""",
        pathToString(repository.listPathTransfers())
      );
    });
  }

  @Test
  public void testStraightLineTransfersWithPatternsPruning() {
    var repository = DirectTransferGeneratorTestData.of()
      .withPatterns()
      .withTransferRequests(REQUEST_WITH_WALK_TRANSFER)
      .build();

    // Exactly one transfer is expected from each stop to the closest place to board all
    // patterns, including the same pattern used by the stop (E.g. S12 - S11).
    // S11, S13, S21, S23 -> *   No patterns alight here
    // * -> S0, S12, S13, S23    No patterns board here
    assertEquals(
      """
       S0 - S11, 1668m
       S0 - S21, 1829m
      S12 - S11, 2224m
      S12 - S22, 751m
      S22 - S11, 2347m""",
      pathToString(repository.listPathTransfers())
    );
  }

  @Test
  public void testStraightLineTransfersWithBoardingRestrictions() {
    var repository = DirectTransferGeneratorTestData.of()
      .withPatterns()
      .withNoBoardingForR1AtStop11()
      .withTransferRequests(REQUEST_WITH_WALK_TRANSFER)
      .build();

    assertEquals(
      // * -> S11 is not allowed, because of boarding constraints
      // S11, S13, S21 -> *   No patterns alight here
      // * -> S0, S12, S23    No patterns board here
      """
       S0 - S21, 1829m
      S12 - S22, 751m""",
      pathToString(repository.listPathTransfers())
    );
  }

  @Test
  public void testStreetTransfersWithNoPatterns() {
    // There is no trip-patterns to transfer between; Hence empty.
    var repository = DirectTransferGeneratorTestData.of()
      .withStreetGraph()
      .withTransferRequests(REQUEST_WITH_WALK_TRANSFER)
      .build();

    assertEquals("<Empty>", pathToString(repository.listPathTransfers()));
  }

  @Test
  public void testStreetTransfersWithoutPatternsPruning() {
    OTPFeature.ConsiderPatternsForDirectTransfers.testOff(() -> {
      var repository = DirectTransferGeneratorTestData.of()
        .withStreetGraph()
        .withTransferRequests(REQUEST_WITH_WALK_TRANSFER)
        .build();

      // All transfers reachable in the street graph is included.
      assertEquals(
        """
         S0 - S11, 100m
         S0 - S12, 200m
         S0 - S21, 100m
         S0 - S22, 200m
         S0 - S23, 300m
        S11 - S12, 100m
        S11 - S21, 100m
        S11 - S22, 110m
        S11 - S23, 210m
        S12 - S22, 110m
        S12 - S23, 210m
        S13 - S12, 100m
        S13 - S22, 210m
        S13 - S23, 310m
        S22 - S23, 100m""",
        pathToString(repository.listPathTransfers())
      );
    });
  }

  @Test
  public void testStreetTransfersWithPatterns() {
    var repository = DirectTransferGeneratorTestData.of()
      .withPatterns()
      .withStreetGraph()
      .withTransferRequests(REQUEST_WITH_WALK_TRANSFER)
      .build();

    // Best transfers between patterns; Hence S0 - S22 removed
    // S11, S13, S21 -> *   No patterns alight here
    // * -> S0, S12, S23    No patterns board here
    assertEquals(
      """
       S0 - S11, 100m
       S0 - S21, 100m
      S12 - S22, 110m""",
      pathToString(repository.listPathTransfers())
    );
  }

  @Test
  public void testStreetTransfersWithPatternsIncludeRealTimeUsedStops() {
    OTPFeature.IncludeStopsUsedRealTimeInTransfers.testOn(() -> {
      var repository = DirectTransferGeneratorTestData.of()
        .withPatterns()
        .withStreetGraph()
        .withTransferRequests(REQUEST_WITH_WALK_TRANSFER)
        .build();

      // Best transfers between patterns; Hence S0 - S22 removed
      // S11, S21 -> *   No patterns alight here
      // * -> S0, S12    No patterns board here
      // S13, S23        Included, used real-time
      assertEquals(
        """
         S0 - S11, 100m
         S0 - S21, 100m
         S0 - S23, 300m
        S12 - S22, 110m
        S12 - S23, 210m
        S13 - S22, 210m
        S13 - S23, 310m
        S22 - S23, 100m""",
        pathToString(repository.listPathTransfers())
      );
    });
  }

  @Test
  public void testStreetTransfersWithMultipleRequestsWithPatterns() {
    var repository = DirectTransferGeneratorTestData.of()
      .withPatterns()
      .withStreetGraph()
      .withTransferRequests(REQUEST_WITH_WALK_TRANSFER, REQUEST_WITH_BIKE_TRANSFER)
      .build();

    var walkTransfers = repository.findTransfersByMode(StreetMode.WALK);
    var bikeTransfers = repository.findTransfersByMode(StreetMode.BIKE);
    var carTransfers = repository.findTransfersByMode(StreetMode.CAR);

    // Best transfers between patterns; Hence S0 - S22 removed
    // S11, S13, S21 -> *   No patterns alight here
    // * -> S0, S12, S23    No patterns board here
    String expectedWalkAndBike =
      """
       S0 - S11, 100m
       S0 - S21, 100m
      S12 - S22, 110m""";
    assertEquals(expectedWalkAndBike, pathToString(walkTransfers));
    assertEquals(expectedWalkAndBike, pathToString(bikeTransfers));
    assertEquals("<Empty>", pathToString(carTransfers));
  }

  @Test
  public void testStreetTransfersWithStationWithTransfersNotAllowed() {
    OTPFeature.IncludeStopsUsedRealTimeInTransfers.testOn(() -> {
      var repository = DirectTransferGeneratorTestData.of()
        .withPatterns()
        .withStreetGraph()
        .withNoTransfersOnStationA()
        .withTransferRequests(REQUEST_WITH_WALK_TRANSFER)
        .build();

      // Best transfers between patterns; Hence S0 - S22 removed
      // S11, S13, S21 -> *   No patterns alight here
      // * -> S0, S12, S23    No patterns board here
      // Not:   S11, S21      Transfers NOT_ALLOWED for station
      // Allow: S13, S23      Included, used real-time
      assertEquals(
        """
         S0 - S22, 200m
         S0 - S23, 300m
        S12 - S22, 110m
        S12 - S23, 210m
        S13 - S22, 210m
        S13 - S23, 310m
        S22 - S23, 100m""",
        pathToString(repository.listPathTransfers())
      );
    });
  }

  @Test
  public void testBikeRequestWithBikesAllowedTransfersWithIncludeEmptyRailStopsInTransfersOn() {
    OTPFeature.IncludeStopsUsedRealTimeInTransfers.testOn(() -> {
      var repository = DirectTransferGeneratorTestData.of()
        .withPatterns()
        .withStreetGraph()
        .withTransferRequests(REQUEST_WITH_BIKE_TRANSFER)
        .addTransferParameters(StreetMode.BIKE, TX_BIKES_ALLOWED_1H)
        .build();

      var bikeTransfers = repository.findTransfersByMode(StreetMode.BIKE);
      assertEquals(
        """
        S13 - S22, 210m
        S13 - S23, 310m
        S22 - S23, 100m""",
        pathToString(bikeTransfers)
      );
    });
  }

  @Test
  public void testBikeRequestWithBikesAllowedTransfersWithConsiderPatternsForDirectTransfersOff() {
    OTPFeature.ConsiderPatternsForDirectTransfers.testOff(() -> {
      var repository = DirectTransferGeneratorTestData.of()
        .withPatterns()
        .withStreetGraph()
        .withTransferRequests(REQUEST_WITH_BIKE_TRANSFER)
        .addTransferParameters(StreetMode.BIKE, TX_BIKES_ALLOWED_1H)
        .build();

      var bikeTransfers = repository.findTransfersByMode(StreetMode.BIKE);
      assertEquals(
        """
        S13 - S22, 210m
        S13 - S23, 310m
        S22 - S23, 100m""",
        pathToString(bikeTransfers)
      );
    });
  }
}
