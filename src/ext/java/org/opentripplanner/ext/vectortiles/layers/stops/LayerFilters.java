package org.opentripplanner.ext.vectortiles.layers.stops;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.function.Predicate;
import org.opentripplanner.apis.gtfs.PatternByServiceDatesFilter;
import org.opentripplanner.apis.gtfs.model.LocalDateRange;
import org.opentripplanner.transit.model.site.RegularStop;
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
  public static Predicate<RegularStop> currentServiceWeek(TransitService transitService) {
    var serviceDate = LocalDate.now(transitService.getTimeZone());
    var lastSunday = serviceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
    var nextSunday = serviceDate.with(TemporalAdjusters.next(DayOfWeek.SUNDAY)).plusDays(1);
    var filter = new PatternByServiceDatesFilter(
      new LocalDateRange(lastSunday, nextSunday),
      transitService
    );

    return regularStop -> {
      var patterns = transitService.getPatternsForStop(regularStop);
      var patternsInCurrentWeek = filter.filterPatterns(patterns);
      return !patternsInCurrentWeek.isEmpty();
    };
  }

  public static Predicate<RegularStop> forType(FilterType type, TransitService transitService) {
    return switch (type) {
      case NONE -> NO_FILTER;
      case CURRENT_TRIMET_SERVICE_WEEK -> currentServiceWeek(transitService);
    };
  }

  public enum FilterType {
    NONE,
    CURRENT_TRIMET_SERVICE_WEEK,
  }
}
