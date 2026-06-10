package org.opentripplanner.routing.algorithm.raptoradapter.router.startonboardaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;

import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.TripOnDateReference;
import org.opentripplanner.routing.error.InvalidRoutingInputException;
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model._data.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model._data.TripInput;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.TripAlteration;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;

class TripAndServiceDateResolverTest {

  private static final LocalDate SERVICE_DATE = LocalDate.of(2024, 11, 1);
  private static final ZoneId TIME_ZONE = ZoneId.of("GMT");

  private final TransitTestEnvironmentBuilder ENV_BUILDER = TransitTestEnvironment.of(
    SERVICE_DATE,
    TIME_ZONE
  );
  private final RegularStop STOP_A = ENV_BUILDER.stop("A");
  private final RegularStop STOP_B = ENV_BUILDER.stop("B");

  @Test
  void resolvesByTripIdAndServiceDate() {
    var env = ENV_BUILDER.addTrip(
      TripInput.of("T1").addStop(STOP_A, "10:00").addStop(STOP_B, "10:05")
    ).build();

    var reference = TripOnDateReference.ofTripIdAndServiceDate(id("T1"), SERVICE_DATE);
    var result = new TripAndServiceDateResolver(env.transitService()).resolve(reference);

    assertEquals(env.tripData("T1").trip(), result.trip());
    assertEquals(SERVICE_DATE, result.serviceDate());
  }

  @Test
  void resolvesByTripOnServiceDateId() {
    var env = ENV_BUILDER.addTrip(
      TripInput.of("T1").addStop(STOP_A, "10:00").addStop(STOP_B, "10:05")
    ).build();

    var trip = env.tripData("T1").trip();
    var tripOnServiceDate = TripOnServiceDate.of(id("TOSD-1"))
      .withTrip(trip)
      .withServiceDate(SERVICE_DATE)
      .withTripAlteration(TripAlteration.PLANNED)
      .build();
    env.timetableRepository().addTripOnServiceDate(tripOnServiceDate);
    env.timetableRepository().index();

    var reference = TripOnDateReference.ofTripOnServiceDateId(id("TOSD-1"));
    var result = new TripAndServiceDateResolver(env.transitService()).resolve(reference);

    assertEquals(trip, result.trip());
    assertEquals(SERVICE_DATE, result.serviceDate());
  }

  @Test
  void throwsOnUnknownTripId() {
    var env = ENV_BUILDER.addTrip(
      TripInput.of("T1").addStop(STOP_A, "10:00").addStop(STOP_B, "10:05")
    ).build();

    var reference = TripOnDateReference.ofTripIdAndServiceDate(id("unknown"), SERVICE_DATE);
    assertThrows(InvalidRoutingInputException.class, () ->
      new TripAndServiceDateResolver(env.transitService()).resolve(reference)
    );
  }

  @Test
  void throwsOnUnknownTripOnServiceDateId() {
    var env = ENV_BUILDER.addTrip(
      TripInput.of("T1").addStop(STOP_A, "10:00").addStop(STOP_B, "10:05")
    ).build();

    var reference = TripOnDateReference.ofTripOnServiceDateId(id("nonexistent-tosd"));
    assertThrows(InvalidRoutingInputException.class, () ->
      new TripAndServiceDateResolver(env.transitService()).resolve(reference)
    );
  }
}
