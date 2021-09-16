package org.opentripplanner.routing.algorithm.transferoptimization.services;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import org.opentripplanner.routing.algorithm.transferoptimization.api.OptimizedPath;
import org.opentripplanner.routing.algorithm.transferoptimization.model.MinCostFilterChain;
import org.opentripplanner.routing.algorithm.transferoptimization.model.OptimizedPathTail;
import org.opentripplanner.routing.algorithm.transferoptimization.model.TripStopTime;
import org.opentripplanner.routing.algorithm.transferoptimization.model.TripToTripTransfer;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.path.PathLeg;
import org.opentripplanner.transit.raptor.api.path.TransferPathLeg;
import org.opentripplanner.transit.raptor.api.path.TransitPathLeg;
import org.opentripplanner.transit.raptor.api.transit.CostCalculator;
import org.opentripplanner.transit.raptor.api.transit.RaptorSlackProvider;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;


/**
 * This class is responsible for generating all possible permutations of a path with respect to
 * where the transfer happens and picking the best possible path. To improve performance, the paths
 * generated are pruned during construction.
 * <p>
 * <b>Performance</b>
 * <p>
 * <pre>
 * N : number of transfers in path
 * M : number of possible transfer location for a given pair of trips
 *
 * Without any pruning the permutations have an order of O(N^M), but by filtering during the path
 * construction we get close to O(N*M) - witch is acceptable.
 *
 * Example with 3 lines(trips), where the `+` indicate stop places:
 *
 * Stop:     A     B     C     D     E     F     G
 * Line 1:  + --- + --------- +
 * Line 2:        + --- + --- + --- + --- +
 * Line 3:              + --------- + --- + --- +
 *
 * In this example, all the following paths are possible from A to G:
 *
 *   A ~ L1 ~ B ~ L2 ~ C ~ L3 ~ G   (best option)
 *   A ~ L1 ~ B ~ L2 ~ E ~ L3 ~ G
 *   A ~ L1 ~ B ~ L2 ~ F ~ L3 ~ G
 *   A ~ L1 ~ D ~ L2 ~ E ~ L3 ~ G   (second best option)
 *   A ~ L1 ~ D ~ L2 ~ F ~ L3 ~ G
 *
 * However the implementation filters after finding each sub-path to generate less alternatives.
 * The construction starts from the end (tail) and works its way forward.
 *
 * 1. Find transfer between L2 and L3:
 *    1.1 When boarding L2 at D we have 2 options for the next transfer: E and F => E is best
 *    1.2 When boarding L2 at B we have 2 options for the next transfer: C and E (best of E & F) => C is best
 *    1.3 Tails found: [ L2 ~ E ~ L3 ~ G, L2 ~ C ~ L3 ~ G ]
 *
 * 2. Find transfer between L1 and L2, with 2 possible tails:
 *    1.1 Board L2 at D, only one possible tail to use: L2 ~ E ~ L3 ~ G  =>  A ~ L1 ~ D ~ L2 ~ E ~ L3 ~ G
 *    1.2 Board L2 at B, 2 possible tails, but the best is: L2 ~ C ~ L3 ~ G =>  A ~ L1 ~ B ~ L2 ~ C ~ L3 ~ G
 *    1.3 The best path is: A ~ L1 ~ B ~ L2 ~ C ~ L3 ~ G
 * </pre>
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class OptimizePathService<T extends RaptorTripSchedule> {

  private final TransferGenerator<T> transferGenerator;
  private final CostCalculator<T> costCalculator;
  private final RaptorSlackProvider slackProvider;
  private final OptimizedPathFactory<T> optimizedPathFactory;
  private final MinCostFilterChain<OptimizedPathTail<T>> minCostFilterChain;

  public OptimizePathService(
      TransferGenerator<T> transferGenerator,
      CostCalculator<T> costCalculator,
      RaptorSlackProvider slackProvider,
      ToIntFunction<PathLeg<?>> costCalcForWaitOptimization,
      MinCostFilterChain<OptimizedPathTail<T>> minCostFilterChain
  ) {
    this.costCalculator = costCalculator;
    this.slackProvider = slackProvider;
    this.transferGenerator = transferGenerator;
    this.optimizedPathFactory = new OptimizedPathFactory<>(costCalcForWaitOptimization);
    this.minCostFilterChain = minCostFilterChain;
  }

  public Set<OptimizedPath<T>> findBestTransitPath(Path<T> originalPath) {
    List<TransitPathLeg<T>> transitLegs = originalPath.transitLegs().collect(Collectors.toList());

    // Find all possible transfers between each pair of transit legs, and sort on arrival time
    var possibleTransfers = sortTransfersOnArrivalTimeInDecOrder(
            transferGenerator.findAllPossibleTransfers(transitLegs)
    );

    // Combine transit legs and transfers
    var tails = findBestTransferOption(
            originalPath, transitLegs, possibleTransfers
    );

    Set<OptimizedPath<T>> paths = new HashSet<>();
    for (OptimizedPathTail<T> tail : tails) {
      paths.add(optimizedPathFactory.createPath(originalPath, tail));
    }

    return paths;
  }

  private Set<OptimizedPathTail<T>> findBestTransferOption(
          Path<T> path,
          List<TransitPathLeg<T>> originalTransitLegs,
          List<List<TripToTripTransfer<T>>> possibleTransfers
  ) {
    // Create a set of tails with the last transit leg in it (one element)
    Set<OptimizedPathTail<T>> resultTails = Set.of(
            optimizedPathFactory.createPathLeg(last(originalTransitLegs))
    );

    // Cache accessArrivalTime, any event before the access-arrival-time is safe to ignore
    int accessArrivalTime = path.accessLeg().toTime();

    // Make sure we add the proper cost/slack for flex access
    boolean accessWithoutRides = !path.accessLeg().access().hasRides();

    for (int i = possibleTransfers.size()-1; i >=0; --i) {
      boolean firstRide = i==0 && accessWithoutRides;

      // Get the list of transfers for the current index
      List<TripToTripTransfer<T>> transfers = possibleTransfers.get(i);
      TransitPathLeg<T> originalFromTransitLeg = originalTransitLegs.get(i);

      // Find the earliest possible time we can arrive and still find a matching transfer in the
      // next iteration (looking at the boarding of the current transit leg)
      int earliestDepartureTime = i==0
              ? accessArrivalTime
              // The transfer with the earliest-arrival-time BEFORE the transit-leg is used to
              // prune the transfers AFTER the transit-leg. The transfers are sorted on
              // arrival-time in descending order, so the earliest-arrival-time is the
              // last element of the list of transfers.
              : last(possibleTransfers.get(i-1)).to().time();

      // create a tailSelector for the tails produced in the last round and use it to filter them
      // based on the transfer-arrival-time and given filter
      var tailSelector = new TransitPathLegSelector<>(minCostFilterChain, resultTails);

      // Reset the result set to an empty set
      resultTails = new HashSet<>();


      for (TripToTripTransfer<T> tx : transfers) {

        // Skip transfers happening before earliest possible board time
        if(tx.from().time() <= earliestDepartureTime) {
          continue;
        }

        // Find the best tails that are safe to board with respect to the arrival
        var candidateTails = tailSelector.next(tx.to().time());

        for (OptimizedPathTail<T> tail : candidateTails) {
          // Tail can be used with current transfer
          if (tail != null) {
            resultTails.add(
                createNewTransitLegTail(
                    firstRide,
                    originalFromTransitLeg,
                    earliestDepartureTime,
                    tx,
                    tail
                )
            );
          }
        }
      }
    }

    // Final filter
    var tailSelector = new TransitPathLegSelector<>(minCostFilterChain, resultTails);

    return tailSelector.next(path.startTime());
  }

  /**
   * Create a new {@link OptimizedPathTail} for the originalLeg with the given
   * transfer, earliest-departure-time and following leg (tail).
   *
   * Since the previous leg is not yet known, the earliest-departure-time is used instead.
   * For the first transit leg in a path the {@code earliestDepartureTime} must be set to
   * the correct values (the access-leg-arrival-time), for all other cases it only need to be
   * before the first possible boarding.
   */
  private OptimizedPathTail<T> createNewTransitLegTail(
          boolean firstRide,
          TransitPathLeg<T> originalLeg,
          int earliestDepartureTime,
          TripToTripTransfer<T> tx,
          OptimizedPathTail<T> tail
  ) {
    int txDepartureTime = arrivalTime(tx.from());
    int txArrivalTime = txDepartureTime + tx.transferDuration();

    TransitPathLeg<T> toTransitLeg = tail.getLeg().mutate()
            .boardStop(tx.to().stop(), tx.to().time())
            .build(costCalculator, slackProvider, false, txArrivalTime);

    PathLeg<T> newTail = createTransferLegIfExist(tx, txDepartureTime, txArrivalTime, toTransitLeg);

    var legBuilder = originalLeg.mutate();

    // The transfer may happen before the original board point. If so, the boarding must be changed
    // so the leg is valid (not traveling in reverse/back in time).
    if(tx.from().time() <= originalLeg.fromTime()) {
      legBuilder.boardStop(0, earliestDepartureTime);
    }

    legBuilder.newTail(tx.from().time(), newTail);

    // Using the earliest-departure-time as input here is in some cases wrong, but the leg
    // created here is temporary and will be mutated when connected with the leg in front of it.
    var fromTransitLeg = legBuilder.build(
            costCalculator, slackProvider, firstRide, earliestDepartureTime
    );

    return optimizedPathFactory.createPathLeg(
            fromTransitLeg,
            tx.guaranteedTransfer(),
            tail.getTransfersTo(),
            toTransitLeg
    );
  }

  private PathLeg<T> createTransferLegIfExist(
          TripToTripTransfer<T> tx,
          int departureTime,
          int arrivalTime,
          TransitPathLeg<T> nextLeg
  ) {
    if(tx.sameStop()) { return nextLeg; }

    //noinspection ConstantConditions
    return new TransferPathLeg<>(
            tx.from().stop(),
            departureTime,
            arrivalTime,
            tx.getPathTransfer(),
            nextLeg
    );
  }

  private List<List<TripToTripTransfer<T>>> sortTransfersOnArrivalTimeInDecOrder(
          List<List<TripToTripTransfer<T>>> transfers
  ) {
    return transfers.stream()
            .map(it ->
                    it.stream()
                            .sorted(Comparator.comparingInt(l -> -l.to().time()))
                            .collect(Collectors.toList())
            )
            .collect(Collectors.toList());
  }


  /** Trip alight time + alight slack */
  private int arrivalTime(TripStopTime<T> arrival) {
    return arrival.time() + slackProvider.alightSlack(arrival.trip().pattern());
  }

  private static <T> T last(List<T> list) { return list.get(list.size()-1);}
}
