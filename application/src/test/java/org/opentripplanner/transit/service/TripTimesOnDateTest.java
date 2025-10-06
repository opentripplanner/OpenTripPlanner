package org.opentripplanner.transit.service;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.api.request.TripTimeOnDateRequest;
import org.opentripplanner.transit.model._data.SiteTestBuilder;
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model._data.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model._data.TripInput;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.updater.trip.RealtimeTestConstants;

public class TripTimesOnDateTest implements RealtimeTestConstants {

  private static final LocalDate SERVICE_DATE = LocalDate.of(2025, 3, 3);

  private final SiteTestBuilder siteBuilder = SiteTestBuilder.of();
  private final RegularStop STOP_A = siteBuilder.stop(STOP_A_ID);
  private final RegularStop STOP_B = siteBuilder.stop(STOP_B_ID);
  private final RegularStop STOP_C = siteBuilder.stop(STOP_C_ID);
  private final RegularStop STOP_D = siteBuilder.stop(STOP_D_ID);
  private final RegularStop STOP_E = siteBuilder.stop(STOP_E_ID);
  private final RegularStop STOP_F = siteBuilder.stop(STOP_F_ID);

  private final TransitTestEnvironmentBuilder envBuilder = TransitTestEnvironment.of(
    siteBuilder.build(),
    SERVICE_DATE
  );

  private final TripInput TRIP_INPUT1 = TripInput.of("t1")
    .withServiceDates(SERVICE_DATE, SERVICE_DATE.plusDays(1))
    .addStop(STOP_A_ID, "12:00", "12:01")
    .addStop(STOP_B_ID, "12:10", "12:11")
    .addStop(STOP_C_ID, "12:20", "12:21")
    .build();
  private final TripInput TRIP_INPUT2 = TripInput.of("t2")
    .withServiceDates(SERVICE_DATE, SERVICE_DATE.plusDays(1))
    .addStop(STOP_D_ID, "12:00", "12:01")
    .addStop(STOP_E_ID, "12:10", "12:11")
    .addStop(STOP_F_ID, "12:20", "12:21")
    .build();

  private final TripInput TRIP_INPUT3 = TripInput.of("t3")
    .withServiceDates(SERVICE_DATE, SERVICE_DATE.plusDays(1))
    .addStop(STOP_F_ID, "12:15", "12:15")
    .addStop(STOP_E_ID, "12:20", "12:20")
    .build();

  @Test
  void onFirstStop() {
    var env = envBuilder.addTrip(TRIP_INPUT1).addTrip(TRIP_INPUT2).build();
    var transitService = env.getTransitService();
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
    var transitService = env.getTransitService();
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
    var transitService = env.getTransitService();
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
    var transitService = env.getTransitService();
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
    var transitService = env.getTransitService();
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
    var transitService = env.getTransitService();
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
}
