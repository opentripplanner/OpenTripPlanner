package org.opentripplanner.raptor.direct.service;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.model.RelaxFunction;
import org.opentripplanner.raptor.api.model.SearchDirection;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.path.PathBuilder;
import org.opentripplanner.raptor.spi.BoardAndAlightTime;
import org.opentripplanner.raptor.spi.IntIterator;
import org.opentripplanner.raptor.spi.RaptorRoute;
import org.opentripplanner.raptor.spi.RaptorTransitDataProvider;
import org.opentripplanner.raptor.util.BitSetIterator;
import org.opentripplanner.raptor.util.paretoset.ParetoComparator;
import org.opentripplanner.raptor.util.paretoset.ParetoSet;

/// The direct transit search finds paths using a single transit leg, limited to a
/// specified cost window. It will find paths even if they are not optimal in regard to the criteria
/// in the main raptor search.
public class DirectTransitSearch<T extends RaptorTripSchedule> {

  private final int earliestDepartureTime;
  private final int latestDepartureTime;
  private final RelaxFunction relaxC1;
  private final Collection<RaptorAccessEgress> accesses;
  private final Collection<RaptorAccessEgress> egresses;
  private final RaptorTransitDataProvider<T> data;

  /* Variables used during the search (mutable) */

  private int currentRouteBoardSlack = RaptorConstants.NOT_SET;

  public DirectTransitSearch(
    int earliestDepartureTime,
    int searchWindowInSeconds,
    RelaxFunction relaxC1,
    Collection<RaptorAccessEgress> accesses,
    Collection<RaptorAccessEgress> egresses,
    RaptorTransitDataProvider<T> data
  ) {
    this.earliestDepartureTime = earliestDepartureTime;
    this.latestDepartureTime = earliestDepartureTime + searchWindowInSeconds;
    this.relaxC1 = relaxC1;
    this.accesses = accesses;
    this.egresses = egresses;
    this.data = data;
  }

  /// Run the search
  public Collection<RaptorPath<T>> route() {
    var results = ParetoSet.<RaptorPath<T>>of(new DestinationArrivalComparator<>(relaxC1));

    var routes = data.routeIndexIterator(findAllAccessStopIndexes());

    while (routes.hasNext()) {
      var route = data.getRouteForIndex(routes.next());
      var paths = routeSearch(route);
      results.addAll(paths);
    }
    return results;
  }

  private IntIterator findAllAccessStopIndexes() {
    BitSet accessStopBitSet = new BitSet();
    for (RaptorAccessEgress it : accesses) {
      accessStopBitSet.set(it.stop());
    }
    return new BitSetIterator(accessStopBitSet);
  }

  /// First find ONE path for each combination of access/egress. This corresponds to the
  /// first iteration of a RangeRaptor search. We will later expand this to all trip schedules
  /// within the search-window. All paths with the same (route, access, and egress) will have
  /// almost identical cost, so we can use this to prune the set of paths before expanding the
  /// timetable. For each route/pattern we only want the best combination of access and egress.
  private List<RaptorPath<T>> routeSearch(RaptorRoute<T> route) {
    this.currentRouteBoardSlack = data.slackProvider().boardSlack(route.pattern().slackIndex());
    RaptorPath<T> bestPath = null;

    for (var access : accesses) {
      var pattern = route.pattern();
      int boardPos = pattern.findStopPositionAfter(0, access.stop());

      if (boardPos == -1) {
        continue;
      }

      for (var egress : egresses) {
        int alightPos = pattern.findStopPositionAfter(boardPos + 1, egress.stop());

        if (alightPos == -1) {
          continue;
        }

        var pathOp = findFirstPathInSearchWindow(route, access, egress, boardPos, alightPos);

        if (pathOp.isPresent()) {
          var candidate = pathOp.get();
          if (bestPath == null || candidate.c1() < bestPath.c1()) {
            bestPath = candidate;
          }
        }
      }
    }
    this.currentRouteBoardSlack = RaptorConstants.NOT_SET;
    // Expand the best-path into all paths within the search-window
    return bestPath == null ? List.of() : findAllPathsInSearchWindow(route, bestPath);
  }

