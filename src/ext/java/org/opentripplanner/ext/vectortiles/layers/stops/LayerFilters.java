package org.opentripplanner.ext.vectortiles.layers.stops;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.opentripplanner.apis.gtfs.model.LocalDateRange;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.service.PatternByServiceDatesFilter;
import org.opentripplanner.transit.service.TransitService;

/**
 * Predicates for filtering elements of vector tile layers. Currently only contains predicates
 * for {@link RegularStop}. Once more types need to be filtered, this may need some refactoring.
 */
public class LayerFilters {

  /**
   * No filter is applied: all stops are included in the result.
   */
  public static final Predicate<RegularStop> NO_FILTER = x -> true;

  /**
   * Returns a predicate which only includes stop which are visited by a pattern that is in the current
   * TriMet service week, namely from Sunday to Sunday.
   */
  public static Predicate<RegularStop> currentServiceWeek(
    Function<RegularStop, Collection<TripPattern>> getPatternsForStop,
    Function<Trip, Collection<LocalDate>> getServiceDatesForTrip,
    Supplier<LocalDate> nowSupplier
  ) {
    var serviceDate = nowSupplier.get();
    var lastSunday = serviceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
    var nextSunday = serviceDate.with(TemporalAdjusters.next(DayOfWeek.SUNDAY)).plusDays(1);

    var filter = new PatternByServiceDatesFilter(
      new LocalDateRange(lastSunday, nextSunday),
      // not used
      route -> List.of(),
      getServiceDatesForTrip
    );

    return regularStop -> {
      var patterns = getPatternsForStop.apply(regularStop);
      var patternsInCurrentWeek = filter.filterPatterns(patterns);
      return !patternsInCurrentWeek.isEmpty();
    };
  }

  public static Predicate<RegularStop> forType(FilterType type, TransitService transitService) {
    return switch (type) {
      case NONE -> NO_FILTER;
      case CURRENT_TRIMET_SERVICE_WEEK -> currentServiceWeek(
        transitService::getPatternsForStop,
        trip ->
          transitService.getCalendarService().getServiceDatesForServiceId(trip.getServiceId()),
        () -> LocalDate.now(transitService.getTimeZone())
      );
    };
  }

  public enum FilterType {
    NONE,
    CURRENT_TRIMET_SERVICE_WEEK,
  }
}
