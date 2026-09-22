package org.opentripplanner.transit.model.filter.transit;

import java.time.LocalDate;
import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.core.model.time.LocalDateRange;
import org.opentripplanner.core.model.time.TimePeriod;
import org.opentripplanner.model.modes.AllowTransitModeFilter;
import org.opentripplanner.transit.api.request.TripOnServiceDateRequest;
import org.opentripplanner.transit.model.basic.NarrowedTransitMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.filter.expr.ContainsMatcher;
import org.opentripplanner.transit.model.filter.expr.EqualityMatcher;
import org.opentripplanner.transit.model.filter.expr.ExpressionBuilder;
import org.opentripplanner.transit.model.filter.expr.GenericUnaryMatcher;
import org.opentripplanner.transit.model.filter.expr.Matcher;
import org.opentripplanner.transit.model.filter.selector.SelectorBasedMatcherFactory;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripAlteration;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;

/**
 * A factory for creating matchers for TripOnServiceDates.
 * <p>
 * This factory is used to create matchers for {@link TripOnServiceDate} objects based on a request.
 * The resulting matcher can be used to filter a list of TripOnServiceDate objects.
 */
public class TripOnServiceDateMatcherFactory {

  /**
   * Creates a matcher for TripOnServiceDates.
   *
   * @param request             the criteria for filtering TripOnServiceDates.
   * @param patternResolver     resolves the pattern of a trip on a service date, or {@code null} if
   *                            it has no pattern.
   * @param runningTimeResolver resolves a trip's runtime according to its schedule, from the
   *                            departure from the first stop to the arrival at the last stop.
   *                            Returns {@code null} if the running time cannot be resolved, in
   *                            which case the trip never matches a running time filter.
   * @return a matcher for filtering TripOnServiceDates.
   */
  public static Matcher<TripOnServiceDate> of(
    TripOnServiceDateRequest request,
    BiFunction<Trip, LocalDate, TripPattern> patternResolver,
    Function<TripOnServiceDate, TimePeriod> runningTimeResolver
  ) {
    ExpressionBuilder<TripOnServiceDate> expr = ExpressionBuilder.of();

    if (!request.filters().isEmpty()) {
      expr.matches(
        SelectorBasedMatcherFactory.of(request.filters(), selector ->
          buildSelectorMatcher(selector, runningTimeResolver)
        )
      );
    }

    expr.atLeastOneMatch(
      request.includeServiceDates(),
      TripOnServiceDateMatcherFactory::serviceDate
    );
    expr.atLeastOneMatch(
      request.includeServiceDateRanges(),
      TripOnServiceDateMatcherFactory::serviceDateRange
    );
    if (!request.includeRunningTimePeriods().includeEverything()) {
      expr.matches(
        runningTimePeriods(request.includeRunningTimePeriods().get(), runningTimeResolver)
      );
    }
    expr.atLeastOneMatch(request.includeAgencies(), TripOnServiceDateMatcherFactory::agencyId);
    expr.atLeastOneMatch(request.includeRoutes(), TripOnServiceDateMatcherFactory::routeId);
    expr.atLeastOneMatch(request.includePatterns(), id -> patternId(id, patternResolver));
    expr.atLeastOneMatch(
      request.includeServiceJourneys(),
      TripOnServiceDateMatcherFactory::serviceJourneyId
    );
    expr.atLeastOneMatch(
      request.includeReplacementFor(),
      TripOnServiceDateMatcherFactory::replacementFor
    );
    expr.atLeastOneMatch(
      request.includeNetexInternalPlanningCodes(),
      TripOnServiceDateMatcherFactory::netexInternalPlanningCode
    );
    expr.atLeastOneMatch(request.includeAlterations(), TripOnServiceDateMatcherFactory::alteration);
    expr.atLeastOneMatch(request.includeModes(), TripOnServiceDateMatcherFactory::mode);
    expr.matchesNone(request.excludeModes(), TripOnServiceDateMatcherFactory::mode);
    return expr.build();
  }