  private Optional<RaptorPath<T>> findFirstPathInSearchWindow(
    RaptorRoute<T> route,
    RaptorAccessEgress access,
    RaptorAccessEgress egress,
    int boardPos,
    int alightPos
  ) {
    var timetable = route.timetable();
    var search = timetable.tripSearch(SearchDirection.FORWARD);
    int boardTime = earliestDepartureTime + access.durationInSeconds() + currentRouteBoardSlack;

    // find the first possible trip
    var boardEvent = search.search(boardTime, boardPos);

    if (boardEvent.empty()) {
      return Optional.empty();
    }
    var path = mapToPath(boardEvent.trip(), access, egress, boardPos, alightPos);

    if (path.startTime() < earliestDepartureTime) {
      throw new IllegalStateException(
        "This should not happen. There is a mismatch between the calculated board time/" +
          "trip search and the assembly of the path."
      );
    }

    if (path.startTime() < latestDepartureTime) {
      return Optional.of(path);
    }
    return Optional.empty();
  }

  private List<RaptorPath<T>> findAllPathsInSearchWindow(
    RaptorRoute<T> route,
    RaptorPath<T> firstPath
  ) {
    var transitLeg = firstPath.accessLeg().nextLeg().asTransitLeg();
    int tripScheduleStartIndex = transitLeg.trip().tripSortIndex() + 1;
    var access = firstPath.accessLeg().access();
    var egress = firstPath.egressLeg().egress();
    int boardPos = transitLeg.getFromStopPosition();
    int alightPos = transitLeg.getToStopPosition();
    var timetable = route.timetable();

    var results = new ArrayList<RaptorPath<T>>();
    results.add(firstPath);

    for (int i = tripScheduleStartIndex; i < timetable.numberOfTripSchedules(); i++) {
      var schedule = timetable.getTripSchedule(i);
      var path = mapToPath(schedule, access, egress, boardPos, alightPos);

      // We only need to check the end of the search-window, since we know the {@code firstPath} is
      // inside. All successive schedules will therefore also be after the
      // {@code earliestDepartureTime}.
      if (path.startTime() > latestDepartureTime) {
        return results;
      }
      results.add(path);
    }
    return results;
  }

  private RaptorPath<T> mapToPath(
    T schedule,
    RaptorAccessEgress access,
    RaptorAccessEgress egress,
    int boardPos,
    int alightPos
  ) {
    var times = new BoardAndAlightTime(schedule, boardPos, alightPos);

    // This is the range-raptor iteration start time, this is required meta-info
    var iterationDepartureTime = calculateIterationDepartureTime(
      access.durationInSeconds(),
      schedule.departure(boardPos),
      currentRouteBoardSlack
    );

    var pathBuilder = PathBuilder.tailPathBuilder(
      data.slackProvider(),
      iterationDepartureTime,
      data.multiCriteriaCostCalculator(),
      null,
      null
    );
    pathBuilder.access(access);
    pathBuilder.transit(schedule, times);
    pathBuilder.egress(egress);
    return pathBuilder.build();
  }

  static int calculateIterationDepartureTime(int accessDuration, int boardTime, int boardSlack) {
    return ((boardTime - (accessDuration + boardSlack)) / 60) * 60;
  }

  /// This comparator uses a relax function on the cost to decide if a path dominates another.
  private static class DestinationArrivalComparator<T extends RaptorTripSchedule>
    implements ParetoComparator<RaptorPath<T>> {

    private final RelaxFunction relaxFunction;

    public DestinationArrivalComparator(RelaxFunction relaxFunction) {
      this.relaxFunction = relaxFunction;
    }

    @Override
    public boolean leftDominanceExist(RaptorPath<T> left, RaptorPath<T> right) {
      return (
        left.startTime() > right.startTime() ||
        left.endTime() < right.endTime() ||
        left.c1() < relaxFunction.relax(right.c1())
      );
    }
  }
}
