package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import java.time.ZonedDateTime;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.model.transfer.TransferService;
import org.opentripplanner.raptor.api.model.RaptorConstrainedTransfer;
import org.opentripplanner.raptor.api.model.RaptorStopNameResolver;
import org.opentripplanner.raptor.api.model.RaptorTransfer;
import org.opentripplanner.raptor.spi.IntIterator;
import org.opentripplanner.raptor.spi.RaptorConstrainedBoardingSearch;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;
import org.opentripplanner.raptor.spi.RaptorPathConstrainedTransferSearch;
import org.opentripplanner.raptor.spi.RaptorRoute;
import org.opentripplanner.raptor.spi.RaptorSlackProvider;
import org.opentripplanner.raptor.spi.RaptorTransitDataProvider;
import org.opentripplanner.raptor.util.BitSetIterator;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RaptorTransferIndex;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RaptorTransitData;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.SlackProvider;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.constrainedtransfer.ConstrainedBoardingSearch;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.constrainedtransfer.ConstrainedTransfersForPatterns;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.CostCalculatorFactory;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.GeneralizedCostParametersMapper;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.transit.model.network.RoutingTripPattern;
import org.opentripplanner.transit.model.network.grouppriority.TransitGroupPriorityService;
import org.opentripplanner.utils.time.ServiceDateUtils;

/**
 * This is the data provider for the Range Raptor search engine. It uses data from the RaptorTransitData,
 * but filters it by dates and modes per request. Transfer durations are pre-calculated per request
 * based on walk speed.
 */
public class RaptorRoutingRequestTransitData implements RaptorTransitDataProvider<TripSchedule> {

  private final RaptorTransitData raptorTransitData;

  private final TransferService transferService;

  /**
   * Active route indices by stop index
   */
  private final List<int[]> activeTripPatternsPerStop;

  /**
   * Trip patterns by route index
   */
  private final List<TripPatternForDates> patternIndex;

  /**
   * Transfers by stop index
   */
  private final RaptorTransferIndex transferIndex;

  private final ConstrainedTransfersForPatterns constrainedTransfers;

  private final ZonedDateTime transitSearchTimeZero;

  private final RaptorCostCalculator<TripSchedule> generalizedCostCalculator;

  private final RaptorSlackProvider slackProvider;

  private final int validTransitDataStartTime;

  private final int validTransitDataEndTime;

  public RaptorRoutingRequestTransitData(
    RaptorTransitData raptorTransitData,
    TransitGroupPriorityService transitGroupPriorityService,
    ZonedDateTime transitSearchTimeZero,
    int additionalPastSearchDays,
    int additionalFutureSearchDays,
    TransitDataProviderFilter filter,
    RouteRequest request
  ) {
    this.transferService = raptorTransitData.getTransferService();
    this.raptorTransitData = raptorTransitData;
    this.transitSearchTimeZero = transitSearchTimeZero;

    // Delegate to the creator to construct the needed data structures. The code is messy so
    // it is nice to NOT have it in the class. It isolates this code to only be available at
    // the time of construction
    var transitDataCreator = new RaptorRoutingRequestTransitDataCreator(
      raptorTransitData,
      transitSearchTimeZero
    );
    List<TripPatternForDates> tripPatterns = transitDataCreator.createTripPatterns(
      additionalPastSearchDays,
      additionalFutureSearchDays,
      filter,
      transitGroupPriorityService
    );
    this.patternIndex = transitDataCreator.createPatternIndex(tripPatterns);
    this.activeTripPatternsPerStop = transitDataCreator.createTripPatternsPerStop(tripPatterns);
    this.transferIndex = raptorTransitData.getRaptorTransfersForRequest(request);
    this.constrainedTransfers = raptorTransitData.getConstrainedTransfers();

    var mcCostParams = GeneralizedCostParametersMapper.map(request, patternIndex);

    this.generalizedCostCalculator = CostCalculatorFactory.createCostCalculator(
      mcCostParams,
      raptorTransitData.getStopBoardAlightTransferCosts()
    );

    this.slackProvider = new SlackProvider(
      (int) request.preferences().transfer().slack().toSeconds(),
      request.preferences().transit().boardSlack(),
      request.preferences().transit().alightSlack()
    );

    this.validTransitDataStartTime = ServiceDateUtils.secondsSinceStartOfTime(
      this.transitSearchTimeZero,
      this.transitSearchTimeZero.minusDays(additionalPastSearchDays).toInstant()
    );
    // The +1 is due to the validity being to the end of the day
    this.validTransitDataEndTime = ServiceDateUtils.secondsSinceStartOfTime(
      this.transitSearchTimeZero,
      this.transitSearchTimeZero.plusDays(additionalFutureSearchDays + 1).toInstant()
    );
  }

