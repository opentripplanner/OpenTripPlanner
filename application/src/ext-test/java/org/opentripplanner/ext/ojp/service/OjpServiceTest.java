package org.opentripplanner.ext.ojp.service;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.routing.graphfinder.DirectGraphFinder;
import org.opentripplanner.routing.graphfinder.GraphFinder;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.routing.graphfinder.PlaceAtDistance;
import org.opentripplanner.routing.graphfinder.PlaceType;
import org.opentripplanner.street.model.PropulsionType;
import org.opentripplanner.street.model.RentalFormFactor;
import org.opentripplanner.street.search.state.TestStateBuilder;
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model._data.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model._data.TripInput;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.EntityNotFoundException;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.ArrivalDeparture;
import org.opentripplanner.transit.service.TransitService;

class OjpServiceTest {

  private final TransitTestEnvironmentBuilder envBuilder = TransitTestEnvironment.of();

  private static final String STOP_A_ID = "A";
  private static final String STOP_B_ID = "B";
  private static final String STOP_C_ID = "C";
  private static final String STATION_OMEGA_ID = "OMEGA";

  private final RegularStop STOP_A = envBuilder.stopAtStation(STOP_A_ID, STATION_OMEGA_ID);
  private final RegularStop STOP_B = envBuilder.stop(STOP_B_ID);
  private final RegularStop STOP_C = envBuilder.stop(STOP_C_ID);

  private final TripInput TRIP_INPUT = TripInput.of("t1")
    .addStop(STOP_A, "12:00", "12:01")
    .addStop(STOP_B, "12:10", "12:11")
    .addStop(STOP_C, "12:20", "12:21");

  private OjpService.StopEventRequestParams params(TransitTestEnvironment env, int departures) {
    return new OjpService.StopEventRequestParams(
      env.localTimeParser().instant("12:00"),
      ArrivalDeparture.BOTH,
      Duration.ofHours(2),
      1000,
      departures,
      Set.of(),
      Set.of(),
      Set.of(),
      Set.of(),
      Set.of(),
      Set.of()
    );
  }

  public static List<FeedScopedId> stopPointRefCases() {
    return List.of(id(STOP_A_ID), id(STATION_OMEGA_ID));
  }

  @ParameterizedTest
  @MethodSource("stopPointRefCases")
  void stopPointRef(FeedScopedId ref) {
    var env = envBuilder.addTrip(TRIP_INPUT).build();
    var service = new OjpService(env.transitService(), new DirectGraphFinder(e -> List.of()));
    var result = service.findCallsAtStop(ref, params(env, 100));
    assertThat(result).hasSize(1);
    var stopId = result.getFirst().tripTimeOnDate().getStop().getId();
    assertEquals(STOP_A.getId(), stopId);
  }

  @Test
  void notFound() {
    var env = envBuilder.addTrip(TRIP_INPUT).build();
    var service = new OjpService(env.transitService(), new DirectGraphFinder(e -> List.of()));
    assertThrows(EntityNotFoundException.class, () ->
      service.findCallsAtStop(id("unknown"), params(env, 100))
    );
  }

  @Test
  void coordinates() {
    var finder = new GraphFinder() {
      @Override
      public List<NearbyStop> findClosestStops(Coordinate coordinate, double radiusMeters) {
        return List.of(
          new NearbyStop(STOP_A, 100, List.of(), TestStateBuilder.ofWalking().streetEdge().build())
        );
      }

      @Override
      public List<PlaceAtDistance> findClosestPlaces(
        double lat,
        double lon,
        double radiusMeters,
        int maxResults,
        List<TransitMode> filterByModes,
        List<PlaceType> filterByPlaceTypes,
        List<FeedScopedId> filterByStops,
        List<FeedScopedId> filterByStations,
        List<FeedScopedId> filterByRoutes,
        List<String> filterByBikeRentalStations,
        List<RentalFormFactor> filterByVehicleFormFactor,
        List<PropulsionType> filterByVehiclePropulsionType,
        List<String> filterByNetwork,
        TransitService transitService
      ) {
        return List.of();
      }
    };
    var env = envBuilder.addTrip(TRIP_INPUT).build();
    var service = new OjpService(env.transitService(), finder);
    var result = service.findCallsAtStop(WgsCoordinate.GREENWICH, params(env, 100));
    assertThat(result).hasSize(1);
    var stopId = result.getFirst().tripTimeOnDate().getStop().getId();
    assertEquals(STOP_A.getId(), stopId);
  }

  @Test
  void tooManyDepartures() {
    var env = envBuilder.addTrip(TRIP_INPUT).build();
    var service = new OjpService(env.transitService(), new DirectGraphFinder(e -> List.of()));
    assertThrows(IllegalArgumentException.class, () ->
      service.findCallsAtStop(STOP_A.getId(), params(env, 101))
    );
  }
}
