package org.opentripplanner.graph_builder.module;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import jakarta.inject.Inject;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.service.TimetableRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TripPatternNamer implements GraphBuilderModule {

  private static final Logger LOG = LoggerFactory.getLogger(TripPatternNamer.class);
  private final TimetableRepository timetableRepository;

  @Inject
  public TripPatternNamer(TimetableRepository timetableRepository) {
    this.timetableRepository = timetableRepository;
  }

  @Override
  public void buildGraph() {
    /* Generate unique human-readable names for all the TableTripPatterns. */
    generateUniqueNames(timetableRepository.getAllTripPatterns());
  }

  /**
   * Static method that creates unique human-readable names for a collection of TableTripPatterns.
   * Perhaps this should be in TripPattern, and apply to Frequency patterns as well. TODO: resolve
   * this question: can a frequency and table pattern have the same stoppattern? If so should they
   * have the same "unique" name?
   * <p>
   * The names should be dataset unique, not just route-unique?
   * <p>
   * A TripPattern groups all trips visiting a particular pattern of stops on a particular route.
   * GFTS Route names are intended for very general customer information, but sometimes there is a
   * need to know where a particular trip actually goes. For example, the New York City N train has
   * at least four different variants: express (over the Manhattan bridge) and local (via lower
   * Manhattan and the tunnel), in two directions (to Astoria or to Coney Island). During
   * construction, a fifth variant sometimes appears: trains use the D line to Coney Island after
   * 59th St (or from Coney Island to 59th in the opposite direction).
   * <p>
   * TripPattern names are machine-generated on a best-effort basis. They are guaranteed to be
   * unique (among TripPatterns for a single Route) but not stable across graph builds, especially
   * when different versions of GTFS inputs are used. For instance, if a variant is the only variant
   * of the N that ends at Coney Island, the name will be "N to Coney Island". But if multiple
   * variants end at Coney Island (but have different stops elsewhere), that name would not be
   * chosen. OTP also tries start and intermediate stations ("from Coney Island", or "via
   * Whitehall", or even combinations ("from Coney Island via Whitehall"). But if there is no way to
   * create a unique name from start/end/intermediate stops, then the best we can do is to create a
   * "like [trip id]" name, which at least tells you where in the GTFS you can find a related trip.
   */
  // TODO: pass in a transit index that contains a Multimap<Route, TripPattern> and derive all TableTripPatterns
  // TODO: combine from/to and via in a single name. this could be accomplished by grouping the trips by destination,
  // then disambiguating in groups of size greater than 1.
  /*
   * Another possible approach: for each route, determine the necessity of each field (which
   * combination will create unique names). from, to, via, express. Then concatenate all necessary
   * fields. Express should really be determined from number of stops and/or run time of trips.
   */
  public static void generateUniqueNames(Collection<TripPattern> tableTripPatterns) {
    LOG.info("Generating unique names for stop patterns on each route.");

    /* Group TripPatterns by Route */
    Multimap<Route, TripPattern> patternsByRoute = ArrayListMultimap.create();
    for (TripPattern ttp : tableTripPatterns) {
      patternsByRoute.put(ttp.getRoute(), ttp);
    }

    /* Iterate over all routes, giving the patterns within each route unique names. */
    for (Route route : patternsByRoute.keySet()) {
      Collection<TripPattern> routeTripPatterns = patternsByRoute.get(route);

      // Only generate name for patterns with at least one missing name
      if (routeTripPatterns.stream().allMatch(tripPattern -> tripPattern.getName() != null)) {
        continue;
      }
      String routeName = route.getName();

      /* Simplest case: there's only one route variant, so we'll just give it the route's name. */
      if (routeTripPatterns.size() == 1) {
        routeTripPatterns.iterator().next().initName(routeName);
        continue;
      }

      /* Do the patterns within this Route have a unique start, end, or via Stop? */
      Multimap<String, TripPattern> signs = ArrayListMultimap.create(); // prefer headsigns
      Multimap<StopLocation, TripPattern> starts = ArrayListMultimap.create();
      Multimap<StopLocation, TripPattern> ends = ArrayListMultimap.create();
      Multimap<StopLocation, TripPattern> vias = ArrayListMultimap.create();

      for (TripPattern pattern : routeTripPatterns) {
        StopLocation start = pattern.firstStop();
        StopLocation end = pattern.lastStop();
        String headsign = pattern.getTripHeadsign() != null
          ? pattern.getTripHeadsign().toString()
          : null;
        if (headsign != null) {
          signs.put(headsign, pattern);
        }
        starts.put(start, pattern);
        ends.put(end, pattern);
        for (StopLocation stop : pattern.getStops()) {
          vias.put(stop, pattern);
        }
      }
      PATTERN: for (TripPattern pattern : routeTripPatterns) {
        if (pattern.getName() != null) {
          continue;
        }
        StringBuilder sb = new StringBuilder(routeName);
        String headsign = pattern.getTripHeadsign() != null
          ? pattern.getTripHeadsign().toString()
          : null;
        if (headsign != null && signs.get(headsign).size() == 1) {
          pattern.initName(sb.append(" ").append(headsign).toString());
          continue;
        }

        /* First try to name with destination. */
        var end = pattern.lastStop();
        sb.append(" to ").append(stopNameAndId(end));
        if (ends.get(end).size() == 1) {
          pattern.initName(sb.toString());
          continue; // only pattern with this last stop
        }

        /* Then try to name with origin. */
        var start = pattern.firstStop();
        sb.append(" from ").append(stopNameAndId(start));
        if (starts.get(start).size() == 1) {
          pattern.initName((sb.toString()));
          continue; // only pattern with this first stop
        }

        /* Check whether (end, start) is unique. */
        Collection<TripPattern> tripPatterns = starts.get(start);
        Set<TripPattern> remainingPatterns = new HashSet<>(tripPatterns);
        remainingPatterns.retainAll(ends.get(end)); // set intersection
        if (remainingPatterns.size() == 1) {
          pattern.initName((sb.toString()));
          continue;
        }

        /* Still not unique; try (end, start, via) for each via. */
        for (var via : pattern.getStops()) {
          if (via.equals(start) || via.equals(end)) continue;
          Set<TripPattern> intersection = new HashSet<>(remainingPatterns);
          intersection.retainAll(vias.get(via));
          if (intersection.size() == 1) {
            sb.append(" via ").append(stopNameAndId(via));
            pattern.initName((sb.toString()));
            continue PATTERN;
          }
        }

        /* Still not unique; check for express. */
        if (remainingPatterns.size() == 2) {
          // There are exactly two patterns sharing this start/end.
          // The current one must be a subset of the other, because it has no unique via.
          // Therefore we call it the express.
          sb.append(" express");
        } else {
          // The final fallback: reference a specific trip ID.
          Optional.ofNullable(pattern.getScheduledTimetable().getRepresentativeTripTimes())
            .map(TripTimes::getTrip)
            .ifPresent(value -> sb.append(" like trip ").append(value.getId()));
        }
        pattern.initName((sb.toString()));
      } // END foreach PATTERN
    } // END foreach ROUTE

    if (LOG.isDebugEnabled()) {
      LOG.debug("Done generating unique names for stop patterns on each route.");
      for (Route route : patternsByRoute.keySet()) {
        Collection<TripPattern> routeTripPatterns = patternsByRoute.get(route);
        LOG.debug("Named {} patterns in route {}", routeTripPatterns.size(), route.getName());
        for (TripPattern pattern : routeTripPatterns) {
          LOG.debug("    {} ({} stops)", pattern.getName(), pattern.numberOfStops());
        }
      }
    }
  }

  private static String stopNameAndId(StopLocation stop) {
    return stop.getName() + " (" + stop.getId().toString() + ")";
  }
}
