package org.opentripplanner.routing.algorithm.transferoptimization;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.routing.algorithm.raptor.path.PathDiff;
import org.opentripplanner.routing.algorithm.transferoptimization.api.OptimizedPath;
import org.opentripplanner.routing.algorithm.transferoptimization.model.MinSafeTransferTimeCalculator;
import org.opentripplanner.routing.algorithm.transferoptimization.model.TransferWaitTimeCostCalculator;
import org.opentripplanner.routing.algorithm.transferoptimization.services.OptimizePathDomainService;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class OptimizeTransferService<T extends RaptorTripSchedule> {

  private static final Logger LOG = LoggerFactory.getLogger(OptimizeTransferService.class);

  private final OptimizePathDomainService<T> optimizePathDomainService;
  private final MinSafeTransferTimeCalculator<T> minSafeTransferTimeCalculator;
  private final TransferWaitTimeCostCalculator transferWaitTimeCostCalculator;

  public OptimizeTransferService(
          OptimizePathDomainService<T> optimizePathDomainService,
          MinSafeTransferTimeCalculator<T> minSafeTransferTimeCalculator,
          TransferWaitTimeCostCalculator transferWaitTimeCostCalculator
  ) {
    this.optimizePathDomainService = optimizePathDomainService;
    this.minSafeTransferTimeCalculator = minSafeTransferTimeCalculator;
    this.transferWaitTimeCostCalculator = transferWaitTimeCostCalculator;
  }

  public OptimizeTransferService(OptimizePathDomainService<T> optimizePathDomainService) {
    this.optimizePathDomainService = optimizePathDomainService;
    this.minSafeTransferTimeCalculator = null;
    this.transferWaitTimeCostCalculator = null;
  }

  public List<Path<T>> optimize(Collection<Path<T>> paths) {
    setup(paths);

    long start = LOG.isDebugEnabled() ? System.currentTimeMillis() : 0;

    List<Path<T>> results = new ArrayList<>();

    for (Path<T> path : paths) {
      results.addAll(optimize(path));
    }

    if(LOG.isDebugEnabled()) {
      LOG.debug("Optimized transfers done in {} ms.", System.currentTimeMillis() - start);
      PathDiff.logDiff("RAPTOR", paths, "OPT", results, false, false, LOG::debug);
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
    // Skip transfer optimization if no transfers exist.
    if (path.numberOfTransfersExAccessEgress() == 0) {
      return List.of(new OptimizedPath<>(path));
    }
    return optimizePathDomainService.findBestTransitPath(path);
  }
}