package org.opentripplanner.raptor.directsearch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.PrimitiveIterator;
import java.util.stream.IntStream;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.api.request.RaptorRequest;
import org.opentripplanner.raptor.path.PathBuilder;
import org.opentripplanner.raptor.rangeraptor.internalapi.RaptorRouter;
import org.opentripplanner.raptor.rangeraptor.transit.RaptorTransitCalculator;
import org.opentripplanner.raptor.spi.BoardAndAlightTime;
import org.opentripplanner.raptor.spi.IntIterator;
import org.opentripplanner.raptor.spi.RaptorRoute;
import org.opentripplanner.raptor.spi.RaptorTransitDataProvider;
import org.opentripplanner.raptor.util.IntIterators;
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

    for (var access: accesses) {
      var origin = access.stop();
      var routesFromOrigin = data.routeIndexIterator(IntIterators.singleValueIterator(origin));

      while (routesFromOrigin.hasNext()) {
        var routeIdx = routesFromOrigin.next();
        var route = data.getRouteForIndex(routeIdx);
        var pattern = route.pattern();

        var boardPos = pattern.findStopPositionAfter(0, origin);

        // TODO: this is inefficient
        var alightPos = -1;
        RaptorAccessEgress egress = null;
        for (var e: params.egressPaths()) {
          alightPos = pattern.findStopPositionAfter(boardPos + 1, e.stop());
          if (alightPos != -1) {
            // TODO: This might not be the best position
            egress = e;
            break;
          }
        }

        if (alightPos != -1) {
          // Next route
          continue;
        }

        var timetable = route.timetable();
        var edt = request.searchParams().earliestDepartureTime();
        var ldt = edt + request.searchParams().searchWindowInSeconds();

        for (int scheduleIdx = 0; scheduleIdx < timetable.numberOfTripSchedules(); scheduleIdx++) {
          var schedule = timetable.getTripSchedule(scheduleIdx);
          var path = mapToPath(
            route,
            schedule,
            access,
            egress,
            boardPos,
            alightPos
          );
          if (path.endTime() > ldt) {
            break;
          }
          if (path.startTime() > edt) {
            destinationSet.add(path);
          }
        }
      }
    }

    return destinationSet;
  }

  private RaptorPath<T> mapToPath(RaptorRoute<T> route, T schedule, RaptorAccessEgress access, RaptorAccessEgress egress, int boardPos, int alightPos) {
    var times = new BoardAndAlightTime(schedule, boardPos, alightPos);

    var earliestDepartureTime = 0;

    var pathBuilder = PathBuilder.headPathBuilder(
      data.slackProvider(), earliestDepartureTime, data.multiCriteriaCostCalculator(), null, null
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

  private static class DestinationArrivalComparator<T extends RaptorTripSchedule> implements ParetoComparator<RaptorPath<T>> {
    private final static int COST_SLACK_FACTOR = 2;
    @Override
    public boolean leftDominanceExist(RaptorPath<T> left, RaptorPath<T> right) {
      return left.startTime() > right.startTime() || left.endTime() < right.endTime() || left.c1() < right.c1() * COST_SLACK_FACTOR;
    }
  }
}
