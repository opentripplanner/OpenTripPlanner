package org.opentripplanner.routing.algorithm.transferoptimization.services;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;
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

public class TransfersPermutationService<T extends RaptorTripSchedule> {
  /**
   * This limit is used to exit an infinite recursion (programming error).
   * For a large transit network the maximum possible number of rounds is
   * between 10 and 20. For all of Norway 14 round is the current max.
   */
  public static final int MAX_ROUNDS_LIMIT = 100;

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

  /**
   */
  public List<OptimizedPath<T>> findAllTransitPathPermutations(Path<T> path) {
    TransitPathLeg<T> leg0 = path.accessLeg().nextTransitLeg();

    // Path has no transit legs(possible with flex access) or
    // the path have no transfers, then use the path found.
    if(leg0 == null || leg0.nextTransitLeg() == null) {
      return List.of(new OptimizedPath<T>(path, path));
    }

    List<TransitPathLeg<T>> paths = findTransitPathsNextRound(
        0, path.accessLeg().toTime(), fromStopTime(leg0), leg0
    );

    return paths.stream()
        .map(l -> newPath(path, l))
        .map(p -> new OptimizedPath<>(path, p))
        .collect(Collectors.toList());
  }

  private List<TransitPathLeg<T>> findTransitPathsNextRound(
      final int round, final int arrivalTime, final StopTime from, final TransitPathLeg<T> tail
  ) {
    return withNewRound(round, nextRound -> findTransitPaths(nextRound, arrivalTime, from, tail));
  }

  private List<TransitPathLeg<T>> findTransitPaths(
      final int round, final int arrivalTime, final StopTime from, final TransitPathLeg<T> leg
  ) {
      TransitPathLeg<T> nxtLeg = leg.nextTransitLeg();

      if(nxtLeg == null) {
        return List.of(
            leg.mutate()
                .boardStop(from.stop(), from.time())
                .build(costCalculator, slackProvider, round < 2, arrivalTime)
        );
      }

      StopTime fromDeparture = StopTime.stopTime(leg.fromStop(), leg.fromTime());
      List<TripToTripTransfer<T>> transfers = t2tService.findTransfers(
          leg.trip(), fromDeparture, nxtLeg.trip()
      );

      if(transfers.isEmpty()) { return List.of(); }

      List<TransitPathLeg<T>> result = new ArrayList<>();

      for (TripToTripTransfer<T> tx : transfers) {
        List<PathLeg<T>> paths = createTransfers(round, tx, nxtLeg);

        for (PathLeg<T> next : paths) {
          result.add(
              leg.mutate()
                  .boardStop(from.stop(), from.time())
                  .newTail(tx.from().time(), next)
                  .build(costCalculator, slackProvider, round < 2, arrivalTime)
          );
        }
      }
      return result;
  }

  private List<PathLeg<T>> createTransfers(int round, TripToTripTransfer<T> tx, TransitPathLeg<T> nextLeg) {
    int departureTime = arrivalTime(tx.from());
    int arrivalTime = departureTime + tx.transferDuration();
    List<TransitPathLeg<T>> paths = findTransitPathsNextRound(round, arrivalTime, tx.to(), nextLeg);

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

  /**
   * Make sure round counter is incremented before body and decrements after, also
   * check for max depth and update cost calculator.
   */
  private <S>  S withNewRound(int round, IntFunction<S> body) {
    if(round == MAX_ROUNDS_LIMIT) {
      throw new IllegalStateException("The current path have more than " + MAX_ROUNDS_LIMIT + " legs.");
    }
    ++round;
    S result = body.apply(round);
    --round;
    return result;
  }
}
