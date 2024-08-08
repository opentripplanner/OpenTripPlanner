package org.opentripplanner.ext.vectortiles.layers.stops;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.function.Predicate;
import org.opentripplanner.apis.gtfs.PatternByServiceDatesFilter;
import org.opentripplanner.apis.gtfs.model.LocalDateRange;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.TransitService;

public class Predicates {

  public static final Predicate<RegularStop> NO_FILTER = x -> true;

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
}
