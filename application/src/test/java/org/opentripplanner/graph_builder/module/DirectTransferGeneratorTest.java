package org.opentripplanner.graph_builder.module;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.model.PathTransfer;
import org.opentripplanner.routing.algorithm.GraphRoutingTest;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.framework.DurationForEnum;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.network.CarAccess;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.ScheduledTripTimes;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * This creates a graph with trip patterns
 <pre>
  S0 -  V0 ------------
        |     \       |
 S11 - V11 --------> V21 - S21
        |      \      |
 S12 - V12 --------> V22 - V22
        |             |
 S13 - V13 --------> V23 - V23
 </pre>
 */
class DirectTransferGeneratorTest extends GraphRoutingTest {

  private static final Duration MAX_TRANSFER_DURATION = Duration.ofHours(1);
  private TransitStopVertex S0, S11, S12, S13, S21, S22, S23;
  private StreetVertex V0, V11, V12, V13, V21, V22, V23;

  @Test
  public void testDirectTransfersWithoutPatterns() {
    var otpModel = model(false);
    var graph = otpModel.graph();
    var timetableRepository = otpModel.timetableRepository();
    var req = new RouteRequest();
    req.journey().transfer().setMode(StreetMode.WALK);
    var transferRequests = List.of(req);
    graph.hasStreets = false;

    new DirectTransferGenerator(
      graph,
      timetableRepository,
      DataImportIssueStore.NOOP,
      MAX_TRANSFER_DURATION,
      transferRequests
    ).buildGraph();

    assertTransfers(timetableRepository.getAllPathTransfers());
  }

  @Test
  public void testDirectTransfersWithPatterns() {
    var otpModel = model(true);
    var graph = otpModel.graph();
    graph.hasStreets = false;
    var timetableRepository = otpModel.timetableRepository();
    var req = new RouteRequest();
    req.journey().transfer().setMode(StreetMode.WALK);
    var transferRequests = List.of(req);

    new DirectTransferGenerator(
      graph,
      timetableRepository,
      DataImportIssueStore.NOOP,
      MAX_TRANSFER_DURATION,
      transferRequests
    ).buildGraph();

    assertTransfers(
      timetableRepository.getAllPathTransfers(),
      tr(S0, 556, S11),
      tr(S0, 935, S21),
      tr(S11, 751, S21),
      tr(S12, 751, S22),
      tr(S13, 2224, S12),
      tr(S13, 2347, S22),
      tr(S21, 751, S11),
      tr(S22, 751, S12),
      tr(S23, 2347, S12),
      tr(S23, 2224, S22)
    );
  }

  @Test
  public void testDirectTransfersWithRestrictedPatterns() {
    var otpModel = model(true, true);
    var graph = otpModel.graph();
    graph.hasStreets = false;
    var timetableRepository = otpModel.timetableRepository();
    var transferRequests = List.of(new RouteRequest());

    new DirectTransferGenerator(
      graph,
      timetableRepository,
      DataImportIssueStore.NOOP,
      MAX_TRANSFER_DURATION,
      transferRequests
    ).buildGraph();

    assertTransfers(
      timetableRepository.getAllPathTransfers(),
      tr(S0, 2780, S12),
      tr(S0, 935, S21),
      tr(S11, 2224, S12),
      tr(S11, 751, S21),
      tr(S12, 751, S22),
      tr(S13, 2224, S12),
      tr(S13, 2347, S22),
      tr(S21, 2347, S12),
      tr(S22, 751, S12),
      tr(S23, 2347, S12),
      tr(S23, 2224, S22)
    );
  }

  @Test
  public void testSingleRequestWithoutPatterns() {
    var req = new RouteRequest();
    req.journey().transfer().setMode(StreetMode.WALK);
    var transferRequests = List.of(req);

    var otpModel = model(false);
    var graph = otpModel.graph();
    graph.hasStreets = true;
    var timetableRepository = otpModel.timetableRepository();

    new DirectTransferGenerator(
      graph,
      timetableRepository,
      DataImportIssueStore.NOOP,
      MAX_TRANSFER_DURATION,
      transferRequests
    ).buildGraph();

    assertTransfers(timetableRepository.getAllPathTransfers());
  }

