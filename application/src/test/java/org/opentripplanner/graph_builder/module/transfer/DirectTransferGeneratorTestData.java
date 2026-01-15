package org.opentripplanner.graph_builder.module.transfer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.module.TransferParameters;
import org.opentripplanner.routing.algorithm.GraphRoutingTest;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.transfer.TransferRepository;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.network.BikeAccess;
import org.opentripplanner.transit.model.network.CarAccess;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.timetable.ScheduledTripTimes;

/**
 * <img src="DirectTransferGeneratorTest.drawio.png" />
 */
class DirectTransferGeneratorTestData extends GraphRoutingTest {

  private static final Duration MAX_TRANSFER_DURATION = Duration.ofHours(1);

  private boolean addPatterns = false;
  private boolean withBoardingConstraint = false;
  private boolean noTransfersOnStationA = false;
  private boolean graphHasStreets = false;
  private boolean includeCarFerryTrips = false;
  private Duration maxTransferDuration = MAX_TRANSFER_DURATION;
  private final List<RouteRequest> transferRequests = new ArrayList<>();
  private final Map<StreetMode, TransferParameters> transferParametersForMode = new HashMap<>();

  public DirectTransferGeneratorTestData withPatterns() {
    this.addPatterns = true;
    return this;
  }

  public DirectTransferGeneratorTestData withNoBoardingForR1AtStop11() {
    this.withBoardingConstraint = true;
    return this;
  }

  public DirectTransferGeneratorTestData withNoTransfersOnStationA() {
    this.noTransfersOnStationA = true;
    return this;
  }

  public DirectTransferGeneratorTestData withStreetGraph() {
    this.graphHasStreets = true;
    return this;
  }

  public DirectTransferGeneratorTestData withCarFerrys_FARAWAY_S0_S12_and_S22_S23() {
    this.includeCarFerryTrips = true;
    return this;
  }

  public DirectTransferGeneratorTestData withMaxTransferDuration(Duration value) {
    this.maxTransferDuration = value;
    return this;
  }

  public DirectTransferGeneratorTestData withTransferRequests(RouteRequest... request) {
    this.transferRequests.addAll(Arrays.asList(request));
    return this;
  }

  public DirectTransferGeneratorTestData addTransferParameters(
    StreetMode mode,
    TransferParameters value
  ) {
    this.transferParametersForMode.put(mode, value);
    return this;
  }

  public TransferRepository build() {
    var model = modelOf(new Builder());
    model.graph().hasStreets = graphHasStreets;

    new DirectTransferGenerator(
      model.graph(),
      model.timetableRepository(),
      model.transferRepository(),
      DataImportIssueStore.NOOP,
      maxTransferDuration,
      transferRequests,
      transferParametersForMode
    ).buildGraph();

    return model.transferRepository();
  }

  static DirectTransferGeneratorTestData of() {
    return new DirectTransferGeneratorTestData();
  }

  private class Builder extends GraphRoutingTest.Builder {

