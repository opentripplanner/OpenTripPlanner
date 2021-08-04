package org.opentripplanner.transit.raptor._data.transit;

import static org.opentripplanner.model.transfer.Transfer.MAX_WAIT_TIME_NOT_SET;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import lombok.val;
import org.opentripplanner.model.transfer.Transfer;
import org.opentripplanner.model.transfer.TransferPriority;
import org.opentripplanner.routing.algorithm.transferoptimization.model.TripStopTime;
import org.opentripplanner.routing.algorithm.transferoptimization.services.TransferServiceAdaptor;
import org.opentripplanner.transit.raptor._data.debug.TestDebugLogger;
import org.opentripplanner.transit.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.transit.raptor.api.transit.IntIterator;
import org.opentripplanner.transit.raptor.api.transit.RaptorRoute;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransitDataProvider;

@SuppressWarnings("UnusedReturnValue")
public class TestTransitData implements RaptorTransitDataProvider<TestTripSchedule> {

  private final List<List<RaptorTransfer>> transfersByStop = new ArrayList<>();
  private final List<Set<TestRoute>> routesByStop = new ArrayList<>();
  private final List<TestRoute> routes = new ArrayList<>();
  private final List<Transfer> guaranteedTransfers = new ArrayList<>();

  @Override
  public Iterator<? extends RaptorTransfer> getTransfers(int fromStop) {
    return transfersByStop.get(fromStop).iterator();
  }

  @Override
  public Iterator<? extends RaptorRoute<TestTripSchedule>> routeIterator(IntIterator stops) {
    Set<RaptorRoute<TestTripSchedule>> routes = new HashSet<>();
    while (stops.hasNext()) {
      int stop = stops.next();
      routes.addAll(routesByStop.get(stop));
    }
    return routes.iterator();
  }

  @Override
  public int numberOfStops() {
    return routesByStop.size();
  }

  @Override
  public int[] stopBoarAlightCost() {
    // Not implemented, no test for this yet.
    return null;
  }

  public TestRoute getRoute(int index) {
    return routes.get(index);
  }

  public void debugToStdErr(RaptorRequestBuilder<TestTripSchedule> request) {
    var debug = request.debug();

    if(debug.stops().isEmpty()) {
      debug.addStops(stopsVisited());
    }
    val logger = new TestDebugLogger(true);
    debug
        .stopArrivalListener(logger::stopArrivalLister)
        .patternRideDebugListener(logger::patternRideLister)
        .pathFilteringListener(logger::pathFilteringListener)
        .logger(logger);
  }

  public TestTransitData withRoute(TestRoute route) {
    this.routes.add(route);
    var pattern = route.pattern();
    for(int i=0; i< pattern.numberOfStopsInPattern(); ++i) {
      int stopIndex = pattern.stopIndex(i);
      expandNumOfStops(stopIndex);
      routesByStop.get(stopIndex).add(route);
    }
    return this;
  }

  public TestTransitData withRoutes(TestRoute ... routes) {
    for (TestRoute route : routes) {
      withRoute(route);
    }
    return this;
  }

  public TestTransitData withTransfer(int fromStop, TestTransfer transfer) {
    expandNumOfStops(fromStop);
    transfersByStop.get(fromStop).add(transfer);
    return this;
  }

  public TestTransitData withGuaranteedTransfer(
          TestTripSchedule fromTrip, int fromStop,
          TestTripSchedule toTrip, int toStop
  ) {
    int fromStopPos = fromTrip.pattern().findStopPositionAfter(0, fromStop);
    int toStopPos = toTrip.pattern().findStopPositionAfter(0, toStop);

    for (TestRoute route : routes) {
      for (int i = 0; i < route.timetable().numberOfTripSchedules(); i++) {
        var trip = route.timetable().getTripSchedule(i);
        if(fromTrip == trip) {
          route.addGuaranteedTxFrom(trip, i, fromStopPos, toTrip, toStopPos);
        }
        if(toTrip == trip) {
          route.addGuaranteedTxTo(fromTrip, fromStopPos, trip, i, toStopPos);
        }
      }
    }
    guaranteedTransfers.add(new Transfer(
            new TestTransferPoint(fromStop, fromTrip),
            new TestTransferPoint(toStop, toTrip),
            TransferPriority.ALLOWED,
            false,
            true,
            MAX_WAIT_TIME_NOT_SET
    ));
    return this;
  }

  public Transfer findGuaranteedTransfer(
          TestTripSchedule fromTrip,
          int fromStop,
          TestTripSchedule toTrip,
          int toStop
  ) {
    for (Transfer tx : guaranteedTransfers) {
      if(
          ((TestTransferPoint)tx.getFrom()).matches(fromTrip, fromStop) &&
          ((TestTransferPoint)tx.getTo()).matches(toTrip, toStop)
      ) {
        return tx;
      }
    }
    return null;
  }

  public TransferServiceAdaptor<TestTripSchedule> transferServiceAdaptor() {
    return new TransferServiceAdaptor<>(null, null) {
      @Override protected Transfer findTransfer(
              TripStopTime<TestTripSchedule> from, TestTripSchedule toTrip, int toStop
      ) {
        return findGuaranteedTransfer(from.trip(), from.stop(), toTrip, toStop);
      }
    };
  };

  private void expandNumOfStops(int stopIndex) {
    for (int i=numberOfStops(); i<=stopIndex; ++i) {
      transfersByStop.add(new ArrayList<>());
      routesByStop.add(new HashSet<>());
    }
  }

  private List<Integer> stopsVisited() {
    final List<Integer> stops = new ArrayList<>();
    for (int i = 0; i < routesByStop.size(); i++) {
       if(!routesByStop.get(i).isEmpty()) {
         stops.add(i);
       }
    }
    return stops;
  }

}