  @Test
  public void testSingleRequestWithPatterns() {
    var req = new RouteRequest();
    req.journey().transfer().setMode(StreetMode.WALK);
    var transferRequests = List.of(req);

    var otpModel = model(true);
    var graph = otpModel.graph();
    graph.hasStreets = true;
    var timetableRepository = otpModel.timetableRepository();

    new DirectTransferGenerator(
      graph,
      timetableRepository,
      DataImportIssueStore.NOOP,
      MAX_TRANSFER_DURATION,
      transferRequests
    ).buildGraph();

    assertTransfers(
      timetableRepository.getAllPathTransfers(),
      tr(S0, 100, List.of(V0, V11), S11),
      tr(S0, 100, List.of(V0, V21), S21),
      tr(S11, 100, List.of(V11, V21), S21)
    );
  }

  @Test
  public void testMultipleRequestsWithoutPatterns() {
    var reqWalk = new RouteRequest();
    reqWalk.journey().transfer().setMode(StreetMode.WALK);

    var reqBike = new RouteRequest();
    reqBike.journey().transfer().setMode(StreetMode.BIKE);

    var transferRequests = List.of(reqWalk, reqBike);

    var otpModel = model(false);
    var graph = otpModel.graph();
    graph.hasStreets = true;
    var timetableRepository = otpModel.timetableRepository();

    new DirectTransferGenerator(
      graph,
      timetableRepository,
      DataImportIssueStore.NOOP,
      MAX_TRANSFER_DURATION,
      transferRequests
    ).buildGraph();

    assertTransfers(timetableRepository.getAllPathTransfers());
  }

  @Test
  public void testMultipleRequestsWithPatterns() {
    var reqWalk = new RouteRequest();
    reqWalk.journey().transfer().setMode(StreetMode.WALK);

    var reqBike = new RouteRequest();
    reqBike.journey().transfer().setMode(StreetMode.BIKE);

    var transferRequests = List.of(reqWalk, reqBike);

    TestOtpModel model = model(true);
    var graph = model.graph();
    graph.hasStreets = true;
    var timetableRepository = model.timetableRepository();

    new DirectTransferGenerator(
      graph,
      timetableRepository,
      DataImportIssueStore.NOOP,
      MAX_TRANSFER_DURATION,
      transferRequests
    ).buildGraph();

    var walkTransfers = timetableRepository.findTransfers(StreetMode.WALK);
    var bikeTransfers = timetableRepository.findTransfers(StreetMode.BIKE);
    var carTransfers = timetableRepository.findTransfers(StreetMode.CAR);

    assertTransfers(
      walkTransfers,
      tr(S0, 100, List.of(V0, V11), S11),
      tr(S0, 100, List.of(V0, V21), S21),
      tr(S11, 100, List.of(V11, V21), S21)
    );
    assertTransfers(
      bikeTransfers,
      tr(S0, 100, List.of(V0, V11), S11),
      tr(S0, 100, List.of(V0, V21), S21),
      tr(S11, 110, List.of(V11, V22), S22)
    );
    assertTransfers(carTransfers);
  }

  @Test
  public void testTransferOnIsolatedStations() {
    var otpModel = model(true, false, true, false);
    var graph = otpModel.graph();
    graph.hasStreets = false;

    var timetableRepository = otpModel.timetableRepository();
    var req = new RouteRequest();
    req.journey().transfer().setMode(StreetMode.WALK);
    var transferRequests = List.of(req);

    new DirectTransferGenerator(
      graph,
      timetableRepository,
      DataImportIssueStore.NOOP,
      MAX_TRANSFER_DURATION,
      transferRequests
    ).buildGraph();

    assertTrue(timetableRepository.getAllPathTransfers().isEmpty());
  }