    @Override
    public void build() {
      var stationA = stationEntity("1", s -> s.withTransfersNotAllowed(noTransfersOnStationA));
      TransitStopVertex S0, S_FAR_AWAY, S11, S12, S13, S21, S22, S23;
      StreetVertex V0, V11, V12, V13, V21, V22, V23;

      S0 = stop("S0", b -> b.withCoordinate(47.485, 19.001).withVehicleType(TransitMode.RAIL));
      S_FAR_AWAY = stop("FarAway", 55.0, 30.0);
      S11 = stop("S11", 47.500, 19.001, stationA);
      S12 = stop("S12", 47.520, 19.001);
      S13 = stop("S13", b -> b.withCoordinate(47.540, 19.001).withSometimesUsedRealtime(true));
      S21 = stop("S21", 47.500, 19.011, stationA);
      S22 = stop("S22", b ->
        b
          .withCoordinate(47.520, 19.011)
          .withVehicleType(TransitMode.BUS)
          .withSometimesUsedRealtime(true)
      );
      S23 = stop("S23", b -> b.withCoordinate(47.540, 19.011).withSometimesUsedRealtime(true));

      V0 = intersection("V0", 47.485, 19.000);
      V11 = intersection("V11", 47.500, 19.000);
      V12 = intersection("V12", 47.520, 19.000);
      V13 = intersection("V13", 47.540, 19.000);
      V21 = intersection("V21", 47.500, 19.010);
      V22 = intersection("V22", 47.520, 19.010);
      V23 = intersection("V23", 47.540, 19.010);

      biLink(V0, S0);
      biLink(V11, S11);
      biLink(V12, S12);
      biLink(V13, S13);
      biLink(V21, S21);
      biLink(V22, S22);
      biLink(V23, S23);

      // The street routing is not under test, so no need to restrict
      // StreetTraversalPermission - this only complicates the tests.
      street(V0, V11, 100, StreetTraversalPermission.ALL);
      street(V0, V21, 100, StreetTraversalPermission.ALL);
      street(V0, V22, 200, StreetTraversalPermission.ALL);

      street(V11, V12, 100, StreetTraversalPermission.ALL);
      street(V11, V21, 100, StreetTraversalPermission.ALL);
      street(V11, V22, 110, StreetTraversalPermission.ALL);
      street(V12, V22, 110, StreetTraversalPermission.ALL);
      street(V13, V12, 100, StreetTraversalPermission.ALL);
      street(V22, V23, 100, StreetTraversalPermission.ALL);

      if (addPatterns) {
        var agency = TimetableRepositoryForTest.agency("Agency");

        tripPattern(
          TripPattern.of(TimetableRepositoryForTest.id("TP0"))
            .withRoute(route("R0", TransitMode.RAIL, agency))
            .withStopPattern(new StopPattern(List.of(st(S_FAR_AWAY), st(S0))))
            .build()
        );
        tripPattern(
          TripPattern.of(TimetableRepositoryForTest.id("TP1"))
            .withRoute(route("R1", TransitMode.BUS, agency))
            .withStopPattern(
              new StopPattern(List.of(st(S11, !withBoardingConstraint, true), st(S12)))
            )
            .build()
        );
        tripPattern(
          TripPattern.of(TimetableRepositoryForTest.id("TP2"))
            .withRoute(route("R2", TransitMode.BUS, agency))
            .withStopPattern(new StopPattern(List.of(st(S21), st(S22), st(S_FAR_AWAY))))
            .withScheduledTimeTableBuilder(builder ->
              builder.addTripTimes(
                ScheduledTripTimes.of()
                  .withTrip(
                    TimetableRepositoryForTest.trip("bikesAllowedTrip")
                      .withBikesAllowed(BikeAccess.ALLOWED)
                      .build()
                  )
                  .withDepartureTimes("00:00 01:00 02:00")
                  .build()
              )
            )
            .build()
        );

        if (includeCarFerryTrips) {
          tripPattern(
            TripPattern.of(TimetableRepositoryForTest.id("TP4"))
              .withRoute(route("R4", TransitMode.FERRY, agency))
              .withStopPattern(new StopPattern(List.of(st(S_FAR_AWAY), st(S0), st(S12))))
              .withScheduledTimeTableBuilder(b ->
                b.addTripTimes(createCarsAllowedTripTimesWithTwoStops())
              )
              .build()
          );
          tripPattern(
            TripPattern.of(TimetableRepositoryForTest.id("TP5"))
              .withRoute(route("R5", TransitMode.FERRY, agency))
              .withStopPattern(new StopPattern(List.of(st(S22), st(S23))))
              .withScheduledTimeTableBuilder(b ->
                b.addTripTimes(createCarsAllowedTripTimesWithTwoStops())
              )
              .build()
          );
        }
      }
    }

    private static ScheduledTripTimes createCarsAllowedTripTimesWithTwoStops() {
      return ScheduledTripTimes.of()
        .withTrip(
          TimetableRepositoryForTest.trip("carsAllowed").withCarsAllowed(CarAccess.ALLOWED).build()
        )
        .withDepartureTimes("00:00 01:00")
        .build();
    }

    private TransitStopVertex stop(String id, double lat, double lon, Station parentStation) {
      return stop(id, b -> b.withCoordinate(lat, lon).withParentStation(parentStation));
    }
  }
}
