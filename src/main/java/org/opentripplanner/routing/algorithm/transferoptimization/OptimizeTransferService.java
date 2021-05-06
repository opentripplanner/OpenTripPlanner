package org.opentripplanner.routing.algorithm.transferoptimization;

import static org.opentripplanner.routing.algorithm.transferoptimization.services.TransferDiffDebug.debugDiffAfterPriorityFilter;
import static org.opentripplanner.routing.algorithm.transferoptimization.services.TransferDiffDebug.debugDiffAfterWaitTimeFilter;
import static org.opentripplanner.routing.algorithm.transferoptimization.services.TransferDiffDebug.debugDiffOriginalVsPermutations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.opentripplanner.routing.algorithm.transferoptimization.api.OptimizedPath;
import org.opentripplanner.routing.algorithm.transferoptimization.model.FilterPathsUtil;
import org.opentripplanner.routing.algorithm.transferoptimization.services.MinSafeTransferTimeCalculator;
import org.opentripplanner.routing.algorithm.transferoptimization.services.OptimizeTransferCostCalculator;
import org.opentripplanner.routing.algorithm.transferoptimization.services.PriorityBasedTransfersCostCalculator;
import org.opentripplanner.routing.algorithm.transferoptimization.services.TransfersPermutationService;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.path.TransitPathLeg;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptimizeTransferService<T extends RaptorTripSchedule> {

  private static final Logger LOG = LoggerFactory.getLogger(OptimizeTransferService.class);

  private final TransfersPermutationService<T> transfersPermutationService;
  private final PriorityBasedTransfersCostCalculator<T> priorityBasedTransfersCostCalculator;
  private final MinSafeTransferTimeCalculator<T> minSafeTransferTimeCalculator;
  private final OptimizeTransferCostCalculator transferWaitTimeCostCalculator;

  public OptimizeTransferService(
      TransfersPermutationService<T> transfersPermutationService,
      PriorityBasedTransfersCostCalculator<T> priorityBasedTransfersCostCalculator,
      MinSafeTransferTimeCalculator<T> minSafeTransferTimeCalculator,
      OptimizeTransferCostCalculator optimizeTransferCostCalculator
  ) {
    this.transfersPermutationService = transfersPermutationService;
    this.priorityBasedTransfersCostCalculator = priorityBasedTransfersCostCalculator;
    this.minSafeTransferTimeCalculator = minSafeTransferTimeCalculator;
    this.transferWaitTimeCostCalculator = optimizeTransferCostCalculator;
  }

  public OptimizeTransferService(
      TransfersPermutationService<T> transfersPermutationService,
      PriorityBasedTransfersCostCalculator<T> priorityBasedTransfersCostCalculator
  ) {
    this.transfersPermutationService = transfersPermutationService;
    this.priorityBasedTransfersCostCalculator = priorityBasedTransfersCostCalculator;
    this.minSafeTransferTimeCalculator = null;
    this.transferWaitTimeCostCalculator = null;
  }

  public List<Path<T>> optimize(Collection<Path<T>> paths) {
    setup(paths);

    List<Path<T>> results = new ArrayList<>();

    for (Path<T> path : paths) {
      results.addAll(optimize(path));
    }
    return results;
  }

  /**
   * Initiate calculation.
   */
  @SuppressWarnings("ConstantConditions")
  private void setup(Collection<Path<T>> paths) {
    if (transferWaitTimeCostCalculator != null) {
      transferWaitTimeCostCalculator.setMinSafeTransferTime(
              minSafeTransferTimeCalculator.minSafeTransferTime(paths)
      );
    }
  }

  /**
   * Optimize a single transfer, finding all possible permutations of transfers for the path and
   * filtering the list down one path, or a few equally good paths.
   */
  private Collection<OptimizedPath<T>> optimize(Path<T> path) {
    TransitPathLeg<T> leg0 = path.accessLeg().nextTransitLeg();

    // Path has no transit legs(possible with flex access) or
    // the path have no transfers, then use the path found.
    if (leg0 == null || leg0.nextTransitLeg() == null) {
      return List.of(new OptimizedPath<>(path, path));
    }
    LOG.debug("Optimize path: {}", path);

    var transfers = decorateWithSpecializedTransfers(
            generateAllPossibleTransfers(path)
    );

    if(notMoreThanOneOptionAvailable(transfers)) { return transfers; }

    transfers = optimizedTransfersOnPriority(transfers);

    if (notMoreThanOneOptionAvailable(transfers)) { return transfers; }

    return optimizeTransferOnGenerelazedCostAndWaitTime(transfers);
  }

  private List<OptimizedPath<T>> generateAllPossibleTransfers(Path<T> path) {
    var allPossibleTransfers = transfersPermutationService.findAllTransitPathPermutations(path);
    debugDiffOriginalVsPermutations(path, allPossibleTransfers);
    return allPossibleTransfers;
  }

  private List<OptimizedPath<T>> optimizedTransfersOnPriority(List<OptimizedPath<T>> list) {
    var result = FilterPathsUtil.filter(list, OptimizedPath::transferPriorityCost);
    debugDiffAfterPriorityFilter(list, result);
    return result;
  }

  private List<OptimizedPath<T>> decorateWithSpecializedTransfers(List<OptimizedPath<T>> list) {
    return list.stream()
            .map(priorityBasedTransfersCostCalculator::decorateWithTransfers)
            .collect(Collectors.toList());
  }

  private List<OptimizedPath<T>> optimizeTransferOnGenerelazedCostAndWaitTime(List<OptimizedPath<T>> list) {
    List<OptimizedPath<T>> result;

    // Fallback to the generalized-cost if not adjust wait-time cost calculator is set
    if(transferWaitTimeCostCalculator == null) {
      result = FilterPathsUtil.filter(list, Path::generalizedCost);
    }
    else {
      result = list.stream()
              .map(p -> p.withWaitTimeCost(transferWaitTimeCostCalculator.cost(p)))
              .collect(Collectors.toList());
      result = FilterPathsUtil.filter(result, OptimizedPath::getWaitTimeOptimizedCost);
    }

    debugDiffAfterWaitTimeFilter(list, result);

    return result;
  }

  private boolean notMoreThanOneOptionAvailable(List<OptimizedPath<T>> paths) {
    return paths.size() <= 1;
  }
}