  @Test
  public void testRequestWithCarsAllowedPatterns() {
    var reqCar = new RouteRequest();
    reqCar.journey().transfer().setMode(StreetMode.CAR);

    var transferRequests = List.of(reqCar);

    var otpModel = model(false, false, false, true);
    var graph = otpModel.graph();
    graph.hasStreets = true;
    var timetableRepository = otpModel.timetableRepository();

    TransferParameters.Builder transferParametersBuilder = new TransferParameters.Builder();
    transferParametersBuilder.withCarsAllowedStopMaxTransferDuration(Duration.ofMinutes(60));
    transferParametersBuilder.withDisableDefaultTransfers(true);
    Map<StreetMode, TransferParameters> transferParametersForMode = new HashMap<>();
    transferParametersForMode.put(StreetMode.CAR, transferParametersBuilder.build());

    new DirectTransferGenerator(
      graph,
      timetableRepository,
      DataImportIssueStore.NOOP,
      MAX_TRANSFER_DURATION,
      transferRequests,
      transferParametersForMode
    ).buildGraph();

    assertTransfers(
      timetableRepository.getAllPathTransfers(),
      tr(S0, 100, List.of(V0, V11), S11),
      tr(S0, 200, List.of(V0, V12), S12)
    );
  }

  @Test
  public void testRequestWithCarsAllowedPatternsWithDurationLimit() {
    var reqCar = new RouteRequest();
    reqCar.journey().transfer().setMode(StreetMode.CAR);

    var transferRequests = List.of(reqCar);

    var otpModel = model(false, false, false, true);
    var graph = otpModel.graph();
    graph.hasStreets = true;
    var timetableRepository = otpModel.timetableRepository();

    TransferParameters.Builder transferParametersBuilder = new TransferParameters.Builder();
    transferParametersBuilder.withCarsAllowedStopMaxTransferDuration(Duration.ofSeconds(10));
    transferParametersBuilder.withDisableDefaultTransfers(true);
    Map<StreetMode, TransferParameters> transferParametersForMode = new HashMap<>();
    transferParametersForMode.put(StreetMode.CAR, transferParametersBuilder.build());

    new DirectTransferGenerator(
      graph,
      timetableRepository,
      DataImportIssueStore.NOOP,
      MAX_TRANSFER_DURATION,
      transferRequests,
      transferParametersForMode
    ).buildGraph();

    assertTransfers(timetableRepository.getAllPathTransfers(), tr(S0, 100, List.of(V0, V11), S11));
  }

  @Test
  public void testMultipleRequestsWithPatternsAndWithCarsAllowedPatterns() {
    var reqWalk = new RouteRequest();
    reqWalk.journey().transfer().setMode(StreetMode.WALK);

    var reqBike = new RouteRequest();
    reqBike.journey().transfer().setMode(StreetMode.BIKE);

    var reqCar = new RouteRequest();
    reqCar.journey().transfer().setMode(StreetMode.CAR);

    var transferRequests = List.of(reqWalk, reqBike, reqCar);

    var otpModel = model(true, false, false, true);
    var graph = otpModel.graph();
    graph.hasStreets = true;
    var timetableRepository = otpModel.timetableRepository();

    TransferParameters.Builder transferParametersBuilder = new TransferParameters.Builder();
    transferParametersBuilder.withCarsAllowedStopMaxTransferDuration(Duration.ofMinutes(60));
    transferParametersBuilder.withDisableDefaultTransfers(true);
    Map<StreetMode, TransferParameters> transferParametersForMode = new HashMap<>();
    transferParametersForMode.put(StreetMode.CAR, transferParametersBuilder.build());

    new DirectTransferGenerator(
      graph,
      timetableRepository,
      DataImportIssueStore.NOOP,
      MAX_TRANSFER_DURATION,
      transferRequests,
      transferParametersForMode
    ).buildGraph();

    var walkTransfers = timetableRepository.findTransfers(StreetMode.WALK);
    var bikeTransfers = timetableRepository.findTransfers(StreetMode.BIKE);
    var carTransfers = timetableRepository.findTransfers(StreetMode.CAR);

    assertTransfers(
      walkTransfers,
      tr(S0, 100, List.of(V0, V11), S11),
      tr(S0, 100, List.of(V0, V21), S21),
      tr(S11, 100, List.of(V11, V21), S21),
      tr(S0, 200, List.of(V0, V12), S12),
      tr(S11, 100, List.of(V11, V12), S12)
    );
    assertTransfers(
      bikeTransfers,
      tr(S0, 100, List.of(V0, V11), S11),
      tr(S0, 100, List.of(V0, V21), S21),
      tr(S11, 110, List.of(V11, V22), S22),
      tr(S0, 200, List.of(V0, V12), S12),
      tr(S11, 100, List.of(V11, V12), S12)
    );
    assertTransfers(
      carTransfers,
      tr(S0, 100, List.of(V0, V11), S11),
      tr(S0, 200, List.of(V0, V12), S12),
      tr(S0, 100, List.of(V0, V21), S21)
    );
  }

