package org.opentripplanner.raptor.directsearch;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.api.request.RaptorRequest;
import org.opentripplanner.raptor.path.PathBuilder;
import org.opentripplanner.raptor.rangeraptor.transit.RaptorTransitCalculator;
import org.opentripplanner.raptor.spi.BoardAndAlightTime;
import org.opentripplanner.raptor.spi.RaptorRoute;
import org.opentripplanner.raptor.spi.RaptorTransitDataProvider;
import org.opentripplanner.raptor.util.BitSetIterator;
import org.opentripplanner.raptor.util.paretoset.ParetoComparator;
import org.opentripplanner.raptor.util.paretoset.ParetoSet;

public class DirectSearchService<T extends RaptorTripSchedule> {

  private final RaptorTransitDataProvider<T> data;
  private final RaptorRequest<T> request;
  private final RaptorTransitCalculator<T> calculator;

  public DirectSearchService(
    RaptorTransitDataProvider<T> data,
    RaptorRequest<T> request,
    RaptorTransitCalculator<T> calculator
  ) {
    this.data = data;
    this.calculator = calculator;
    this.request = request;
  }

  public Collection<RaptorPath<T>> route() {
    // TODO DT - search direction

    var params = request.searchParams();
    var accesses = params.accessPaths();

    var destinationSet = new ParetoSet<RaptorPath<T>>(new DestinationArrivalComparator<>());

    BitSet accessStopBitSet = new BitSet();
    for (RaptorAccessEgress it : accesses) {
      accessStopBitSet.set(it.stop());
    }

    BitSet egressStopBitSet = new BitSet();
    for (RaptorAccessEgress it : params.egressPaths()) {
      egressStopBitSet.set(it.stop());
    }

    var accessStopIterator = new BitSetIterator(accessStopBitSet);
    var routes = data.routeIndexIterator(accessStopIterator);

    while (routes.hasNext()) {
      var routeIdx = routes.next();
      var route = data.getRouteForIndex(routeIdx);

      Map<T, List<RaptorPath<T>>> routeResults = new HashMap<>();

      for (var access : accesses) {
        var pattern = route.pattern();
        int boardPos = pattern.findStopPositionAfter(0, access.stop());
        if(boardPos == -1) {
          continue;
        }

        for (var e : params.egressPaths()) {
          int alightPos = pattern.findStopPositionAfter(boardPos + 1, e.stop());
          if (alightPos == -1) {
            continue;
          }
          var paths = mapToPaths(route, access, e, boardPos, alightPos);
          for (RaptorPath<T> path : paths) {
            routeResults.computeIfAbsent(path.accessLeg().nextLeg().asTransitLeg().trip(), key -> new ArrayList<>()).add(path);
          }
        }
      }
      for (T trip : routeResults.keySet()) {
        var paths = routeResults.get(trip);
        var path = paths.stream().min(Comparator.comparingInt(it -> it.c1()));
        if(path.isPresent()) {
          destinationSet.add(path.get());
        }
      }
    }

    return destinationSet;
  }

  private Collection<RaptorPath<T>> mapToPaths(RaptorRoute<T> route, RaptorAccessEgress access, RaptorAccessEgress egress, int boardPos, int alightPos) {
    var timetable = route.timetable();
    var edt = request.searchParams().earliestDepartureTime();
    var ldt = edt + request.searchParams().searchWindowInSeconds();
    var results = new ArrayList<RaptorPath<T>>();

    for (int scheduleIdx = 0; scheduleIdx < timetable.numberOfTripSchedules(); scheduleIdx++) {
      var schedule = timetable.getTripSchedule(scheduleIdx);
      var path = mapToPath(schedule, access, egress, boardPos, alightPos);

      if (path.endTime() > ldt) {
        break;
      }
      if (path.startTime() > edt) {
        results.add(path);
      }
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

    var earliestDepartureTime =
      (((schedule.departure(boardPos) - access.durationInSeconds()) - 59) / 60) * 60;

    var pathBuilder = PathBuilder.tailPathBuilder(
      data.slackProvider(),
      earliestDepartureTime,
      data.multiCriteriaCostCalculator(),
      null,
      null
    );
    pathBuilder.access(access);
    pathBuilder.transit(schedule, times);
    pathBuilder.egress(egress);
    return pathBuilder.build();
  }

  //private final class StreamIterator implements IntIterator {

  //  private final PrimitiveIterator.OfInt it;

  //  public StreamIterator(IntStream intStream) {
  //    this.it = intStream.iterator();
  //  }

  //  @Override
  //  public int next() {
  //    return it.nextInt();
  //  }

  //  @Override
  //  public boolean hasNext() {
  //    return it.hasNext();
  //  }
  //}

  private static class DestinationArrivalComparator<T extends RaptorTripSchedule>
    implements ParetoComparator<RaptorPath<T>> {

    private static final int COST_SLACK_FACTOR = 2;

    @Override
    public boolean leftDominanceExist(RaptorPath<T> left, RaptorPath<T> right) {
      return (
        left.startTime() > right.startTime() ||
        left.endTime() < right.endTime() ||
        left.c1() < right.c1() * COST_SLACK_FACTOR
      );
    }
  }
}
