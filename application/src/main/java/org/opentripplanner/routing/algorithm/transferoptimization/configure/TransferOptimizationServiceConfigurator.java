package org.opentripplanner.routing.algorithm.transferoptimization.configure;

import java.util.List;
import java.util.function.IntFunction;
import javax.annotation.Nullable;
import org.opentripplanner.model.transfer.TransferService;
import org.opentripplanner.raptor.api.model.RaptorStopNameResolver;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorViaLocation;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;
import org.opentripplanner.raptor.spi.RaptorTransitDataProvider;
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
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * Responsible for assembly of the prioritized-transfer services.
 */
public class TransferOptimizationServiceConfigurator<T extends RaptorTripSchedule> {

  private final IntFunction<StopLocation> stopLookup;
  private final RaptorStopNameResolver stopNameResolver;
  private final TransferService transferService;
  private final RaptorTransitDataProvider<T> transitDataProvider;

  @Nullable
  private final int[] stopBoardAlightTransferCosts;

  private final TransferOptimizationParameters config;
  private final List<RaptorViaLocation> viaLocations;

  private TransferOptimizationServiceConfigurator(
    IntFunction<StopLocation> stopLookup,
    RaptorStopNameResolver stopNameResolver,
    TransferService transferService,
    RaptorTransitDataProvider<T> transitDataProvider,
    int[] stopBoardAlightTransferCosts,
    TransferOptimizationParameters config,
    List<RaptorViaLocation> viaLocations
  ) {
    this.stopLookup = stopLookup;
    this.stopNameResolver = stopNameResolver;
    this.transferService = transferService;
    this.transitDataProvider = transitDataProvider;
    this.stopBoardAlightTransferCosts = stopBoardAlightTransferCosts;
    this.config = config;
    this.viaLocations = viaLocations;
  }

  /**
   * Scope: Request
   */
  public static <
    T extends RaptorTripSchedule
  > OptimizeTransferService<T> createOptimizeTransferService(
    IntFunction<StopLocation> stopLookup,
    RaptorStopNameResolver stopNameResolver,
    TransferService transferService,
    RaptorTransitDataProvider<T> transitDataProvider,
    @Nullable int[] stopBoardAlightTransferCosts,
    TransferOptimizationParameters config,
    List<RaptorViaLocation> viaLocations
  ) {
    return new TransferOptimizationServiceConfigurator<T>(
      stopLookup,
      stopNameResolver,
      transferService,
      transitDataProvider,
      stopBoardAlightTransferCosts,
      config,
      viaLocations
    )
      .createOptimizeTransferService();
  }

  private OptimizeTransferService<T> createOptimizeTransferService() {
    var pathTransferGenerator = createTransferGenerator(config.optimizeTransferPriority());

    if (config.optimizeTransferWaitTime()) {
      var transferWaitTimeCalculator = createTransferWaitTimeCalculator();

      var transfersPermutationService = createOptimizePathService(
        pathTransferGenerator,
        transferWaitTimeCalculator,
        transitDataProvider.multiCriteriaCostCalculator()
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
        transitDataProvider.multiCriteriaCostCalculator()
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
      transitDataProvider.slackProvider(),
      transferWaitTimeCostCalculator,
      stopBoardAlightTransferCosts,
      config.extraStopBoardAlightCostsFactor(),
      createFilter(),
      stopNameResolver
    );
  }

  private MinSafeTransferTimeCalculator<T> createMinSafeTxTimeService() {
    return new MinSafeTransferTimeCalculator<>(transitDataProvider.slackProvider());
  }

  private TransferGenerator<T> createTransferGenerator(boolean transferPriority) {
    var transferServiceAdaptor = (transferService != null && transferPriority)
      ? TransferServiceAdaptor.<T>create(stopLookup, transferService)
      : TransferServiceAdaptor.<T>noop();

    return new TransferGenerator<>(transferServiceAdaptor, transitDataProvider);
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
    )
      .createFilter();

    if (!viaLocations.isEmpty()) {
      filter = new PassThroughPathTailFilter<>(filter, viaLocations);
    }
    return filter;
  }
}