  @Test
  public void testBikeRequestWithPatternsAndWithCarsAllowedPatterns() {
    var reqBike = new RouteRequest();
    reqBike.journey().transfer().setMode(StreetMode.BIKE);

    var transferRequests = List.of(reqBike);

    var otpModel = model(true, false, false, true);
    var graph = otpModel.graph();
    graph.hasStreets = true;
    var timetableRepository = otpModel.timetableRepository();

    TransferParameters.Builder transferParametersBuilder = new TransferParameters.Builder();
    transferParametersBuilder.withCarsAllowedStopMaxTransferDuration(Duration.ofMinutes(120));
    Map<StreetMode, TransferParameters> transferParametersForMode = new HashMap<>();
    transferParametersForMode.put(StreetMode.BIKE, transferParametersBuilder.build());

    new DirectTransferGenerator(
      graph,
      timetableRepository,
      DataImportIssueStore.NOOP,
      Duration.ofSeconds(30),
      transferRequests,
      transferParametersForMode
    ).buildGraph();

    assertTransfers(
      timetableRepository.getAllPathTransfers(),
      tr(S0, 100, List.of(V0, V11), S11),
      tr(S0, 100, List.of(V0, V21), S21),
      tr(S0, 200, List.of(V0, V12), S12),
      tr(S11, 110, List.of(V11, V22), S22),
      tr(S11, 100, List.of(V11, V12), S12)
    );
  }

  @Test
  public void testBikeRequestWithPatternsAndWithCarsAllowedPatternsWithoutCarInTransferRequests() {
    var reqBike = new RouteRequest();
    reqBike.journey().transfer().setMode(StreetMode.BIKE);

    var transferRequests = List.of(reqBike);

    var otpModel = model(true, false, false, true);
    var graph = otpModel.graph();
    graph.hasStreets = true;
    var timetableRepository = otpModel.timetableRepository();

    new DirectTransferGenerator(
      graph,
      timetableRepository,
      DataImportIssueStore.NOOP,
      Duration.ofSeconds(30),
      transferRequests
    ).buildGraph();

    assertTransfers(
      timetableRepository.getAllPathTransfers(),
      tr(S0, 100, List.of(V0, V11), S11),
      tr(S0, 100, List.of(V0, V21), S21),
      tr(S11, 110, List.of(V11, V22), S22)
    );
  }

