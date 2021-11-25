package org.opentripplanner.routing.algorithm.transferoptimization.configure;

import java.util.function.IntFunction;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.transfer.TransferService;
import org.opentripplanner.routing.algorithm.transferoptimization.OptimizeTransferService;
import org.opentripplanner.routing.algorithm.transferoptimization.api.TransferOptimizationParameters;
import org.opentripplanner.routing.algorithm.transferoptimization.model.MinCostFilterChain;
import org.opentripplanner.routing.algorithm.transferoptimization.model.MinSafeTransferTimeCalculator;
import org.opentripplanner.routing.algorithm.transferoptimization.model.OptimizedPathTail;
import org.opentripplanner.routing.algorithm.transferoptimization.model.TransferWaitTimeCalculator;
import org.opentripplanner.routing.algorithm.transferoptimization.services.OptimizePathDomainService;
import org.opentripplanner.routing.algorithm.transferoptimization.services.TransferGenerator;
import org.opentripplanner.routing.algorithm.transferoptimization.services.TransferOptimizedFilterFactory;
import org.opentripplanner.routing.algorithm.transferoptimization.services.TransferServiceAdaptor;
import org.opentripplanner.transit.raptor.api.request.RaptorRequest;
import org.opentripplanner.transit.raptor.api.transit.CostCalculator;
import org.opentripplanner.transit.raptor.api.transit.RaptorStopNameResolver;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransitDataProvider;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

/**
 * Responsible for assembly of the prioritized-transfer services.
 */
public class TransferOptimizationServiceConfigurator<T extends RaptorTripSchedule> {
  private final IntFunction<Stop> stopLookup;
  private final RaptorStopNameResolver stopNameResolver;
  private final TransferService transferService;
  private final RaptorTransitDataProvider<T> transitDataProvider;
  private final RaptorRequest<T> raptorRequest;
  private final TransferOptimizationParameters config;


  public TransferOptimizationServiceConfigurator(
      IntFunction<Stop> stopLookup,
      RaptorStopNameResolver stopNameResolver,
      TransferService transferService,
      RaptorTransitDataProvider<T> transitDataProvider,
      RaptorRequest<T> raptorRequest,
      TransferOptimizationParameters config
  ) {
    this.stopLookup = stopLookup;
    this.stopNameResolver = stopNameResolver;
    this.transferService = transferService;
    this.transitDataProvider = transitDataProvider;
    this.raptorRequest = raptorRequest;
    this.config = config;
  }

  /**
   * Scope: Request
   */
  public static <T extends RaptorTripSchedule> OptimizeTransferService<T> createOptimizeTransferService(
      IntFunction<Stop> stopLookup,
      RaptorStopNameResolver stopNameResolver,
      TransferService transferService,
      RaptorTransitDataProvider<T> transitDataProvider,
      RaptorRequest<T> raptorRequest,
      TransferOptimizationParameters config
  ) {
    return new TransferOptimizationServiceConfigurator<T>(
        stopLookup,
        stopNameResolver,
        transferService,
        transitDataProvider,
        raptorRequest,
        config
    ).createOptimizeTransferService();
  }

  private OptimizeTransferService<T> createOptimizeTransferService() {
    var pathTransferGenerator = createTransferGenerator(config.optimizeTransferPriority());
    var filter = createTransferOptimizedFilter(
            config.optimizeTransferPriority(), config.optimizeTransferWaitTime()
    );

    if(config.optimizeTransferWaitTime()) {
      var transferWaitTimeCalculator = createTransferWaitTimeCalculator();

      var transfersPermutationService = createOptimizePathService(
              pathTransferGenerator,
              filter,
              transferWaitTimeCalculator,
              transitDataProvider.multiCriteriaCostCalculator()
      );

      return new OptimizeTransferService<>(
              transfersPermutationService,
              createMinSafeTxTimeService(),
              transferWaitTimeCalculator
      );
    }
    else {
      var transfersPermutationService = createOptimizePathService(
              pathTransferGenerator,
              filter,
              null,
              transitDataProvider.multiCriteriaCostCalculator()
      );
      return new OptimizeTransferService<>(transfersPermutationService);
    }
  }

  private OptimizePathDomainService<T> createOptimizePathService(
          TransferGenerator<T> transferGenerator,
          MinCostFilterChain<OptimizedPathTail<T>> transferPointFilter,
          TransferWaitTimeCalculator transferWaitTimeCalculator,
          CostCalculator costCalculator
  ) {
    return new OptimizePathDomainService<>(
            transferGenerator,
            costCalculator,
            raptorRequest.slackProvider(),
            transferWaitTimeCalculator,
            transferPointFilter,
            stopNameResolver
    );
  }

  private MinSafeTransferTimeCalculator<T> createMinSafeTxTimeService() {
    return new MinSafeTransferTimeCalculator<>(raptorRequest.slackProvider());
  }

  private TransferGenerator<T> createTransferGenerator(boolean transferPriority) {
    var transferServiceAdaptor = (transferService != null && transferPriority)
            ? TransferServiceAdaptor.<T>create(stopLookup, transferService)
            : TransferServiceAdaptor.<T>noop();

    return new TransferGenerator<>(
        transferServiceAdaptor,
        raptorRequest.slackProvider(),
        transitDataProvider
    );
  }

  private TransferWaitTimeCalculator createTransferWaitTimeCalculator() {
    return new TransferWaitTimeCalculator(
            config.backTravelWaitTimeFactor(),
            config.minSafeWaitTimeFactor()
    );
  }

  private MinCostFilterChain<OptimizedPathTail<T>> createTransferOptimizedFilter(
          boolean transferPriority, boolean optimizeWaitTime
  ) {
    return TransferOptimizedFilterFactory.filter(transferPriority, optimizeWaitTime);
  }
}
