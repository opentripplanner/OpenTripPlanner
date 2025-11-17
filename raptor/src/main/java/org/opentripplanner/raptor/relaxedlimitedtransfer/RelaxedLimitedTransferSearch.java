package org.opentripplanner.raptor.relaxedlimitedtransfer;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.model.RelaxFunction;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.api.request.RaptorRequest;
import org.opentripplanner.raptor.path.PathBuilder;
import org.opentripplanner.raptor.rangeraptor.transit.AccessEgressWithExtraCost;
import org.opentripplanner.raptor.rangeraptor.transit.RaptorTransitCalculator;
import org.opentripplanner.raptor.spi.BoardAndAlightTime;
import org.opentripplanner.raptor.spi.IntIterator;
import org.opentripplanner.raptor.spi.RaptorRoute;
import org.opentripplanner.raptor.spi.RaptorTransitDataProvider;
import org.opentripplanner.raptor.util.BitSetIterator;
import org.opentripplanner.raptor.util.paretoset.ParetoComparator;
import org.opentripplanner.raptor.util.paretoset.ParetoSet;

public class RelaxedLimitedTransferSearch<T extends RaptorTripSchedule> {

  private final RaptorTransitDataProvider<T> data;
  private final RaptorTransitCalculator<T> transitCalculator;
  private final int earliestDepartureTime;
  private final int latestDepartureTime;
  private final Collection<RaptorAccessEgress> accesses;
  private final Collection<RaptorAccessEgress> egresses;
  private final RelaxFunction relaxFunction;

  /* Variables used during the search (mutable) */

  private int currentRouteBoardSlack = 0;

  public RelaxedLimitedTransferSearch(
    RaptorTransitDataProvider<T> data,
    RaptorRequest<T> request,
    RaptorTransitCalculator<T> transitCalculator
  ) {
    this.data = data;
    this.transitCalculator = transitCalculator;
    this.earliestDepartureTime = request.searchParams().earliestDepartureTime();
    this.latestDepartureTime =
      earliestDepartureTime + request.searchParams().searchWindowInSeconds();
    var rltRequest = request.multiCriteria().relaxedLimitedTransferRequest();
    var disableAccessEgress = rltRequest.disableAccessEgress();
    var extraAccessEgressCostFactor = rltRequest.extraAccessEgressCostFactor();
    this.accesses = filterAndMapAccessEgress(
      request.searchParams().accessPaths(),
      disableAccessEgress,
      extraAccessEgressCostFactor
    );
    this.egresses = filterAndMapAccessEgress(
      request.searchParams().egressPaths(),
      disableAccessEgress,
      extraAccessEgressCostFactor
    );
    this.relaxFunction = rltRequest.costRelaxFunction();
  }

  public Collection<RaptorPath<T>> route() {
    var results = new ParetoSet<RaptorPath<T>>(new DestinationArrivalComparator<>(relaxFunction));

    var routes = data.routeIndexIterator(findAllAccessStopIndexes());

    while (routes.hasNext()) {
      var route = data.getRouteForIndex(routes.next());
      var paths = routeSearch(route);
      results.addAll(paths);
    }
    return results;
  }

  private static Collection<RaptorAccessEgress> filterAndMapAccessEgress(
    Collection<RaptorAccessEgress> list,
    boolean disableAccessEgress,
    double extraCostFactor
  ) {
    return list
      .stream()
      .filter(a -> !a.hasRides() && !a.hasOpeningHours())
      .filter(accessEgress -> !disableAccessEgress || accessEgress.isFree())
      .map(accessEgress ->
        (RaptorAccessEgress) new AccessEgressWithExtraCost(accessEgress, extraCostFactor)
      )
      .toList();
  }

  private IntIterator findAllAccessStopIndexes() {
    BitSet accessStopBitSet = new BitSet();
    for (RaptorAccessEgress it : accesses) {
      accessStopBitSet.set(it.stop());
    }
    return new BitSetIterator(accessStopBitSet);
  }

  /// First find ONE path for each combination of access/egress. This correspond to the
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
    var search = transitCalculator.createTripSearch(timetable);
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
    int firstScheduleIndex = transitLeg.trip().tripSortIndex();
    var access = firstPath.accessLeg().access();
    var egress = firstPath.egressLeg().egress();
    int boardPos = transitLeg.getFromStopPosition();
    int alightPos = transitLeg.getToStopPosition();
    var timetable = route.timetable();

    var results = new ArrayList<RaptorPath<T>>();
    results.add(firstPath);

    for (int i = 0; i < timetable.numberOfTripSchedules(); i++) {
      var schedule = timetable.getTripSchedule(i);
      if (schedule.tripSortIndex() <= firstScheduleIndex) {
        continue;
      }
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

  // TODO DT - Add unit tests
  static int calculateIterationDepartureTime(int accessDuration, int boardTime, int boardSlack) {
    return ((boardTime - (accessDuration + boardSlack)) / 60) * 60;
  }

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