  @Test
  public void testDisableDefaultTransfersForMode() {
    var reqWalk = new RouteRequest();
    reqWalk.journey().transfer().setMode(StreetMode.WALK);

    var reqBike = new RouteRequest();
    reqBike.journey().transfer().setMode(StreetMode.BIKE);

    var reqCar = new RouteRequest();
    reqCar.journey().transfer().setMode(StreetMode.CAR);

    var transferRequests = List.of(reqWalk, reqBike, reqCar);

    var otpModel = model(true, false, false, true);
    var graph = otpModel.graph();
    graph.hasStreets = true;
    var timetableRepository = otpModel.timetableRepository();

    TransferParameters.Builder transferParametersBuilderBike = new TransferParameters.Builder();
    transferParametersBuilderBike.withDisableDefaultTransfers(true);
    TransferParameters.Builder transferParametersBuilderCar = new TransferParameters.Builder();
    transferParametersBuilderCar.withDisableDefaultTransfers(true);
    Map<StreetMode, TransferParameters> transferParametersForMode = new HashMap<>();
    transferParametersForMode.put(StreetMode.BIKE, transferParametersBuilderBike.build());
    transferParametersForMode.put(StreetMode.CAR, transferParametersBuilderCar.build());

    new DirectTransferGenerator(
      graph,
      timetableRepository,
      DataImportIssueStore.NOOP,
      MAX_TRANSFER_DURATION,
      transferRequests,
      transferParametersForMode
    ).buildGraph();

    var walkTransfers = timetableRepository.findTransfers(StreetMode.WALK);
    var bikeTransfers = timetableRepository.findTransfers(StreetMode.BIKE);
    var carTransfers = timetableRepository.findTransfers(StreetMode.CAR);

    assertTransfers(
      walkTransfers,
      tr(S0, 100, List.of(V0, V11), S11),
      tr(S0, 100, List.of(V0, V21), S21),
      tr(S11, 100, List.of(V11, V21), S21),
      tr(S0, 200, List.of(V0, V12), S12),
      tr(S11, 100, List.of(V11, V12), S12)
    );
    assertTransfers(bikeTransfers);
    assertTransfers(carTransfers);
  }

  @Test
  public void testMaxTransferDurationForMode() {
    var reqWalk = new RouteRequest();
    reqWalk.journey().transfer().setMode(StreetMode.WALK);

    var reqBike = new RouteRequest();
    reqBike.journey().transfer().setMode(StreetMode.BIKE);

    var transferRequests = List.of(reqWalk, reqBike);

    var otpModel = model(true, false, false, true);
    var graph = otpModel.graph();
    graph.hasStreets = true;
    var timetableRepository = otpModel.timetableRepository();

    TransferParameters.Builder transferParametersBuilderWalk = new TransferParameters.Builder();
    transferParametersBuilderWalk.withMaxTransferDuration(Duration.ofSeconds(100));
    TransferParameters.Builder transferParametersBuilderBike = new TransferParameters.Builder();
    transferParametersBuilderBike.withMaxTransferDuration(Duration.ofSeconds(21));
    Map<StreetMode, TransferParameters> transferParametersForMode = new HashMap<>();
    transferParametersForMode.put(StreetMode.WALK, transferParametersBuilderWalk.build());
    transferParametersForMode.put(StreetMode.BIKE, transferParametersBuilderBike.build());

    new DirectTransferGenerator(
      graph,
      timetableRepository,
      DataImportIssueStore.NOOP,
      MAX_TRANSFER_DURATION,
      transferRequests,
      transferParametersForMode
    ).buildGraph();

    var walkTransfers = timetableRepository.findTransfers(StreetMode.WALK);
    var bikeTransfers = timetableRepository.findTransfers(StreetMode.BIKE);
    var carTransfers = timetableRepository.findTransfers(StreetMode.CAR);

    assertTransfers(
      walkTransfers,
      tr(S0, 100, List.of(V0, V11), S11),
      tr(S0, 100, List.of(V0, V21), S21),
      tr(S11, 100, List.of(V11, V21), S21),
      tr(S11, 100, List.of(V11, V12), S12)
    );
    assertTransfers(
      bikeTransfers,
      tr(S0, 100, List.of(V0, V11), S11),
      tr(S0, 100, List.of(V0, V21), S21)
    );
    assertTransfers(carTransfers);
  }

  private TestOtpModel model(boolean addPatterns) {
    return model(addPatterns, false);
  }

  private TestOtpModel model(boolean addPatterns, boolean withBoardingConstraint) {
    return model(addPatterns, withBoardingConstraint, false, false);
  }

