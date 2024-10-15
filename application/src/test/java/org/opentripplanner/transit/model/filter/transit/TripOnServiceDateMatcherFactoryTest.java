package org.opentripplanner.transit.model.filter.transit;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.api.request.TripOnServiceDateRequest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.filter.expr.Matcher;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;

class TripOnServiceDateMatcherFactoryTest {

  private TripOnServiceDate tripOnServiceDateRut;
  private TripOnServiceDate tripOnServiceDateRut2;
  private TripOnServiceDate tripOnServiceDateAkt;

  @BeforeEach
  void setup() {
    tripOnServiceDateRut =
      TripOnServiceDate
        .of(new FeedScopedId("RUT:route:trip:date", "123"))
        .withTrip(
          Trip
            .of(new FeedScopedId("RUT:route:trip", "1"))
            .withRoute(
              Route
                .of(new FeedScopedId("RUT:route", "2"))
                .withAgency(
                  Agency
                    .of(new FeedScopedId("RUT", "3"))
                    .withName("RUT")
                    .withTimezone("Europe/Oslo")
                    .build()
                )
                .withMode(TransitMode.BUS)
                .withShortName("BUS")
                .build()
            )
            .build()
        )
        .withServiceDate(LocalDate.of(2024, 2, 22))
        .build();

    tripOnServiceDateRut2 =
      TripOnServiceDate
        .of(new FeedScopedId("RUT:route:trip:date", "123"))
        .withTrip(
          Trip
            .of(new FeedScopedId("RUT:route:trip2", "1"))
            .withRoute(
              Route
                .of(new FeedScopedId("RUT:route", "2"))
                .withAgency(
                  Agency
                    .of(new FeedScopedId("RUT", "3"))
                    .withName("RUT")
                    .withTimezone("Europe/Oslo")
                    .build()
                )
                .withMode(TransitMode.BUS)
                .withShortName("BUS")
                .build()
            )
            .build()
        )
        .withServiceDate(LocalDate.of(2024, 2, 22))
        .build();

    tripOnServiceDateAkt =
      TripOnServiceDate
        .of(new FeedScopedId("AKT:route:trip:date", "123"))
        .withTrip(
          Trip
            .of(new FeedScopedId("AKT:route:trip", "1"))
            .withRoute(
              Route
                .of(new FeedScopedId("AKT:route", "2"))
                .withAgency(
                  Agency
                    .of(new FeedScopedId("AKT", "3"))
                    .withName("AKT")
                    .withTimezone("Europe/Oslo")
                    .build()
                )
                .withMode(TransitMode.BUS)
                .withShortName("BUS")
                .build()
            )
            .build()
        )
        .withServiceDate(LocalDate.of(2024, 2, 22))
        .build();
  }

  @Test
  void testMatchOperatingDays() {
    TripOnServiceDateRequest request = TripOnServiceDateRequest
      .of()
      .withOperatingDays(List.of(LocalDate.of(2024, 2, 22)))
      .build();

    Matcher<TripOnServiceDate> matcher = TripOnServiceDateMatcherFactory.of(request);

    assertTrue(matcher.match(tripOnServiceDateRut));
    assertTrue(matcher.match(tripOnServiceDateRut2));
    assertTrue(matcher.match(tripOnServiceDateAkt));
  }

  @Test
  void testMatchMultiple() {
    TripOnServiceDateRequest request = TripOnServiceDateRequest
      .of()
      .withOperatingDays(List.of(LocalDate.of(2024, 2, 22)))
      .withAuthorities(List.of(new FeedScopedId("RUT", "3")))
      .withLines(List.of(new FeedScopedId("RUT:route", "2")))
      .withServiceJourneys(List.of(new FeedScopedId("RUT:route:trip", "1")))
      .build();

    Matcher<TripOnServiceDate> matcher = TripOnServiceDateMatcherFactory.of(request);

    assertTrue(matcher.match(tripOnServiceDateRut));
    assertFalse(matcher.match(tripOnServiceDateRut2));
    assertFalse(matcher.match(tripOnServiceDateAkt));
  }

  @Test
  void testMatchMultipleServiceJourneyMatchers() {
    TripOnServiceDateRequest request = TripOnServiceDateRequest
      .of()
      .withOperatingDays(List.of(LocalDate.of(2024, 2, 22)))
      .withAuthorities(List.of(new FeedScopedId("RUT", "3")))
      .withLines(List.of(new FeedScopedId("RUT:route", "2")))
      .withServiceJourneys(
        List.of(new FeedScopedId("RUT:route:trip", "1"), new FeedScopedId("RUT:route:trip2", "1"))
      )
      .build();

    Matcher<TripOnServiceDate> matcher = TripOnServiceDateMatcherFactory.of(request);

    assertTrue(matcher.match(tripOnServiceDateRut));
    assertTrue(matcher.match(tripOnServiceDateRut2));
    assertFalse(matcher.match(tripOnServiceDateAkt));
  }
}
