package org.opentripplanner.graph_builder.module.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.graph_builder.module.transfer.PathTransferToString.pathToString;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.graph_builder.module.TransferParameters;
import org.opentripplanner.routing.algorithm.GraphRoutingTest;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.street.model.StreetMode;

class DirectTransferGeneratorCarTest extends GraphRoutingTest {

  private static final RouteRequest REQUEST_WITH_WALK_TRANSFER = RouteRequest.defaultValue();
  private static final RouteRequest REQUEST_WITH_BIKE_TRANSFER = RouteRequest.of()
    .withJourney(jb -> jb.withTransfer(new StreetRequest(StreetMode.BIKE)))
    .buildDefault();
  private static final RouteRequest REQUEST_WITH_CAR_TRANSFER = RouteRequest.of()
    .withJourney(jb -> jb.withTransfer(new StreetRequest(StreetMode.CAR)))
    .buildDefault();

  @Test
  public void testRequestWithCarsAllowedPatterns() {
    OTPFeature.ConsiderPatternsForDirectTransfers.testOff(() -> {
      var transferParameters = new TransferParameters.Builder()
        .withCarsAllowedStopMaxTransferDuration(Duration.ofMinutes(60))
        .withDisableDefaultTransfers(true)
        .build();

      var repository = testDataWithStreetFraphAndPatterns()
        .withCarFerrys_FARAWAY_S0_S12_and_S22_S23()
        .withTransferRequests(REQUEST_WITH_CAR_TRANSFER)
        .addTransferParameters(StreetMode.CAR, transferParameters)
        .build();

      assertEquals(
        """
         S0 - S12, 200m
         S0 - S22, 200m
         S0 - S23, 300m
        S12 - S22, 110m
        S12 - S23, 210m
        S22 - S23, 100m""",
        pathToString(repository.listPathTransfers())
      );
    });
  }

  @Test
  public void testRequestWithCarsAllowedPatternsWithDurationLimit() {
    var transferParameters = new TransferParameters.Builder()
      .withCarsAllowedStopMaxTransferDuration(Duration.ofSeconds(10))
      .withDisableDefaultTransfers(true)
      .build();

    var repository = testDataWithStreetFraphAndPatterns()
      .withCarFerrys_FARAWAY_S0_S12_and_S22_S23()
      .withTransferRequests(REQUEST_WITH_CAR_TRANSFER)
      .addTransferParameters(StreetMode.CAR, transferParameters)
      .build();

    assertEquals(
      """
      S12 - S22, 110m""",
      pathToString(repository.listPathTransfers())
    );
  }

  @Test
  public void testMultipleRequestsWithPatternsAndWithCarsAllowedPatterns() {
    var transferParameters = new TransferParameters.Builder()
      .withCarsAllowedStopMaxTransferDuration(Duration.ofMinutes(60))
      .withDisableDefaultTransfers(true)
      .build();

    var repository = testDataWithStreetFraphAndPatterns()
      .withCarFerrys_FARAWAY_S0_S12_and_S22_S23()
      .withTransferRequests(
        REQUEST_WITH_WALK_TRANSFER,
        REQUEST_WITH_BIKE_TRANSFER,
        REQUEST_WITH_CAR_TRANSFER
      )
      .addTransferParameters(StreetMode.CAR, transferParameters)
      .build();

    String expected_walk_bike_results = """
       S0 - S11, 100m
       S0 - S21, 100m
       S0 - S22, 200m
      S12 - S22, 110m""";
    assertEquals(
      expected_walk_bike_results,
      pathToString(repository.findTransfersByMode(StreetMode.WALK))
    );
    assertEquals(
      expected_walk_bike_results,
      pathToString(repository.findTransfersByMode(StreetMode.BIKE))
    );
    assertEquals(
      """
       S0 - S22, 200m
      S12 - S22, 110m""",
      pathToString(repository.findTransfersByMode(StreetMode.CAR))
    );
  }