  private TestOtpModel model(
    boolean addPatterns,
    boolean withBoardingConstraint,
    boolean withNoTransfersOnStations,
    boolean addCarsAllowedPatterns
  ) {
    return modelOf(
      new Builder() {
        @Override
        public void build() {
          var station = stationEntity("1", builder ->
            builder.withTransfersNotAllowed(withNoTransfersOnStations)
          );

          S0 = stop("S0", 47.495, 19.001, station);
          S11 = stop("S11", 47.500, 19.001, station);
          S12 = stop("S12", 47.520, 19.001, station);
          S13 = stop("S13", 47.540, 19.001, station);
          S21 = stop("S21", 47.500, 19.011, station);
          S22 = stop("S22", 47.520, 19.011, station);
          S23 = stop("S23", 47.540, 19.011, station);

          V0 = intersection("V0", 47.495, 19.000);
          V11 = intersection("V11", 47.500, 19.000);
          V12 = intersection("V12", 47.510, 19.000);
          V13 = intersection("V13", 47.520, 19.000);
          V21 = intersection("V21", 47.500, 19.010);
          V22 = intersection("V22", 47.510, 19.010);
          V23 = intersection("V23", 47.520, 19.010);

          biLink(V0, S0);
          biLink(V11, S11);
          biLink(V12, S12);
          biLink(V13, S13);
          biLink(V21, S21);
          biLink(V22, S22);
          biLink(V23, S23);

          street(V0, V11, 100, StreetTraversalPermission.ALL);
          street(V0, V12, 200, StreetTraversalPermission.ALL);
          street(V0, V21, 100, StreetTraversalPermission.ALL);
          street(V0, V22, 200, StreetTraversalPermission.ALL);

          street(V11, V12, 100, StreetTraversalPermission.PEDESTRIAN);
          street(V12, V13, 100, StreetTraversalPermission.PEDESTRIAN);
          street(V21, V22, 100, StreetTraversalPermission.PEDESTRIAN);
          street(V22, V23, 100, StreetTraversalPermission.PEDESTRIAN);
          street(V11, V21, 100, StreetTraversalPermission.PEDESTRIAN);
          street(V11, V22, 110, StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE);

          if (addPatterns) {
            var agency = TimetableRepositoryForTest.agency("Agency");

            tripPattern(
              TripPattern.of(TimetableRepositoryForTest.id("TP1"))
                .withRoute(route("R1", TransitMode.BUS, agency))
                .withStopPattern(
                  new StopPattern(List.of(st(S11, !withBoardingConstraint, true), st(S12), st(S13)))
                )
                .build()
            );

            tripPattern(
              TripPattern.of(TimetableRepositoryForTest.id("TP2"))
                .withRoute(route("R2", TransitMode.BUS, agency))
                .withStopPattern(new StopPattern(List.of(st(S21), st(S22), st(S23))))
                .build()
            );
          }

          if (addCarsAllowedPatterns) {
            var agency = TimetableRepositoryForTest.agency("FerryAgency");

            tripPattern(
              TripPattern.of(TimetableRepositoryForTest.id("TP3"))
                .withRoute(route("R3", TransitMode.FERRY, agency))
                .withStopPattern(new StopPattern(List.of(st(S11), st(S21))))
                .withScheduledTimeTableBuilder(builder ->
                  builder.addTripTimes(
                    ScheduledTripTimes.of()
                      .withTrip(
                        TimetableRepositoryForTest.trip("carsAllowedTrip")
                          .withCarsAllowed(CarAccess.ALLOWED)
                          .build()
                      )
                      .withDepartureTimes("00:00 01:00")
                      .build()
                  )
                )
                .build()
            );

            tripPattern(
              TripPattern.of(TimetableRepositoryForTest.id("TP4"))
                .withRoute(route("R4", TransitMode.FERRY, agency))
                .withStopPattern(new StopPattern(List.of(st(S0), st(S13))))
                .withScheduledTimeTableBuilder(builder ->
                  builder.addTripTimes(
                    ScheduledTripTimes.of()
                      .withTrip(
                        TimetableRepositoryForTest.trip("carsAllowedTrip")
                          .withCarsAllowed(CarAccess.ALLOWED)
                          .build()
                      )
                      .withDepartureTimes("00:00 01:00")
                      .build()
                  )
                )
                .build()
            );

            tripPattern(
              TripPattern.of(TimetableRepositoryForTest.id("TP5"))
                .withRoute(route("R5", TransitMode.FERRY, agency))
                .withStopPattern(new StopPattern(List.of(st(S12), st(S22))))
                .withScheduledTimeTableBuilder(builder ->
                  builder.addTripTimes(
                    ScheduledTripTimes.of()
                      .withTrip(
                        TimetableRepositoryForTest.trip("carsAllowedTrip")
                          .withCarsAllowed(CarAccess.ALLOWED)
                          .build()
                      )
                      .withDepartureTimes("00:00 01:00")
                      .build()
                  )
                )
                .build()
            );
          }
        }
      }
    );
  }

