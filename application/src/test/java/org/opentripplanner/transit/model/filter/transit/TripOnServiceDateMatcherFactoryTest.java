package org.opentripplanner.transit.model.filter.transit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.api.model.FilterValues;
import org.opentripplanner.transit.api.request.TripOnServiceDateRequest;
import org.opentripplanner.transit.model.basic.MainAndSubMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.filter.expr.Matcher;
import org.opentripplanner.transit.model.filter.selector.FilterRequest;
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
    tripOnServiceDateRut = TripOnServiceDate.of(id("RUT:route:trip:date:1"))
      .withTrip(
        Trip.of(id("RUT:route:trip:1"))
          .withRoute(
            Route.of(id("RUT:route:1"))
              .withAgency(
                Agency.of(id("RUT:1")).withName("RUT").withTimezone("Europe/Oslo").build()
              )
              .withMode(TransitMode.BUS)
              .withShortName("BUS")
              .build()
          )
          .build()
      )
      .withServiceDate(LocalDate.of(2024, 2, 22))
      .build();

    tripOnServiceDateRut2 = TripOnServiceDate.of(id("RUT:route:trip:date:2"))
      .withTrip(
        Trip.of(id("RUT:route:trip:2"))
          .withRoute(
            Route.of(id("RUT:route:2"))
              .withAgency(
                Agency.of(id("RUT:2")).withName("RUT").withTimezone("Europe/Oslo").build()
              )
              .withMode(TransitMode.BUS)
              .withShortName("BUS")
              .build()
          )
          .build()
      )
      .withServiceDate(LocalDate.of(2024, 2, 22))
      .build();

    tripOnServiceDateAkt = TripOnServiceDate.of(id("AKT:route:trip:date:1"))
      .withTrip(
        Trip.of(id("AKT:route:trip:1"))
          .withRoute(
            Route.of(id("AKT:route:1"))
              .withAgency(
                Agency.of(id("AKT:1")).withName("AKT").withTimezone("Europe/Oslo").build()
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
    TripOnServiceDateRequest request = TripOnServiceDateRequest.of()
      .withIncludeServiceDates(
        FilterValues.ofRequired("serviceDates", List.of(LocalDate.of(2024, 2, 22)))
      )
      .build();

    Matcher<TripOnServiceDate> matcher = TripOnServiceDateMatcherFactory.of(request);

    assertTrue(matcher.match(tripOnServiceDateRut));
    assertTrue(matcher.match(tripOnServiceDateRut2));
    assertTrue(matcher.match(tripOnServiceDateAkt));
  }

  @Test
  void testMatchMultiple() {
    TripOnServiceDateRequest request = TripOnServiceDateRequest.of()
      .withIncludeServiceDates(
        FilterValues.ofRequired("serviceDates", List.of(LocalDate.of(2024, 2, 22)))
      )
      .withIncludeAgencies(FilterValues.ofEmptyIsEverything("agencies", List.of(id("RUT:1"))))
      .withIncludeRoutes(FilterValues.ofEmptyIsEverything("routes", List.of(id("RUT:route:1"))))
      .withIncludeServiceJourneys(
        FilterValues.ofEmptyIsEverything("serviceJourneys", List.of(id("RUT:route:trip:1")))
      )
      .withFilters(
        List.of(
          FilterRequest.<TripOnServiceDateSelectRequest>of()
            .addSelect(
              TripOnServiceDateSelectRequest.of()
                .withTransportModes(List.of(new MainAndSubMode(TransitMode.BUS)))
                .build()
            )
            .build()
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
    TripOnServiceDateRequest request = TripOnServiceDateRequest.of()
      .withIncludeServiceDates(
        FilterValues.ofRequired("serviceDates", List.of(LocalDate.of(2024, 2, 22)))
      )
      .withIncludeAgencies(
        FilterValues.ofEmptyIsEverything("agencies", List.of(id("RUT:1"), id("RUT:2")))
      )
      .withIncludeRoutes(
        FilterValues.ofEmptyIsEverything("routes", List.of(id("RUT:route:1"), id("RUT:route:2")))
      )
      .withIncludeServiceJourneys(
        FilterValues.ofEmptyIsEverything(
          "serviceJourneys",
          List.of(id("RUT:route:trip:1"), id("RUT:route:trip:2"))
        )
      )
      .build();

    Matcher<TripOnServiceDate> matcher = TripOnServiceDateMatcherFactory.of(request);

    assertTrue(matcher.match(tripOnServiceDateRut));
    assertTrue(matcher.match(tripOnServiceDateRut2));
    assertFalse(matcher.match(tripOnServiceDateAkt));
  }

  @Test
  void testIncludeExcludeOrder() {
    // Exclude should negate include, so when same selector is applied both as select and not,
    // the not wins.
    var busSelector = TripOnServiceDateSelectRequest.of()
      .withTransportModes(List.of(new MainAndSubMode(TransitMode.BUS)))
      .build();
    var filter = FilterRequest.<TripOnServiceDateSelectRequest>of()
      .addSelect(busSelector)
      .addNot(busSelector)
      .build();
    TripOnServiceDateRequest request = TripOnServiceDateRequest.of()
      .withIncludeServiceDates(
        FilterValues.ofRequired("serviceDates", List.of(LocalDate.of(2024, 2, 22)))
      )
      .withFilters(List.of(filter))
      .build();

    Matcher<TripOnServiceDate> matcher = TripOnServiceDateMatcherFactory.of(request);

    assertFalse(matcher.match(tripOnServiceDateRut));
    assertFalse(matcher.match(tripOnServiceDateRut2));
    assertFalse(matcher.match(tripOnServiceDateAkt));
  }

  @Test
  void compositeFilterSelectByAgency() {
    var filter = FilterRequest.<TripOnServiceDateSelectRequest>of()
      .addSelect(TripOnServiceDateSelectRequest.of().withAgencies(List.of(id("RUT:1"))).build())
      .build();
    var request = TripOnServiceDateRequest.of().withFilters(List.of(filter)).build();
    Matcher<TripOnServiceDate> matcher = TripOnServiceDateMatcherFactory.of(request);

    assertTrue(matcher.match(tripOnServiceDateRut));
    assertFalse(matcher.match(tripOnServiceDateRut2));
    assertFalse(matcher.match(tripOnServiceDateAkt));
  }

  @Test
  void compositeFilterNotByAgency() {
    var filter = FilterRequest.<TripOnServiceDateSelectRequest>of()
      .addNot(TripOnServiceDateSelectRequest.of().withAgencies(List.of(id("RUT:1"))).build())
      .build();
    var request = TripOnServiceDateRequest.of().withFilters(List.of(filter)).build();
    Matcher<TripOnServiceDate> matcher = TripOnServiceDateMatcherFactory.of(request);

    assertFalse(matcher.match(tripOnServiceDateRut));
    assertTrue(matcher.match(tripOnServiceDateRut2));
    assertTrue(matcher.match(tripOnServiceDateAkt));
  }

  @Test
  void compositeFilterSelectIsOrBetweenSelectors() {
    // Two selectors in select — a trip matching either one should pass
    var filter = FilterRequest.<TripOnServiceDateSelectRequest>of()
      .addSelect(TripOnServiceDateSelectRequest.of().withAgencies(List.of(id("RUT:1"))).build())
      .addSelect(TripOnServiceDateSelectRequest.of().withAgencies(List.of(id("AKT:1"))).build())
      .build();
    var request = TripOnServiceDateRequest.of().withFilters(List.of(filter)).build();
    Matcher<TripOnServiceDate> matcher = TripOnServiceDateMatcherFactory.of(request);

    assertTrue(matcher.match(tripOnServiceDateRut));
    assertFalse(matcher.match(tripOnServiceDateRut2));
    assertTrue(matcher.match(tripOnServiceDateAkt));
  }

  @Test
  void compositeFilterNotOverridesSelect() {
    // select RUT:1 and AKT:1, but not AKT:1 — AKT:1 should be excluded despite being selected
    var filter = FilterRequest.<TripOnServiceDateSelectRequest>of()
      .addSelect(TripOnServiceDateSelectRequest.of().withAgencies(List.of(id("RUT:1"))).build())
      .addSelect(TripOnServiceDateSelectRequest.of().withAgencies(List.of(id("AKT:1"))).build())
      .addNot(TripOnServiceDateSelectRequest.of().withAgencies(List.of(id("AKT:1"))).build())
      .build();
    var request = TripOnServiceDateRequest.of().withFilters(List.of(filter)).build();
    Matcher<TripOnServiceDate> matcher = TripOnServiceDateMatcherFactory.of(request);

    assertTrue(matcher.match(tripOnServiceDateRut));
    assertFalse(matcher.match(tripOnServiceDateRut2));
    assertFalse(matcher.match(tripOnServiceDateAkt));
  }

  @Test
  void multipleFiltersAreOred() {
    // Two separate filters — a trip matching either filter should pass
    var filterRut = FilterRequest.<TripOnServiceDateSelectRequest>of()
      .addSelect(TripOnServiceDateSelectRequest.of().withAgencies(List.of(id("RUT:1"))).build())
      .build();
    var filterAkt = FilterRequest.<TripOnServiceDateSelectRequest>of()
      .addSelect(TripOnServiceDateSelectRequest.of().withAgencies(List.of(id("AKT:1"))).build())
      .build();
    var request = TripOnServiceDateRequest.of().withFilters(List.of(filterRut, filterAkt)).build();
    Matcher<TripOnServiceDate> matcher = TripOnServiceDateMatcherFactory.of(request);

    assertTrue(matcher.match(tripOnServiceDateRut));
    assertFalse(matcher.match(tripOnServiceDateRut2));
    assertTrue(matcher.match(tripOnServiceDateAkt));
  }
}
