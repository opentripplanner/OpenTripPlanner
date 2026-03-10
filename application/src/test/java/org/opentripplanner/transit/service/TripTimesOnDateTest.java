package org.opentripplanner.transit.service;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.api.request.TripTimeOnDateRequest;
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model._data.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model._data.TripInput;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.site.RegularStop;

public class TripTimesOnDateTest {

  private static final LocalDate SERVICE_DATE = LocalDate.of(2025, 3, 3);
  private final TransitTestEnvironmentBuilder envBuilder = TransitTestEnvironment.of(SERVICE_DATE);

  private final RegularStop STOP_A = envBuilder.stop("A");
  private final RegularStop STOP_B = envBuilder.stop("B");
  private final RegularStop STOP_C = envBuilder.stop("C");
  private final RegularStop STOP_D = envBuilder.stop("D");
  private final RegularStop STOP_E = envBuilder.stop("E");
  private final RegularStop STOP_F = envBuilder.stop("F");

  private final TripInput TRIP_INPUT1 = TripInput.of("t1")
    .withServiceDates(SERVICE_DATE, SERVICE_DATE.plusDays(1))
    .addStop(STOP_A, "12:00", "12:01")
    .addStop(STOP_B, "12:10", "12:11")
    .addStop(STOP_C, "12:20", "12:21");
  private final TripInput TRIP_INPUT2 = TripInput.of("t2")
    .withServiceDates(SERVICE_DATE, SERVICE_DATE.plusDays(1))
    .addStop(STOP_D, "12:00", "12:01")
    .addStop(STOP_E, "12:10", "12:11")
    .addStop(STOP_F, "12:20", "12:21");

  private final TripInput TRIP_INPUT3 = TripInput.of("t3")
    .withServiceDates(SERVICE_DATE, SERVICE_DATE.plusDays(1))
    .addStop(STOP_F, "12:15", "12:15")
    .addStop(STOP_E, "12:20", "12:20");

  @Test
  void onFirstStop() {
    var env = envBuilder.addTrip(TRIP_INPUT1).addTrip(TRIP_INPUT2).build();
    var transitService = env.transitService();
    var dt = env.localTimeParser();

    var instant = dt.instant("12:00");
    {
      var result = transitService.findTripTimesOnDate(
        TripTimeOnDateRequest.of(List.of(STOP_A)).withTime(instant).build()
      );

      assertThat(result).hasSize(1);
      var tt = result.getFirst();
      assertEquals(dt.instant("12:01"), tt.scheduledDeparture());
    }
    {
      var result = transitService.findTripTimesOnDate(
        TripTimeOnDateRequest.of(List.of(STOP_B)).withTime(instant).build()
      );
      assertThat(result).hasSize(1);
      var tt = result.getFirst();
      assertEquals(dt.instant("12:11"), tt.scheduledDeparture());
    }
  }

  @Test
  void nextDay() {
    var env = envBuilder.addTrip(TRIP_INPUT1).addTrip(TRIP_INPUT2).build();
    var transitService = env.transitService();
    var dt = env.localTimeParser();

    var instant = dt.instant("12:00").plus(Duration.ofDays(1));
    var result = transitService.findTripTimesOnDate(
      TripTimeOnDateRequest.of(List.of(STOP_A)).withTime(instant).build()
    );

    assertThat(result).hasSize(1);
    var tt = result.getFirst();
    assertEquals(dt.instant("12:01").plus(Duration.ofDays(1)), tt.scheduledDeparture());
  }

  @Test
  void tooLate() {
    var env = envBuilder.addTrip(TRIP_INPUT1).build();
    var transitService = env.transitService();
    var dt = env.localTimeParser();

    var instant = dt.instant("18:00");
    var result = transitService.findTripTimesOnDate(
      TripTimeOnDateRequest.of(List.of(STOP_A)).withTime(instant).build()
    );
    assertThat(result).isEmpty();
  }

