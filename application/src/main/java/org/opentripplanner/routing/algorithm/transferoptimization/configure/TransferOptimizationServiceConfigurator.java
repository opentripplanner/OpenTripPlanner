package org.opentripplanner.routing.algorithm.transferoptimization.configure;

import java.util.List;
import java.util.function.IntFunction;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.api.request.via.RaptorViaLocation;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;
import org.opentripplanner.raptor.spi.RaptorDataProvider;
import org.opentripplanner.raptor.spi.RaptorStopNameResolver;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.routing.algorithm.transferoptimization.OptimizeTransferService;
import org.opentripplanner.routing.algorithm.transferoptimization.api.TransferOptimizationParameters;
import org.opentripplanner.routing.algorithm.transferoptimization.model.MinSafeTransferTimeCalculator;
import org.opentripplanner.routing.algorithm.transferoptimization.model.PathTailFilter;
import org.opentripplanner.routing.algorithm.transferoptimization.model.TransferWaitTimeCostCalculator;
import org.opentripplanner.routing.algorithm.transferoptimization.model.costfilter.MinCostPathTailFilterFactory;
import org.opentripplanner.routing.algorithm.transferoptimization.model.passthrough.PassThroughPathTailFilter;
import org.opentripplanner.routing.algorithm.transferoptimization.services.OptimizePathDomainService;
import org.opentripplanner.routing.algorithm.transferoptimization.services.TransferGenerator;
import org.opentripplanner.routing.algorithm.transferoptimization.services.TransferServiceAdaptor;
import org.opentripplanner.transfer.constrained.ConstrainedTransferService;
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * Responsible for assembly of the prioritized-transfer services.
 */
public class TransferOptimizationServiceConfigurator<T extends RaptorTripSchedule> {

  private final IntFunction<StopLocation> stopLookup;
  private final RaptorStopNameResolver stopNameResolver;
  private final ConstrainedTransferService transferService;
  private final RaptorDataProvider<T> raptorData;

  @Nullable
  private final int[] stopBoardAlightTransferCosts;

  private final TransferOptimizationParameters config;
  private final List<RaptorViaLocation> viaLocations;

  private TransferOptimizationServiceConfigurator(
    IntFunction<StopLocation> stopLookup,
    RaptorStopNameResolver stopNameResolver,
    ConstrainedTransferService transferService,
    RaptorDataProvider<T> raptorData,
    int[] stopBoardAlightTransferCosts,
    TransferOptimizationParameters config,
    List<RaptorViaLocation> viaLocations
  ) {
    this.stopLookup = stopLookup;
    this.stopNameResolver = stopNameResolver;
    this.transferService = transferService;
    this.raptorData = raptorData;
    this.stopBoardAlightTransferCosts = stopBoardAlightTransferCosts;
    this.config = config;
    this.viaLocations = viaLocations;
  }

  /**
   * Scope: Request
   */
  public static <T extends RaptorTripSchedule> OptimizeTransferService<T> createOptimizeTransferService(
    IntFunction<StopLocation> stopLookup,
    RaptorStopNameResolver stopNameResolver,
    ConstrainedTransferService transferService,
    RaptorDataProvider<T> raptorData,
    @Nullable int[] stopBoardAlightTransferCosts,
    TransferOptimizationParameters config,
    List<RaptorViaLocation> viaLocations
  ) {
    return new TransferOptimizationServiceConfigurator<T>(
      stopLookup,
      stopNameResolver,
      transferService,
      raptorData,
      stopBoardAlightTransferCosts,
      config,
      viaLocations
    ).createOptimizeTransferService();
  }

  private OptimizeTransferService<T> createOptimizeTransferService() {
    var pathTransferGenerator = createTransferGenerator(config.optimizeTransferPriority());

    if (config.optimizeTransferWaitTime()) {
      var transferWaitTimeCalculator = createTransferWaitTimeCalculator();

      var transfersPermutationService = createOptimizePathService(
        pathTransferGenerator,
        transferWaitTimeCalculator,
        raptorData.transitData().multiCriteriaCostCalculator()
      );

      return new OptimizeTransferService<>(
        transfersPermutationService,
        createMinSafeTxTimeService(),
        transferWaitTimeCalculator
      );
    } else {
      var transfersPermutationService = createOptimizePathService(
        pathTransferGenerator,
        null,
        raptorData.transitData().multiCriteriaCostCalculator()
      );
      return new OptimizeTransferService<>(transfersPermutationService);
    }
  }

  private OptimizePathDomainService<T> createOptimizePathService(
    TransferGenerator<T> transferGenerator,
    TransferWaitTimeCostCalculator transferWaitTimeCostCalculator,
    RaptorCostCalculator<T> costCalculator
  ) {
    return new OptimizePathDomainService<>(
      transferGenerator,
      costCalculator,
      raptorData.transitData().slackProvider(),
      transferWaitTimeCostCalculator,
      stopBoardAlightTransferCosts,
      config.extraStopBoardAlightCostsFactor(),
      createFilter(),
      stopNameResolver
    );
  }

  private MinSafeTransferTimeCalculator<T> createMinSafeTxTimeService() {
    return new MinSafeTransferTimeCalculator<>(raptorData.transitData().slackProvider());
  }

  private TransferGenerator<T> createTransferGenerator(boolean transferPriority) {
    var transferServiceAdaptor =
      transferService != null && transferPriority
        ? TransferServiceAdaptor.<T>create(stopLookup, transferService)
        : TransferServiceAdaptor.<T>noop();

    return new TransferGenerator<>(
      transferServiceAdaptor,
      raptorData.transitData().slackProvider(),
      raptorData.transferData()
    );
  }

  private TransferWaitTimeCostCalculator createTransferWaitTimeCalculator() {
    return new TransferWaitTimeCostCalculator(
      config.backTravelWaitTimeFactor(),
      config.minSafeWaitTimeFactor()
    );
  }

  private PathTailFilter<T> createFilter() {
    var filter = new MinCostPathTailFilterFactory<T>(
      config.optimizeTransferPriority(),
      config.optimizeTransferWaitTime()
    ).createFilter();

    if (!viaLocations.isEmpty()) {
      filter = new PassThroughPathTailFilter<>(filter, viaLocations);
    }
    return filter;
  }
}