  public RaptorRoutingRequestTransitData(
    RaptorRoutingRequestTransitData original,
    RaptorCostCalculator<TripSchedule> newCostCalculator
  ) {
    this.raptorTransitData = original.raptorTransitData;
    this.transitSearchTimeZero = original.transitSearchTimeZero;
    this.activeTripPatternsPerStop = original.activeTripPatternsPerStop;
    this.patternIndex = original.patternIndex;
    this.transferIndex = original.transferIndex;
    this.transferService = original.transferService;
    this.constrainedTransfers = original.constrainedTransfers;
    this.validTransitDataStartTime = original.validTransitDataStartTime;
    this.validTransitDataEndTime = original.validTransitDataEndTime;
    this.generalizedCostCalculator = newCostCalculator;
    this.slackProvider = original.slackProvider();
  }

  @Override
  public Iterator<RaptorTransfer> getTransfersFromStop(int stopIndex) {
    return transferIndex.getForwardTransfers(stopIndex).iterator();
  }

  @Override
  public Iterator<? extends RaptorTransfer> getTransfersToStop(int stopIndex) {
    return transferIndex.getReversedTransfers(stopIndex).iterator();
  }

  @Override
  public IntIterator routeIndexIterator(IntIterator stops) {
    BitSet activeTripPatternsForGivenStops = new BitSet(RoutingTripPattern.indexCounter());

    while (stops.hasNext()) {
      int[] patterns = activeTripPatternsPerStop.get(stops.next());
      for (int i : patterns) {
        activeTripPatternsForGivenStops.set(i);
      }
    }

    return new BitSetIterator(activeTripPatternsForGivenStops);
  }

  @Override
  public RaptorRoute<TripSchedule> getRouteForIndex(int routeIndex) {
    return patternIndex.get(routeIndex);
  }

  @Override
  public int numberOfStops() {
    return raptorTransitData.getStopCount();
  }

  @Override
  public RaptorCostCalculator<TripSchedule> multiCriteriaCostCalculator() {
    return generalizedCostCalculator;
  }

  @Override
  public RaptorSlackProvider slackProvider() {
    return slackProvider;
  }

  @Override
  public RaptorPathConstrainedTransferSearch<TripSchedule> transferConstraintsSearch() {
    if (OTPFeature.TransferConstraints.isOff() || transferService == null) {
      return null;
    }

    return new RaptorPathConstrainedTransferSearch<>() {
      @Nullable
      @Override
      public RaptorConstrainedTransfer findConstrainedTransfer(
        TripSchedule fromTrip,
        int fromStopPosition,
        TripSchedule toTrip,
        int toStopPosition
      ) {
        return transferService.findTransfer(
          fromTrip.getOriginalTripTimes().getTrip(),
          fromStopPosition,
          raptorTransitData.getStopByIndex(fromTrip.pattern().stopIndex(fromStopPosition)),
          toTrip.getOriginalTripTimes().getTrip(),
          toStopPosition,
          raptorTransitData.getStopByIndex(toTrip.pattern().stopIndex(toStopPosition))
        );
      }
    };
  }

  @Override
  public RaptorStopNameResolver stopNameResolver() {
    return (int stopIndex) -> {
      var s = raptorTransitData.getStopByIndex(stopIndex);
      return s == null ? "null" : s.getName() + "(" + stopIndex + ")";
    };
  }

  @Override
  public int getValidTransitDataStartTime() {
    return validTransitDataStartTime;
  }

  @Override
  public int getValidTransitDataEndTime() {
    return validTransitDataEndTime;
  }

  @Override
  public RaptorConstrainedBoardingSearch<TripSchedule> transferConstraintsForwardSearch(
    int routeIndex
  ) {
    var fromStopTransfers = constrainedTransfers.fromStop(routeIndex);
    var toStopTransfers = constrainedTransfers.toStop(routeIndex);

    if (fromStopTransfers == null && toStopTransfers == null) {
      return ConstrainedBoardingSearch.NOOP_SEARCH;
    }
    return new ConstrainedBoardingSearch(true, fromStopTransfers, toStopTransfers);
  }

  @Override
  public RaptorConstrainedBoardingSearch<TripSchedule> transferConstraintsReverseSearch(
    int routeIndex
  ) {
    var fromStopTransfers = constrainedTransfers.toStop(routeIndex);
    var toStopTransfers = constrainedTransfers.fromStop(routeIndex);

    if (fromStopTransfers == null && toStopTransfers == null) {
      return ConstrainedBoardingSearch.NOOP_SEARCH;
    }
    return new ConstrainedBoardingSearch(false, toStopTransfers, fromStopTransfers);
  }
}
