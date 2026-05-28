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
  private TripOnServiceDate tripOnServiceDateRutRail;

  private static final Agency RUT_AGENCY = Agency.of(id("RUT:1"))
    .withName("RUT")
    .withTimezone("Europe/Oslo")
    .build();
  private static final Agency RUT2_AGENCY = Agency.of(id("RUT:2"))
    .withName("RUT")
    .withTimezone("Europe/Oslo")
    .build();
  private static final Agency AKT_AGENCY = Agency.of(id("AKT:1"))
    .withName("AKT")
    .withTimezone("Europe/Oslo")
    .build();

  @BeforeEach
  void setup() {
    tripOnServiceDateRut = TripOnServiceDate.of(id("RUT:route:trip:date:1"))
      .withTrip(
        Trip.of(id("RUT:route:trip:1"))
          .withRoute(
            Route.of(id("RUT:route:1"))
              .withAgency(RUT_AGENCY)
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
              .withAgency(RUT2_AGENCY)
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
              .withAgency(AKT_AGENCY)
              .withMode(TransitMode.BUS)
              .withShortName("BUS")
              .build()
          )
          .build()
      )
      .withServiceDate(LocalDate.of(2024, 2, 22))
      .build();

    tripOnServiceDateRutRail = TripOnServiceDate.of(id("RUT:route:trip:date:3"))
      .withTrip(
        Trip.of(id("RUT:route:trip:3"))
          .withRoute(
            Route.of(id("RUT:route:rail:1"))
              .withAgency(RUT_AGENCY)
              .withMode(TransitMode.RAIL)
              .withShortName("RAIL")
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
    assertTrue(matcher.match(tripOnServiceDateRutRail));
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
    assertFalse(matcher.match(tripOnServiceDateRutRail));
  }

  @Test
  void compositeFilterSelectByAgency() {
    // Agency RUT:1 has both a BUS trip and a RAIL trip — both should pass
    var filter = FilterRequest.<TripOnServiceDateSelectRequest>of()
      .addSelect(TripOnServiceDateSelectRequest.of().withAgencies(List.of(id("RUT:1"))).build())
      .build();
    var request = TripOnServiceDateRequest.of().withFilters(List.of(filter)).build();
    Matcher<TripOnServiceDate> matcher = TripOnServiceDateMatcherFactory.of(request);

    assertTrue(matcher.match(tripOnServiceDateRut));
    assertFalse(matcher.match(tripOnServiceDateRut2));
    assertFalse(matcher.match(tripOnServiceDateAkt));
    assertTrue(matcher.match(tripOnServiceDateRutRail));
  }

  @Test
  void compositeFilterNotByAgency() {
    // NOT RUT:1 excludes both the BUS and RAIL trips from that agency
    var filter = FilterRequest.<TripOnServiceDateSelectRequest>of()
      .addNot(TripOnServiceDateSelectRequest.of().withAgencies(List.of(id("RUT:1"))).build())
      .build();
    var request = TripOnServiceDateRequest.of().withFilters(List.of(filter)).build();
    Matcher<TripOnServiceDate> matcher = TripOnServiceDateMatcherFactory.of(request);

    assertFalse(matcher.match(tripOnServiceDateRut));
    assertTrue(matcher.match(tripOnServiceDateRut2));
    assertTrue(matcher.match(tripOnServiceDateAkt));
    assertFalse(matcher.match(tripOnServiceDateRutRail));
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
    assertTrue(matcher.match(tripOnServiceDateRutRail));
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
    // tripOnServiceDateRutRail is also agency RUT:1, not excluded by the AKT:1 not-selector
    assertTrue(matcher.match(tripOnServiceDateRutRail));
  }

  @Test
  void compositeFilterSelectByRoute() {
    var filter = FilterRequest.<TripOnServiceDateSelectRequest>of()
      .addSelect(TripOnServiceDateSelectRequest.of().withRoutes(List.of(id("RUT:route:1"))).build())
      .build();
    var request = TripOnServiceDateRequest.of().withFilters(List.of(filter)).build();
    Matcher<TripOnServiceDate> matcher = TripOnServiceDateMatcherFactory.of(request);

    assertTrue(matcher.match(tripOnServiceDateRut));
    assertFalse(matcher.match(tripOnServiceDateRut2));
    assertFalse(matcher.match(tripOnServiceDateAkt));
    // tripOnServiceDateRutRail has a different route (RUT:route:rail:1)
    assertFalse(matcher.match(tripOnServiceDateRutRail));
  }

  @Test
  void compositeFilterSelectorCriteriaAreAnded() {
    // A selector with both agency and route requires both to match (AND logic within selector).
    // RUT:1 has route RUT:route:1, so a selector for RUT:1 + RUT:route:2 should match nothing.
    var filter = FilterRequest.<TripOnServiceDateSelectRequest>of()
      .addSelect(
        TripOnServiceDateSelectRequest.of()
          .withAgencies(List.of(id("RUT:1")))
          .withRoutes(List.of(id("RUT:route:2")))
          .build()
      )
      .build();
    var request = TripOnServiceDateRequest.of().withFilters(List.of(filter)).build();
    Matcher<TripOnServiceDate> matcher = TripOnServiceDateMatcherFactory.of(request);

    assertFalse(matcher.match(tripOnServiceDateRut));
    assertFalse(matcher.match(tripOnServiceDateRut2));
    assertFalse(matcher.match(tripOnServiceDateAkt));
    assertFalse(matcher.match(tripOnServiceDateRutRail));
  }

  @Test
  void compositeFilterSelectByMode() {
    // Select BUS: three BUS trips pass, the RAIL trip does not
    var filter = FilterRequest.<TripOnServiceDateSelectRequest>of()
      .addSelect(
        TripOnServiceDateSelectRequest.of()
          .withTransportModes(List.of(new MainAndSubMode(TransitMode.BUS)))
          .build()
      )
      .build();
    var request = TripOnServiceDateRequest.of().withFilters(List.of(filter)).build();
    Matcher<TripOnServiceDate> matcher = TripOnServiceDateMatcherFactory.of(request);

    assertTrue(matcher.match(tripOnServiceDateRut));
    assertTrue(matcher.match(tripOnServiceDateRut2));
    assertTrue(matcher.match(tripOnServiceDateAkt));
    assertFalse(matcher.match(tripOnServiceDateRutRail));
  }

  @Test
  void compositeFilterNotByMode() {
    // NOT BUS: only the RAIL trip passes
    var filter = FilterRequest.<TripOnServiceDateSelectRequest>of()
      .addNot(
        TripOnServiceDateSelectRequest.of()
          .withTransportModes(List.of(new MainAndSubMode(TransitMode.BUS)))
          .build()
      )
      .build();
    var request = TripOnServiceDateRequest.of().withFilters(List.of(filter)).build();
    Matcher<TripOnServiceDate> matcher = TripOnServiceDateMatcherFactory.of(request);

    assertFalse(matcher.match(tripOnServiceDateRut));
    assertFalse(matcher.match(tripOnServiceDateRut2));
    assertFalse(matcher.match(tripOnServiceDateAkt));
    assertTrue(matcher.match(tripOnServiceDateRutRail));
  }

  @Test
  void compositeFilterSelectorCombinesAgencyAndMode() {
    // AND within a selector: agency RUT:1 AND mode RAIL matches only the RUT rail trip.
    // The BUS trip from RUT:1 fails the mode check; the RAIL trip from other agencies fails agency.
    var filter = FilterRequest.<TripOnServiceDateSelectRequest>of()
      .addSelect(
        TripOnServiceDateSelectRequest.of()
          .withAgencies(List.of(id("RUT:1")))
          .withTransportModes(List.of(new MainAndSubMode(TransitMode.RAIL)))
          .build()
      )
      .build();
    var request = TripOnServiceDateRequest.of().withFilters(List.of(filter)).build();
    Matcher<TripOnServiceDate> matcher = TripOnServiceDateMatcherFactory.of(request);

    assertFalse(matcher.match(tripOnServiceDateRut));
    assertFalse(matcher.match(tripOnServiceDateRut2));
    assertFalse(matcher.match(tripOnServiceDateAkt));
    assertTrue(matcher.match(tripOnServiceDateRutRail));
  }

  @Test
  void compositeFilterSelectByModeOrAgency() {
    // OR between two selectors: RAIL mode OR agency AKT:1
    // Matches the RAIL trip (mode) and the AKT BUS trip (agency), but not the two RUT BUS trips
    var filter = FilterRequest.<TripOnServiceDateSelectRequest>of()
      .addSelect(
        TripOnServiceDateSelectRequest.of()
          .withTransportModes(List.of(new MainAndSubMode(TransitMode.RAIL)))
          .build()
      )
      .addSelect(TripOnServiceDateSelectRequest.of().withAgencies(List.of(id("AKT:1"))).build())
      .build();
    var request = TripOnServiceDateRequest.of().withFilters(List.of(filter)).build();
    Matcher<TripOnServiceDate> matcher = TripOnServiceDateMatcherFactory.of(request);

    assertFalse(matcher.match(tripOnServiceDateRut));
    assertFalse(matcher.match(tripOnServiceDateRut2));
    assertTrue(matcher.match(tripOnServiceDateAkt));
    assertTrue(matcher.match(tripOnServiceDateRutRail));
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
    assertTrue(matcher.match(tripOnServiceDateRutRail));
  }
}
