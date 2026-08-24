package org.opentripplanner.transit.model.filter.transit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.time.LocalDateRange;
import org.opentripplanner.core.model.time.TimePeriod;
import org.opentripplanner.transit.api.model.FilterValues;
import org.opentripplanner.transit.api.request.TripOnServiceDateRequest;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.basic.MainAndSubMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.filter.expr.Matcher;
import org.opentripplanner.transit.model.filter.selector.FilterRequest;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;

class TripOnServiceDateMatcherFactoryTest {

  private static final TransitRepositoryForTest TEST_MODEL = TransitRepositoryForTest.of();

  private TripOnServiceDate tripOnServiceDateRut;
  private TripOnServiceDate tripOnServiceDateRut2;
  private TripOnServiceDate tripOnServiceDateAkt;
  private TripPattern patternRut;
  private TripPattern patternAkt;
  // The RUT trip runs from 10:00 to 11:00 on its service date, the AKT trip is not running at all
  private static final Instant RUT_START = Instant.parse("2024-02-22T10:00:00Z");
  private static final Instant RUT_END = Instant.parse("2024-02-22T11:00:00Z");

  private BiFunction<Trip, LocalDate, TripPattern> patternResolver;
  private Function<TripOnServiceDate, TimePeriod> runningTimeResolver;

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
      .withServiceDate(LocalDate.of(2024, 2, 23))
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
      .withServiceDate(LocalDate.of(2024, 2, 24))
      .build();

