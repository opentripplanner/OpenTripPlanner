package org.opentripplanner.routing.algorithm.transferoptimization.services;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opentripplanner.routing.algorithm.transferoptimization.api.OptimizedPath;
import org.opentripplanner.routing.algorithm.transferoptimization.model.StopTime;
import org.opentripplanner.routing.algorithm.transferoptimization.model.TripStopTime;
import org.opentripplanner.routing.algorithm.transferoptimization.model.TripToTripTransfer;
import org.opentripplanner.transit.raptor.api.path.AccessPathLeg;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.path.PathLeg;
import org.opentripplanner.transit.raptor.api.path.TransferPathLeg;
import org.opentripplanner.transit.raptor.api.path.TransitPathLeg;
import org.opentripplanner.transit.raptor.api.transit.CostCalculator;
import org.opentripplanner.transit.raptor.api.transit.RaptorCostConverter;
import org.opentripplanner.transit.raptor.api.transit.RaptorSlackProvider;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;


/**
 * This class is responsible for generating all possible permutations of places to transfers for a
 * given path. For example, if a path ride 3 busses (Trip 1, 2, and 3) and there are 2 possible
 * places to transfer for each transfer. The transfer between trip 1 and 2 may take palce at stop A
 * or B, and the transfer between trip 2 and 3 may take palce at stop C or D. Then the following
 * paths are generated:
 * <pre>
 * Origin ~ Trip 1 ~ A ~ Trip 2 ~ C ~ Trip 3 ~ Destination
 * Origin ~ Trip 1 ~ B ~ Trip 2 ~ C ~ Trip 3 ~ Destination
 * Origin ~ Trip 1 ~ A ~ Trip 2 ~ D ~ Trip 3 ~ Destination
 * Origin ~ Trip 1 ~ B ~ Trip 2 ~ D ~ Trip 3 ~ Destination
 * </pre>
 */
public class TransfersPermutationService<T extends RaptorTripSchedule> {

  private final StandardTransferGenerator<T> t2tService;
  private final CostCalculator<T> costCalculator;
  private final RaptorSlackProvider slackProvider;

  public TransfersPermutationService(
      StandardTransferGenerator<T> t2tService,
      CostCalculator<T> costCalculator,
      RaptorSlackProvider slackProvider
  ) {
    this.t2tService = t2tService;
    this.costCalculator = costCalculator;
    this.slackProvider = slackProvider;
  }

  public List<OptimizedPath<T>> findAllTransitPathPermutations(Path<T> path) {
    TransitPathLeg<T> leg0 = path.accessLeg().nextTransitLeg();

    // Path has no transit legs(possible with flex access) or
    // the path have no transfers, then use the path found.
    if(leg0 == null || leg0.nextTransitLeg() == null) {
      return List.of(new OptimizedPath<T>(path, path));
    }

    // Make sure we add the proper cost/slack for FLEX access
    boolean firstTransitLeg = !path.accessLeg().access().hasRides();

    List<TransitPathLeg<T>> paths = findTransitPaths(
            path.accessLeg().toTime(),
            fromStopTime(leg0),
            leg0,
            firstTransitLeg
    );

    return paths.stream()
        .map(l -> newPath(path, l))
        .map(p -> new OptimizedPath<>(path, p))
        .collect(Collectors.toList());
  }

  private List<TransitPathLeg<T>> findTransitPaths(
      final int arrivalTime, final StopTime from, final TransitPathLeg<T> leg, boolean firstTransitLeg
  ) {
      TransitPathLeg<T> nxtLeg = leg.nextTransitLeg();

      if(nxtLeg == null) {
        // Do not allow the transfer to happen AFTER alight time/stop
        if(leg.toTime() <= from.time()) {
          return List.of();
        }
        return List.of(
            leg.mutate()
                .boardStop(from.stop(), from.time())
                .build(costCalculator, slackProvider, firstTransitLeg, arrivalTime)
        );
      }

      StopTime fromDeparture = StopTime.stopTime(leg.fromStop(), leg.fromTime());
      List<TripToTripTransfer<T>> transfers = t2tService.findTransfers(
          leg.trip(), fromDeparture, nxtLeg.trip()
      );

      if(transfers.isEmpty()) { return List.of(); }

      List<TransitPathLeg<T>> result = new ArrayList<>();

      for (TripToTripTransfer<T> tx : transfers) {
        List<PathLeg<T>> paths = createTransfers(tx, nxtLeg);

        for (PathLeg<T> next : paths) {
          // Check if the new alight time is AFTER the board time, if not ignore
          if(from.time() < tx.from().time()) {
              result.add(
                      leg.mutate()
                              .boardStop(from.stop(), from.time())
                              .newTail(tx.from().time(), next)
                              .build(costCalculator, slackProvider, firstTransitLeg, arrivalTime)
              );
          }
        }
      }
      return result;
  }

  private List<PathLeg<T>> createTransfers(TripToTripTransfer<T> tx, TransitPathLeg<T> nextLeg) {
    int departureTime = arrivalTime(tx.from());
    int arrivalTime = departureTime + tx.transferDuration();
    List<TransitPathLeg<T>> paths = findTransitPaths(arrivalTime, tx.to(), nextLeg, false);

    return paths.stream().map( p ->
        tx.sameStop()
            ? p
            : new TransferPathLeg<>(
                tx.from().stop(),
                departureTime,
                tx.to().stop(),
                arrivalTime,
                RaptorCostConverter.toOtpDomainCost(
                    costCalculator.walkCost(tx.transferDuration())
                ),
                tx.getTransfer(),
                p
            )
    ).collect(Collectors.toList());
  }

  private static <T extends RaptorTripSchedule> Path<T> newPath(Path<T> path, TransitPathLeg<T> tail) {
    AccessPathLeg<T> accessPathLeg = new AccessPathLeg<>(path.accessLeg(), tail);
    return new Path<>(path.rangeRaptorIterationDepartureTime(), accessPathLeg);
  }

  /** Trip alight time + alight slack */
  private int arrivalTime(TripStopTime<T> arrival) {
    return arrival.time() + slackProvider.alightSlack(arrival.trip().pattern());
  }

  @Nonnull
  private StopTime fromStopTime(final TransitPathLeg<T> leg) {
    return StopTime.stopTime(leg.fromStop(), leg.fromTime());
  }
}