  @Test
  void shortWindow() {
    var env = envBuilder.addTrip(TRIP_INPUT1).build();
    var transitService = env.transitService();
    var dt = env.localTimeParser();

    var instant = dt.instant("11:00");
    var result = transitService.findTripTimesOnDate(
      TripTimeOnDateRequest.of(List.of(STOP_A))
        .withTime(instant)
        .withTimeWindow(Duration.ofMinutes(59))
        .build()
    );
    assertThat(result).isEmpty();
  }

  @Test
  void longerWindow() {
    var env = envBuilder.addTrip(TRIP_INPUT1).build();
    var transitService = env.transitService();
    var dt = env.localTimeParser();

    var instant = dt.instant("11:00");
    var result = transitService.findTripTimesOnDate(
      TripTimeOnDateRequest.of(List.of(STOP_A))
        .withTime(instant)
        .withTimeWindow(Duration.ofMinutes(60))
        .build()
    );
    assertThat(result).isNotEmpty();
  }

  @Test
  void several() {
    var env = envBuilder.addTrip(TRIP_INPUT2).addTrip(TRIP_INPUT3).build();
    var transitService = env.transitService();
    var dt = env.localTimeParser();

    var instant = dt.instant("12:10");
    var result = transitService.findTripTimesOnDate(
      TripTimeOnDateRequest.of(List.of(STOP_F))
        .withTime(instant)
        .withTimeWindow(Duration.ofMinutes(60))
        .build()
    );
    assertThat(result).hasSize(2);
  }

  /**
   * Mode filter should use the trip's mode, not the route's mode. Trips can override
   * the mode of their route, e.g. a rail replacement bus on a rail route.
   */
  @Test
  void modeFilterRespectsTriplevelModeOverride() {
    var envBuilder = TransitTestEnvironment.of(SERVICE_DATE);
    var stopA = envBuilder.stop("A");
    var stopB = envBuilder.stop("B");

    var railRoute = envBuilder.route("RailRoute", r -> r.withMode(TransitMode.RAIL));

    // Two regular rail trips
    envBuilder.addTrip(
      TripInput.of("rail1").withRoute(railRoute).addStop(stopA, "10:00").addStop(stopB, "10:30")
    );
    envBuilder.addTrip(
      TripInput.of("rail2").withRoute(railRoute).addStop(stopA, "10:10").addStop(stopB, "10:40")
    );
    // A rail replacement bus on the same route
    envBuilder.addTrip(
      TripInput.of("replacementBus")
        .withRoute(railRoute)
        .withMode(TransitMode.BUS)
        .addStop(stopA, "10:20")
        .addStop(stopB, "10:50")
    );

    var env = envBuilder.build();
    var transitService = env.transitService();
    var dt = env.localTimeParser();

    // Filtering for BUS should return only the replacement bus trip
    var busResult = transitService.findTripTimesOnDate(
      TripTimeOnDateRequest.of(List.of(stopA))
        .withTime(dt.instant("10:00"))
        .withTimeWindow(Duration.ofHours(2))
        .withNumberOfDepartures(10)
        .withIncludeModes(List.of(TransitMode.BUS))
        .build()
    );
    assertThat(busResult).hasSize(1);
    assertThat(busResult.getFirst().getTrip().getId().getId()).isEqualTo("replacementBus");

    // Filtering for RAIL should return only the rail trips
    var railResult = transitService.findTripTimesOnDate(
      TripTimeOnDateRequest.of(List.of(stopA))
        .withTime(dt.instant("10:00"))
        .withTimeWindow(Duration.ofHours(2))
        .withNumberOfDepartures(10)
        .withIncludeModes(List.of(TransitMode.RAIL))
        .build()
    );
    assertThat(railResult).hasSize(2);
    assertThat(
      railResult
        .stream()
        .map(t -> t.getTrip().getId().getId())
        .toList()
    ).containsExactly("rail1", "rail2");
  }
}