  @Test
  public void testBikeRequestWithPatternsAndWithCarsAllowedPatterns() {
    var transferParameters = new TransferParameters.Builder()
      .withCarsAllowedStopMaxTransferDuration(Duration.ofMinutes(120))
      .build();

    var repository = testDataWithStreetFraphAndPatterns()
      .withCarFerrys_FARAWAY_S0_S12_and_S22_S23()
      .withTransferRequests(REQUEST_WITH_BIKE_TRANSFER)
      .addTransferParameters(StreetMode.BIKE, transferParameters)
      .build();

    assertEquals(
      """
       S0 - S11, 100m
       S0 - S21, 100m
       S0 - S22, 200m
      S12 - S22, 110m""",
      pathToString(repository.listPathTransfers())
    );
  }

  @Test
  public void testBikeRequestWithPatternsAndWithCarsAllowedPatternsWithoutCarInTransferRequests() {
    var repository = testDataWithStreetFraphAndPatterns()
      .withCarFerrys_FARAWAY_S0_S12_and_S22_S23()
      .withMaxTransferDuration(Duration.ofSeconds(30))
      .withTransferRequests(REQUEST_WITH_BIKE_TRANSFER)
      .build();

    assertEquals(
      """
       S0 - S11, 100m
       S0 - S21, 100m
      S12 - S22, 110m""",
      pathToString(repository.listPathTransfers())
    );
  }

  @Test
  public void testDisableDefaultTransfersForMode() {
    var transferParametersBuilderBike = new TransferParameters.Builder()
      .withDisableDefaultTransfers(true)
      .build();

    var transferParametersBuilderCar = new TransferParameters.Builder()
      .withDisableDefaultTransfers(true)
      .build();

    var repository = testDataWithStreetFraphAndPatterns()
      .withCarFerrys_FARAWAY_S0_S12_and_S22_S23()
      .withTransferRequests(
        REQUEST_WITH_WALK_TRANSFER,
        REQUEST_WITH_BIKE_TRANSFER,
        REQUEST_WITH_CAR_TRANSFER
      )
      .addTransferParameters(StreetMode.BIKE, transferParametersBuilderBike)
      .addTransferParameters(StreetMode.CAR, transferParametersBuilderCar)
      .build();

    assertEquals(
      """
       S0 - S11, 100m
       S0 - S21, 100m
       S0 - S22, 200m
      S12 - S22, 110m""",
      pathToString(repository.findTransfersByMode(StreetMode.WALK))
    );

    assertEquals("<Empty>", pathToString(repository.findTransfersByMode(StreetMode.BIKE)));

    assertEquals("<Empty>", pathToString(repository.findTransfersByMode(StreetMode.CAR)));
  }

  @Test
  public void testMaxTransferDurationForMode() {
    var transferParametersBuilderWalk = new TransferParameters.Builder()
      .withMaxTransferDuration(Duration.ofSeconds(100))
      .build();
    var transferParametersBuilderBike = new TransferParameters.Builder()
      .withMaxTransferDuration(Duration.ofSeconds(21))
      .build();

    var repository = DirectTransferGeneratorTestData.of()
      .withPatterns()
      .withStreetGraph()
      .withCarFerrys_FARAWAY_S0_S12_and_S22_S23()
      .withTransferRequests(REQUEST_WITH_WALK_TRANSFER, REQUEST_WITH_BIKE_TRANSFER)
      .addTransferParameters(StreetMode.WALK, transferParametersBuilderWalk)
      .addTransferParameters(StreetMode.BIKE, transferParametersBuilderBike)
      .build();

    assertEquals(
      """
       S0 - S11, 100m
       S0 - S21, 100m
      S12 - S22, 110m""",
      pathToString(repository.findTransfersByMode(StreetMode.WALK))
    );
    assertEquals(
      """
      S0 - S11, 100m
      S0 - S21, 100m""".indent(1)
        .stripTrailing(),
      pathToString(repository.findTransfersByMode(StreetMode.BIKE))
    );
    assertEquals("<Empty>", pathToString(repository.findTransfersByMode(StreetMode.CAR)));
  }

  /**
   * Testing Car Transfer Generation should be done with the street-graph and using the patterns
   * for "tagging" stops with CAR_ALLOWED. Using "line-of-sight" street routing does not add much
   * to this test. It is covered by the {@link DirectTransferGeneratorTest}.
   */
  private static DirectTransferGeneratorTestData testDataWithStreetFraphAndPatterns() {
    return DirectTransferGeneratorTestData.of().withPatterns().withStreetGraph();
  }
}