    patternRut = TransitRepositoryForTest.tripPattern(
      "pattern:rut",
      tripOnServiceDateRut.getTrip().getRoute()
    )
      .withStopPattern(TEST_MODEL.stopPattern(2))
      .build();
    patternAkt = TransitRepositoryForTest.tripPattern(
      "pattern:akt",
      tripOnServiceDateAkt.getTrip().getRoute()
    )
      .withStopPattern(TEST_MODEL.stopPattern(2))
      .build();
    patternResolver = (trip, serviceDate) -> {
      if (trip.equals(tripOnServiceDateAkt.getTrip())) {
        return patternAkt;
      }
      if (
        trip.equals(tripOnServiceDateRut.getTrip()) || trip.equals(tripOnServiceDateRut2.getTrip())
      ) {
        return patternRut;
      }
      return null;
    };
    runningTimeResolver = tripOnServiceDate ->
      tripOnServiceDate.equals(tripOnServiceDateRut) ? TimePeriod.of(RUT_START, RUT_END) : null;
  }

  @Test
  void testMatchServiceDates() {
    TripOnServiceDateRequest request = TripOnServiceDateRequest.of()
      .withIncludeServiceDates(
        FilterValues.ofRequired("serviceDates", List.of(LocalDate.of(2024, 2, 22)))
      )
      .build();

    Matcher<TripOnServiceDate> matcher = TripOnServiceDateMatcherFactory.of(
      request,
      patternResolver,
      runningTimeResolver
    );

    assertTrue(matcher.match(tripOnServiceDateRut));
    assertFalse(matcher.match(tripOnServiceDateRut2));
    assertFalse(matcher.match(tripOnServiceDateAkt));
  }

  @Test
  void testMatchServiceDateRanges() {
    TripOnServiceDateRequest request = TripOnServiceDateRequest.of()
      .withIncludeServiceDateRanges(
        FilterValues.ofRequired(
          "serviceDateRanges",
          List.of(
            LocalDateRange.ofExclusiveEnd(LocalDate.of(2024, 2, 22), LocalDate.of(2024, 2, 24))
          )
        )
      )
      .build();

    Matcher<TripOnServiceDate> matcher = TripOnServiceDateMatcherFactory.of(
      request,
      patternResolver,
      runningTimeResolver
    );

    assertTrue(matcher.match(tripOnServiceDateRut));
    assertTrue(matcher.match(tripOnServiceDateRut2));
    assertFalse(matcher.match(tripOnServiceDateAkt));
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

    Matcher<TripOnServiceDate> matcher = TripOnServiceDateMatcherFactory.of(
      request,
      patternResolver,
      runningTimeResolver
    );

    assertTrue(matcher.match(tripOnServiceDateRut));
    assertFalse(matcher.match(tripOnServiceDateRut2));
    assertFalse(matcher.match(tripOnServiceDateAkt));
  }

  @Test
  void testMatchMultipleServiceJourneyMatchers() {
    TripOnServiceDateRequest request = TripOnServiceDateRequest.of()
      .withIncludeServiceDates(
        FilterValues.ofRequired(
          "serviceDates",
          List.of(LocalDate.of(2024, 2, 22), LocalDate.of(2024, 2, 23))
        )
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

    Matcher<TripOnServiceDate> matcher = TripOnServiceDateMatcherFactory.of(
      request,
      patternResolver,
      runningTimeResolver
    );

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

    Matcher<TripOnServiceDate> matcher = TripOnServiceDateMatcherFactory.of(
      request,
      patternResolver,
      runningTimeResolver
    );

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
    Matcher<TripOnServiceDate> matcher = TripOnServiceDateMatcherFactory.of(
      request,
      patternResolver,
      runningTimeResolver
    );

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
    Matcher<TripOnServiceDate> matcher = TripOnServiceDateMatcherFactory.of(
      request,
      patternResolver,
      runningTimeResolver
    );

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
    Matcher<TripOnServiceDate> matcher = TripOnServiceDateMatcherFactory.of(
      request,
      patternResolver,
      runningTimeResolver
    );

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
    Matcher<TripOnServiceDate> matcher = TripOnServiceDateMatcherFactory.of(
      request,
      patternResolver,
      runningTimeResolver
    );

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
    Matcher<TripOnServiceDate> matcher = TripOnServiceDateMatcherFactory.of(
      request,
      patternResolver,
      runningTimeResolver
    );

    assertTrue(matcher.match(tripOnServiceDateRut));
    assertFalse(matcher.match(tripOnServiceDateRut2));
    assertTrue(matcher.match(tripOnServiceDateAkt));
  }

  @Test
  void testMatchPatterns() {
    TripOnServiceDateRequest request = TripOnServiceDateRequest.of()
      .withIncludePatterns(
        FilterValues.ofEmptyIsEverything("patterns", List.of(patternRut.getId()))
      )
      .build();

    Matcher<TripOnServiceDate> matcher = TripOnServiceDateMatcherFactory.of(
      request,
      patternResolver,
      runningTimeResolver
    );

    assertTrue(matcher.match(tripOnServiceDateRut));
    assertTrue(matcher.match(tripOnServiceDateRut2));
    assertFalse(matcher.match(tripOnServiceDateAkt));
  }

  @Test
  void testMatchPatternsExcludesUnresolvedPattern() {
    TripOnServiceDateRequest request = TripOnServiceDateRequest.of()
      .withIncludePatterns(
        FilterValues.ofEmptyIsEverything("patterns", List.of(patternRut.getId()))
      )
      .build();

    // A resolver that cannot resolve a pattern (returns null) must not match the filter.
    Matcher<TripOnServiceDate> matcher = TripOnServiceDateMatcherFactory.of(
      request,
      (trip, serviceDate) -> null,
      tripOnServiceDate -> null
    );

    assertFalse(matcher.match(tripOnServiceDateRut));
    assertFalse(matcher.match(tripOnServiceDateRut2));
    assertFalse(matcher.match(tripOnServiceDateAkt));
  }

  @Test
  void testMatchPatternAndServiceDate() {
    TripOnServiceDateRequest request = TripOnServiceDateRequest.of()
      .withIncludePatterns(
        FilterValues.ofEmptyIsEverything("patterns", List.of(patternRut.getId()))
      )
      .withIncludeServiceDates(
        FilterValues.ofRequired("serviceDates", List.of(LocalDate.of(2024, 2, 22)))
      )
      .build();

    Matcher<TripOnServiceDate> matcher = TripOnServiceDateMatcherFactory.of(
      request,
      patternResolver,
      runningTimeResolver
    );

    // Same pattern, but only the trip on the matching service date passes.
    assertTrue(matcher.match(tripOnServiceDateRut));
    assertFalse(matcher.match(tripOnServiceDateRut2));
    assertFalse(matcher.match(tripOnServiceDateAkt));
  }

  @Test
  void matchesTripRunningInsidePeriod() {
    assertTrue(
      matcher(
        TimePeriod.of(RUT_START.minus(Duration.ofMinutes(1)), RUT_END.plus(Duration.ofMinutes(1)))
      ).match(tripOnServiceDateRut)
    );
  }

  @Test
  void matchesTripOverlappingPeriod() {
    // The trip arrives one minute before the period ends
    assertTrue(
      matcher(
        TimePeriod.of(RUT_END.minus(Duration.ofMinutes(1)), RUT_END.plus(Duration.ofHours(1)))
      ).match(tripOnServiceDateRut)
    );
    // The trip departs one minute after the period starts
    assertTrue(
      matcher(
        TimePeriod.of(RUT_START.minus(Duration.ofHours(1)), RUT_START.plus(Duration.ofMinutes(1)))
      ).match(tripOnServiceDateRut)
    );
  }

  @Test
  void doesNotMatchTripArrivingExactlyAtStartOfPeriod() {
    // The end of the running time of a trip is exclusive
    assertFalse(matcher(TimePeriod.of(RUT_END, null)).match(tripOnServiceDateRut));
  }

  @Test
  void doesNotMatchTripDepartingExactlyAtEndOfPeriod() {
    // The end of the period is exclusive
    assertFalse(matcher(TimePeriod.of(null, RUT_START)).match(tripOnServiceDateRut));
  }

  @Test
  void doesNotMatchTripOutsidePeriod() {
    assertFalse(
      matcher(TimePeriod.of(RUT_END.plusSeconds(1), RUT_END.plus(Duration.ofHours(1)))).match(
        tripOnServiceDateRut
      )
    );
  }

  @Test
  void matchesOpenEndedPeriods() {
    assertTrue(matcher(TimePeriod.ofUnbounded()).match(tripOnServiceDateRut));
  }

  @Test
  void matchesTripWithOpenEndedRunningTime() {
    // A trip whose running time has no known end is running indefinitely
    runningTimeResolver = tripOnServiceDate ->
      tripOnServiceDate.equals(tripOnServiceDateRut) ? TimePeriod.of(RUT_START, null) : null;

    assertTrue(
      matcher(TimePeriod.of(RUT_END.plus(Duration.ofHours(1)), null)).match(tripOnServiceDateRut)
    );
    assertFalse(
      matcher(TimePeriod.of(null, RUT_START.minusSeconds(1))).match(tripOnServiceDateRut)
    );
  }

  @Test
  void doesNotMatchTripWithUnresolvableRunningTime() {
    assertFalse(matcher(TimePeriod.ofUnbounded()).match(tripOnServiceDateAkt));
  }

  @Test
  void matchesSelectorRunningTimePeriods() {
    var filter = FilterRequest.<TripOnServiceDateSelectRequest>of()
      .addSelect(
        TripOnServiceDateSelectRequest.of()
          .withRunningTimePeriods(List.of(TimePeriod.of(RUT_START, RUT_END)))
          .build()
      )
      .build();
    var request = TripOnServiceDateRequest.of().withFilters(List.of(filter)).build();
    var matcher = TripOnServiceDateMatcherFactory.of(request, patternResolver, runningTimeResolver);

    assertTrue(matcher.match(tripOnServiceDateRut));
    assertFalse(matcher.match(tripOnServiceDateRut2));
    assertFalse(matcher.match(tripOnServiceDateAkt));
  }

  @Test
  void serviceDateAndRunningTimeAreAnded() {
    var request = TripOnServiceDateRequest.of()
      .withIncludeRunningTimePeriods(
        FilterValues.ofRequired("runningTimePeriods", List.of(TimePeriod.ofUnbounded()))
      )
      .withIncludeServiceDates(
        FilterValues.ofRequired("serviceDates", List.of(LocalDate.of(2024, 2, 23)))
      )
      .build();
    var matcher = TripOnServiceDateMatcherFactory.of(request, patternResolver, runningTimeResolver);

    // The running time of the trip on 2024-02-23 cannot be resolved, so it does not match
    assertFalse(matcher.match(tripOnServiceDateRut));
    assertFalse(matcher.match(tripOnServiceDateRut2));
  }

  private Matcher<TripOnServiceDate> matcher(TimePeriod period) {
    var request = TripOnServiceDateRequest.of()
      .withIncludeRunningTimePeriods(FilterValues.ofRequired("runningTimePeriods", List.of(period)))
      .build();
    return TripOnServiceDateMatcherFactory.of(request, patternResolver, runningTimeResolver);
  }
}
