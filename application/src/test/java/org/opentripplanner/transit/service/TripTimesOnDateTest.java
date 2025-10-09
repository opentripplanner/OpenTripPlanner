package org.opentripplanner.transit.service;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.api.request.TripTimeOnDateRequest;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.RealtimeTestEnvironment;
import org.opentripplanner.updater.trip.RealtimeTestEnvironmentBuilder;
import org.opentripplanner.updater.trip.TripInput;
import org.opentripplanner.utils.time.TimeUtils;

public class TripTimesOnDateTest implements RealtimeTestConstants {

  private final RealtimeTestEnvironmentBuilder envBuilder = RealtimeTestEnvironment.of();

  private final RegularStop STOP_A = envBuilder.stop(STOP_A_ID);
  private final RegularStop STOP_B = envBuilder.stop(STOP_B_ID);
  private final RegularStop STOP_C = envBuilder.stop(STOP_C_ID);
  private final RegularStop STOP_D = envBuilder.stop(STOP_D_ID);
  private final RegularStop STOP_E = envBuilder.stop(STOP_E_ID);
  private final RegularStop STOP_F = envBuilder.stop(STOP_F_ID);

  private final TripInput TRIP_INPUT1 = TripInput.of("t1")
    .withServiceDates(SERVICE_DATE, SERVICE_DATE.plusDays(1))
    .addStop(STOP_A, "12:00", "12:01")
    .addStop(STOP_B, "12:10", "12:11")
    .addStop(STOP_C, "12:20", "12:21")
    .build();
  private final TripInput TRIP_INPUT2 = TripInput.of("t2")
    .withServiceDates(SERVICE_DATE, SERVICE_DATE.plusDays(1))
    .addStop(STOP_D, "12:00", "12:01")
    .addStop(STOP_E, "12:10", "12:11")
    .addStop(STOP_F, "12:20", "12:21")
    .build();

  private final TripInput TRIP_INPUT3 = TripInput.of("t3")
    .withServiceDates(SERVICE_DATE, SERVICE_DATE.plusDays(1))
    .addStop(STOP_F, "12:15", "12:15")
    .addStop(STOP_E, "12:20", "12:20")
    .build();

  @Test
  void onFirstStop() {
    var env = envBuilder.addTrip(TRIP_INPUT1).addTrip(TRIP_INPUT2).build();
    var transitService = env.getTransitService();

    var instant = instant("12:00");
    {
      var result = transitService.findTripTimesOnDate(
        TripTimeOnDateRequest.of(List.of(STOP_A)).withTime(instant).build()
      );

      assertThat(result).hasSize(1);
      var tt = result.getFirst();
      assertEquals(instant("12:01"), tt.scheduledDeparture());
    }
    {
      var result = transitService.findTripTimesOnDate(
        TripTimeOnDateRequest.of(List.of(STOP_B)).withTime(instant).build()
      );
      assertThat(result).hasSize(1);
      var tt = result.getFirst();
      assertEquals(instant("12:11"), tt.scheduledDeparture());
    }
  }

  @Test
  void nextDay() {
    var env = envBuilder.addTrip(TRIP_INPUT1).addTrip(TRIP_INPUT2).build();
    var transitService = env.getTransitService();

    var instant = instant("12:00").plus(Duration.ofDays(1));
    var result = transitService.findTripTimesOnDate(
      TripTimeOnDateRequest.of(List.of(STOP_A)).withTime(instant).build()
    );

    assertThat(result).hasSize(1);
    var tt = result.getFirst();
    assertEquals(instant("12:01").plus(Duration.ofDays(1)), tt.scheduledDeparture());
  }

  @Test
  void tooLate() {
    var transitService = envBuilder.addTrip(TRIP_INPUT1).build().getTransitService();

    var instant = instant("18:00");
    var result = transitService.findTripTimesOnDate(
      TripTimeOnDateRequest.of(List.of(STOP_A)).withTime(instant).build()
    );
    assertThat(result).isEmpty();
  }

  @Test
  void shortWindow() {
    var transitService = envBuilder.addTrip(TRIP_INPUT1).build().getTransitService();

    var instant = instant("11:00");
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
    var transitService = envBuilder.addTrip(TRIP_INPUT1).build().getTransitService();

    var instant = instant("11:00");
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
    var transitService = envBuilder
      .addTrip(TRIP_INPUT2)
      .addTrip(TRIP_INPUT3)
      .build()
      .getTransitService();

    var instant = instant("12:10");
    var result = transitService.findTripTimesOnDate(
      TripTimeOnDateRequest.of(List.of(STOP_F))
        .withTime(instant)
        .withTimeWindow(Duration.ofMinutes(60))
        .build()
    );
    assertThat(result).hasSize(2);
  }

  private static Instant instant(String time) {
    var localTime = LocalTime.ofSecondOfDay(TimeUtils.time(time));
    return localTime.atDate(SERVICE_DATE).atZone(TIME_ZONE).toInstant();
  }
}