  /**
   * Builds a matcher from a single {@link TripOnServiceDateSelectRequest}, combining its
   * agencies, routes, and transport modes with AND logic.
   */
  private static Matcher<TripOnServiceDate> buildSelectorMatcher(
    TripOnServiceDateSelectRequest selector,
    Function<TripOnServiceDate, TimePeriod> runningTimeResolver
  ) {
    ExpressionBuilder<TripOnServiceDate> expr = ExpressionBuilder.of();

    expr.atLeastOneMatch(selector.agencies(), TripOnServiceDateMatcherFactory::agencyId);
    expr.atLeastOneMatch(selector.routes(), TripOnServiceDateMatcherFactory::routeId);
    expr.atLeastOneMatch(
      selector.serviceDateRanges(),
      TripOnServiceDateMatcherFactory::serviceDateRange
    );
    if (!selector.runningTimePeriods().includeEverything()) {
      expr.matches(runningTimePeriods(selector.runningTimePeriods().get(), runningTimeResolver));
    }

    if (!selector.transportModes().includeEverything()) {
      var transportModeFilter = AllowTransitModeFilter.of(
        selector.transportModes().get().stream().map(NarrowedTransitMode::of).toList()
      );
      expr.matches(
        new GenericUnaryMatcher<>("transportMode", (TripOnServiceDate tripTime) ->
          transportModeFilter.match(
            tripTime.getTrip().getMode(),
            tripTime.getTrip().getNetexSubMode()
          )
        )
      );
    }

    return expr.build();
  }

  static Matcher<TripOnServiceDate> agencyId(FeedScopedId id) {
    return new EqualityMatcher<>("agency", id, t -> t.getTrip().getRoute().getAgency().getId());
  }

  static Matcher<TripOnServiceDate> routeId(FeedScopedId id) {
    return new EqualityMatcher<>("route", id, t -> t.getTrip().getRoute().getId());
  }

  static Matcher<TripOnServiceDate> patternId(
    FeedScopedId id,
    BiFunction<Trip, LocalDate, TripPattern> patternResolver
  ) {
    return new EqualityMatcher<>("pattern", id, t -> {
      TripPattern pattern = patternResolver.apply(t.getTrip(), t.getServiceDate());
      return pattern == null ? null : pattern.getId();
    });
  }

  static Matcher<TripOnServiceDate> serviceJourneyId(FeedScopedId id) {
    return new EqualityMatcher<>("serviceJourney", id, t -> t.getTrip().getId());
  }

  static Matcher<TripOnServiceDate> replacementFor(FeedScopedId id) {
    return new ContainsMatcher<>(
      "replacementForContains",
      t -> t.getReplacementFor().stream().map(AbstractTransitEntity::getId).toList(),
      new EqualityMatcher<>("replacementForIdEquals", id, idToMatch -> idToMatch)
    );
  }

  static Matcher<TripOnServiceDate> netexInternalPlanningCode(String code) {
    return new EqualityMatcher<>("netexInternalPlanningCode", code, t ->
      t.getTrip().getNetexInternalPlanningCode()
    );
  }

  static Matcher<TripOnServiceDate> serviceDate(LocalDate date) {
    return new EqualityMatcher<>("serviceDate", date, TripOnServiceDate::getServiceDate);
  }

  static Matcher<TripOnServiceDate> serviceDateRange(LocalDateRange dateRange) {
    return new GenericUnaryMatcher<>("serviceDateRange", date ->
      dateRange.contains(date.getServiceDate())
    );
  }

  /**
   * Matches trips which are running during at least one of the given periods, according to their
   * schedule. A trip is running from the scheduled departure from its first stop until the
   * scheduled arrival at its last stop. Trips whose schedule cannot be resolved never match.
   * <p>
   * The running time is resolved once per trip and then tested against every period, so that the
   * (potentially expensive) {@code runningTimeResolver} is not invoked once per period.
   */
  static Matcher<TripOnServiceDate> runningTimePeriods(
    Collection<TimePeriod> periods,
    Function<TripOnServiceDate, TimePeriod> runningTimeResolver
  ) {
    return new GenericUnaryMatcher<>("runningTimePeriod", tripOnServiceDate -> {
      var runningTime = runningTimeResolver.apply(tripOnServiceDate);
      if (runningTime == null) {
        return false;
      }
      for (var period : periods) {
        if (period.overlaps(runningTime)) {
          return true;
        }
      }
      return false;
    });
  }

  static Matcher<TripOnServiceDate> alteration(TripAlteration alteration) {
    return new EqualityMatcher<>("alteration", alteration, TripOnServiceDate::getTripAlteration);
  }

  static Matcher<TripOnServiceDate> mode(TransitMode mode) {
    return new EqualityMatcher<>("mode", mode, t -> t.getTrip().getMode());
  }
}