  private void assertTransfers(
    Collection<PathTransfer> allPathTransfers,
    TransferDescriptor... transfers
  ) {
    var matchedTransfers = new HashSet<PathTransfer>();
    var assertions = Stream.concat(
      Arrays.stream(transfers).map(td -> td.matcher(allPathTransfers, matchedTransfers)),
      Stream.of(allTransfersMatched(allPathTransfers, matchedTransfers))
    );

    assertAll(assertions);
  }

  private Executable allTransfersMatched(
    Collection<PathTransfer> transfersByStop,
    Set<PathTransfer> matchedTransfers
  ) {
    return () -> {
      var missingTransfers = new HashSet<>(transfersByStop);
      missingTransfers.removeAll(matchedTransfers);

      assertEquals(Set.of(), missingTransfers, "All transfers matched");
    };
  }

  private TransferDescriptor tr(TransitStopVertex from, double distance, TransitStopVertex to) {
    return new TransferDescriptor(from, distance, to);
  }

  private TransferDescriptor tr(
    TransitStopVertex from,
    double distance,
    List<StreetVertex> vertices,
    TransitStopVertex to
  ) {
    return new TransferDescriptor(from, distance, vertices, to);
  }

  private static class TransferDescriptor {

    private final StopLocation from;
    private final StopLocation to;
    private final Double distanceMeters;
    private final List<StreetVertex> vertices;

    public TransferDescriptor(TransitStopVertex from, Double distanceMeters, TransitStopVertex to) {
      this.from = from.getStop();
      this.distanceMeters = distanceMeters;
      this.vertices = null;
      this.to = to.getStop();
    }

    public TransferDescriptor(
      TransitStopVertex from,
      Double distanceMeters,
      List<StreetVertex> vertices,
      TransitStopVertex to
    ) {
      this.from = from.getStop();
      this.distanceMeters = distanceMeters;
      this.vertices = vertices;
      this.to = to.getStop();
    }

    @Override
    public String toString() {
      return ToStringBuilder.of(getClass())
        .addObj("from", from)
        .addObj("to", to)
        .addNum("distanceMeters", distanceMeters)
        .addCol("vertices", vertices)
        .toString();
    }

    boolean matches(PathTransfer transfer) {
      if (!Objects.equals(from, transfer.from) || !Objects.equals(to, transfer.to)) {
        return false;
      }

      if (vertices == null) {
        return distanceMeters == transfer.getDistanceMeters() && transfer.getEdges() == null;
      } else {
        var transferVertices = transfer
          .getEdges()
          .stream()
          .map(Edge::getToVertex)
          .filter(StreetVertex.class::isInstance)
          .toList();

        return (
          distanceMeters == transfer.getDistanceMeters() &&
          Objects.equals(vertices, transferVertices)
        );
      }
    }

    private Executable matcher(
      Collection<PathTransfer> transfersByStop,
      Set<PathTransfer> matchedTransfers
    ) {
      return () -> {
        var matched = transfersByStop.stream().filter(this::matches).findFirst();

        if (matched.isPresent()) {
          assertTrue(true, "Found transfer for " + this);
          matchedTransfers.add(matched.get());
        } else {
          fail("Missing transfer for " + this);
        }
      };
    }
  }
}
