package org.opentripplanner.transit.model.filter.transit;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.api.model.FilterValues;
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
    tripOnServiceDateRut = TripOnServiceDate.of(new FeedScopedId("F", "RUT:route:trip:date:1"))
      .withTrip(
        Trip.of(new FeedScopedId("F", "RUT:route:trip:1"))
          .withRoute(
            Route.of(new FeedScopedId("F", "RUT:route:1"))
              .withAgency(
                Agency.of(new FeedScopedId("F", "RUT:1"))
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

    tripOnServiceDateRut2 = TripOnServiceDate.of(new FeedScopedId("F", "RUT:route:trip:date:2"))
      .withTrip(
        Trip.of(new FeedScopedId("F", "RUT:route:trip:2"))
          .withRoute(
            Route.of(new FeedScopedId("F", "RUT:route:2"))
              .withAgency(
                Agency.of(new FeedScopedId("F", "RUT:2"))
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

    tripOnServiceDateAkt = TripOnServiceDate.of(new FeedScopedId("F", "AKT:route:trip:date:1"))
      .withTrip(
        Trip.of(new FeedScopedId("F", "AKT:route:trip:1"))
          .withRoute(
            Route.of(new FeedScopedId("F", "AKT:route:1"))
              .withAgency(
                Agency.of(new FeedScopedId("F", "AKT:1"))
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
    TripOnServiceDateRequest request = TripOnServiceDateRequest.of(
      FilterValues.ofRequired("serviceDates", List.of(LocalDate.of(2024, 2, 22)))
    ).build();

    Matcher<TripOnServiceDate> matcher = TripOnServiceDateMatcherFactory.of(request);

    assertTrue(matcher.match(tripOnServiceDateRut));
    assertTrue(matcher.match(tripOnServiceDateRut2));
    assertTrue(matcher.match(tripOnServiceDateAkt));
  }

  @Test
  void testMatchMultiple() {
    TripOnServiceDateRequest request = TripOnServiceDateRequest.of(
      FilterValues.ofRequired("serviceDates", List.of(LocalDate.of(2024, 2, 22)))
    )
      .withAgencies(
        FilterValues.ofEmptyIsEverything("agencies", List.of(new FeedScopedId("F", "RUT:1")))
      )
      .withRoutes(
        FilterValues.ofEmptyIsEverything("routes", List.of(new FeedScopedId("F", "RUT:route:1")))
      )
      .withServiceJourneys(
        FilterValues.ofEmptyIsEverything(
          "serviceJourneys",
          List.of(new FeedScopedId("F", "RUT:route:trip:1"))
        )
      )
      .build();

    Matcher<TripOnServiceDate> matcher = TripOnServiceDateMatcherFactory.of(request);

    assertTrue(matcher.match(tripOnServiceDateRut));
    assertFalse(matcher.match(tripOnServiceDateRut2));
    assertFalse(matcher.match(tripOnServiceDateAkt));
  }

  @Test
  void testMatchMultipleServiceJourneyMatchers() {
    TripOnServiceDateRequest request = TripOnServiceDateRequest.of(
      FilterValues.ofRequired("serviceDates", List.of(LocalDate.of(2024, 2, 22)))
    )
      .withAgencies(
        FilterValues.ofEmptyIsEverything(
          "agencies",
          List.of(new FeedScopedId("F", "RUT:1"), new FeedScopedId("F", "RUT:2"))
        )
      )
      .withRoutes(
        FilterValues.ofEmptyIsEverything(
          "routes",
          List.of(new FeedScopedId("F", "RUT:route:1"), new FeedScopedId("F", "RUT:route:2"))
        )
      )
      .withServiceJourneys(
        FilterValues.ofEmptyIsEverything(
          "serviceJourneys",
          List.of(
            new FeedScopedId("F", "RUT:route:trip:1"),
            new FeedScopedId("F", "RUT:route:trip:2")
          )
        )
      )
      .build();

    Matcher<TripOnServiceDate> matcher = TripOnServiceDateMatcherFactory.of(request);

    assertTrue(matcher.match(tripOnServiceDateRut));
    assertTrue(matcher.match(tripOnServiceDateRut2));
    assertFalse(matcher.match(tripOnServiceDateAkt));
  }
}
