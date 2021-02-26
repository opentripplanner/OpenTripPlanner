package org.opentripplanner.routing.algorithm.transferoptimization.services;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.transfer.Transfer;
import org.opentripplanner.model.transfer.TransferService;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.routing.algorithm.transferoptimization.api.OptimizedPath;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.path.PathLeg;
import org.opentripplanner.transit.raptor.api.path.TransitPathLeg;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;


public class PriorityBasedTransfersCostCalculator<T extends RaptorTripSchedule> {
  private final IntFunction<Stop> stopLookup;
  private final TransferService transferService;

  public PriorityBasedTransfersCostCalculator(
      IntFunction<Stop> stopLookup,
      TransferService transferService
  ) {
    this.stopLookup = stopLookup;
    this.transferService = transferService;
  }

  public OptimizedPath<T> decorateWithTransfers(OptimizedPath<T> path) {
    var legs = path
            .legStream()
            .filter(PathLeg::isTransitLeg)
            .map(PathLeg::asTransitLeg)
            .collect(Collectors.toList());

    Map<PathLeg<T>, Transfer> transfers = new HashMap<>();
    for (int i=1; i< legs.size(); ++i) {
      var from = legs.get(i - 1);
      var to = legs.get(i);
      var t = findTransfer(from, to);

      if(t != null) {
        transfers.put(to, t);
      }
    }
    return path.withTransfers(transfers);
  }


  public int cost(Path<T> path) {
    var legs = path
        .legStream()
        .filter(PathLeg::isTransitLeg)
        .map(PathLeg::asTransitLeg)
        .collect(Collectors.toList());

    int cost = 0;
    for (int i=1; i< legs.size(); ++i) {
      cost += cost(legs.get(i-1), legs.get(i));
    }
    return cost;
  }

  private int cost(TransitPathLeg<T> from, TransitPathLeg<T> to) {
    return Transfer.priorityCost(findTransfer(from, to));
  }

  private Transfer findTransfer(TransitPathLeg<T> from, TransitPathLeg<T> to) {
    return transferService.findTransfer(
            stop(from.toStop()),
            stop(to.fromStop()),
            trip(from.trip()),
            trip(to.trip()),
            from.getToStopPosition(),
            to.getFromStopPosition()
    );
  }

  private Stop stop(int index) {
    return stopLookup.apply(index);
  }

  private Trip trip(T raptorTripSchedule) {
    return ((TripSchedule)raptorTripSchedule).getOriginalTripTimes().trip;
  }
}
