package org.opentripplanner.transit.api.request;

import java.time.LocalDate;
import java.util.List;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.api.model.FilterValues;
import org.opentripplanner.transit.model.basic.MainAndSubMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.filter.selector.FilterRequest;
import org.opentripplanner.transit.model.filter.transit.TripOnServiceDateSelectRequest;
import org.opentripplanner.transit.model.timetable.TripAlteration;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;

/**
 * A request for trips on certain service dates.
 * <p>
 * This request is used to retrieve {@link TripOnServiceDate}s that match the provided filter
 * values.
 */
public class TripOnServiceDateRequest {

  private final FilterValues<LocalDate> includeServiceDates;
  private final FilterValues<FeedScopedId> includeAgencies;
  private final FilterValues<FeedScopedId> includeRoutes;
  private final FilterValues<FeedScopedId> includeServiceJourneys;
  private final FilterValues<FeedScopedId> includeReplacementFor;
  private final FilterValues<String> includeNetexInternalPlanningCodes;
  private final FilterValues<TripAlteration> includeAlterations;
  private final List<FilterRequest<TripOnServiceDateSelectRequest>> filters;

  TripOnServiceDateRequest(
    FilterValues<LocalDate> includeServiceDates,
    FilterValues<FeedScopedId> includeAgencies,
    FilterValues<FeedScopedId> includeRoutes,
    FilterValues<FeedScopedId> includeServiceJourneys,
    FilterValues<FeedScopedId> includeReplacementFor,
    FilterValues<String> includeNetexInternalPlanningCodes,
    FilterValues<TripAlteration> includeAlterations,
    List<FilterRequest<TripOnServiceDateSelectRequest>> filters
  ) {
    this.includeServiceDates = includeServiceDates;
    this.includeAgencies = includeAgencies;
    this.includeRoutes = includeRoutes;
    this.includeServiceJourneys = includeServiceJourneys;
    this.includeReplacementFor = includeReplacementFor;
    this.includeNetexInternalPlanningCodes = includeNetexInternalPlanningCodes;
    this.includeAlterations = includeAlterations;
    this.filters = filters;
  }

  public static TripOnServiceDateRequestBuilder of() {
    return new TripOnServiceDateRequestBuilder();
  }

  public FilterValues<FeedScopedId> includeAgencies() {
    return includeAgencies;
  }

  public FilterValues<FeedScopedId> includeRoutes() {
    return includeRoutes;
  }

  public FilterValues<FeedScopedId> includeServiceJourneys() {
    return includeServiceJourneys;
  }

  public FilterValues<FeedScopedId> includeReplacementFor() {
    return includeReplacementFor;
  }

  public FilterValues<String> includeNetexInternalPlanningCodes() {
    return includeNetexInternalPlanningCodes;
  }

  public FilterValues<TripAlteration> includeAlterations() {
    return includeAlterations;
  }

  public FilterValues<LocalDate> includeServiceDates() {
    return includeServiceDates;
  }

  public FilterValues<TransitMode> includeModes() {
    List<TransitMode> modesToInclude = null;
    if (!filters.isEmpty()) {
      // Since we currently only support a single filter, we only use the first filter and select
      var includes = filters.getFirst().select();
      if (includes != null && !includes.getFirst().transportModes().includeEverything()) {
        modesToInclude = extractTransitModes(includes.getFirst());
      }
    }
    return FilterValues.ofNullIsEverything("modesToInclude", modesToInclude);
  }

  public FilterValues<TransitMode> excludeModes() {
    List<TransitMode> modesToExclude = null;
    if (!filters.isEmpty()) {
      // Since we currently only support a single filter, we only use the first filter and select
      var excludes = filters.getFirst().not();
      if (excludes != null && !excludes.getFirst().transportModes().includeEverything()) {
        modesToExclude = extractTransitModes(excludes.getFirst());
      }
    }
    return FilterValues.ofNullIsEverything("modesToExclude", modesToExclude);
  }

  public List<FilterRequest<TripOnServiceDateSelectRequest>> filters() {
    return filters;
  }

  private List<TransitMode> extractTransitModes(TripOnServiceDateSelectRequest selectRequest) {
    return selectRequest.transportModes().get().stream().map(MainAndSubMode::mainMode).toList();
  }
}
