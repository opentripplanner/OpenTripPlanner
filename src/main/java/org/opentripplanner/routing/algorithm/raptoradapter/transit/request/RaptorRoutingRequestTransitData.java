package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import java.time.ZonedDateTime;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opentripplanner.framework.time.ServiceDateUtils;
import org.opentripplanner.model.transfer.TransferService;
import org.opentripplanner.raptor.spi.CostCalculator;
import org.opentripplanner.raptor.spi.IntIterator;
import org.opentripplanner.raptor.spi.RaptorConstrainedTransfer;
import org.opentripplanner.raptor.spi.RaptorConstrainedTripScheduleBoardingSearch;
import org.opentripplanner.raptor.spi.RaptorPathConstrainedTransferSearch;
import org.opentripplanner.raptor.spi.RaptorRoute;
import org.opentripplanner.raptor.spi.RaptorStopNameResolver;
import org.opentripplanner.raptor.spi.RaptorTransfer;
import org.opentripplanner.raptor.spi.RaptorTransitDataProvider;
import org.opentripplanner.raptor.util.BitSetIterator;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RaptorTransferIndex;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.constrainedtransfer.ConstrainedBoardingSearch;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.constrainedtransfer.TransferForPatternByStopPos;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.CostCalculatorFactory;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.GeneralizedCostParametersMapper;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.transit.model.network.RoutingTripPattern;
import org.opentripplanner.util.OTPFeature;

/**
 * This is the data provider for the Range Raptor search engine. It uses data from the TransitLayer,
 * but filters it by dates and modes per request. Transfers durations are pre-calculated per request
 * based on walk speed.
 */
public class RaptorRoutingRequestTransitData implements RaptorTransitDataProvider<TripSchedule> {

  private final TransitLayer transitLayer;

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

  private final List<TransferForPatternByStopPos> forwardConstrainedTransfers;

  private final List<TransferForPatternByStopPos> reverseConstrainedTransfers;

  private final ZonedDateTime transitSearchTimeZero;

  private final CostCalculator<TripSchedule> generalizedCostCalculator;

  private final int validTransitDataStartTime;

  private final int validTransitDataEndTime;

  public RaptorRoutingRequestTransitData(
    TransitLayer transitLayer,
    ZonedDateTime transitSearchTimeZero,
    int additionalPastSearchDays,
    int additionalFutureSearchDays,
    TransitDataProviderFilter filter,
    RouteRequest request
  ) {
    this.transferService = transitLayer.getTransferService();
    this.transitLayer = transitLayer;
    this.transitSearchTimeZero = transitSearchTimeZero;

    // Delegate to the creator to construct the needed data structures. The code is messy so
    // it is nice to NOT have it in the class. It isolate this code to only be available at
    // the time of construction
    var transitDataCreator = new RaptorRoutingRequestTransitDataCreator(
      transitLayer,
      transitSearchTimeZero
    );
    List<TripPatternForDates> tripPatterns = transitDataCreator.createTripPatterns(
      additionalPastSearchDays,
      additionalFutureSearchDays,
      filter
    );
    this.patternIndex = transitDataCreator.createPatternIndex(tripPatterns);
    this.activeTripPatternsPerStop = transitDataCreator.createTripPatternsPerStop(tripPatterns);
    this.transferIndex = transitLayer.getRaptorTransfersForRequest(request);

    this.forwardConstrainedTransfers = transitLayer.getForwardConstrainedTransfers();
    this.reverseConstrainedTransfers = transitLayer.getReverseConstrainedTransfers();

    var mcCostParams = GeneralizedCostParametersMapper.map(request, patternIndex);

    this.generalizedCostCalculator =
      CostCalculatorFactory.createCostCalculator(
        mcCostParams,
        transitLayer.getStopBoardAlightCosts()
      );

    this.validTransitDataStartTime =
      ServiceDateUtils.secondsSinceStartOfTime(
        this.transitSearchTimeZero,
        this.transitSearchTimeZero.minusDays(additionalPastSearchDays).toInstant()
      );
    // The +1 is due to the validity being to the end of the day
    this.validTransitDataEndTime =
      ServiceDateUtils.secondsSinceStartOfTime(
        this.transitSearchTimeZero,
        this.transitSearchTimeZero.plusDays(additionalFutureSearchDays + 1).toInstant()
      );
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
    return transitLayer.getStopCount();
  }

  @Override
  public CostCalculator<TripSchedule> multiCriteriaCostCalculator() {
    return generalizedCostCalculator;
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
          transitLayer.getStopByIndex(fromTrip.pattern().stopIndex(fromStopPosition)),
          toTrip.getOriginalTripTimes().getTrip(),
          toStopPosition,
          transitLayer.getStopByIndex(toTrip.pattern().stopIndex(toStopPosition))
        );
      }
    };
  }

  @Nonnull
  @Override
  public RaptorStopNameResolver stopNameResolver() {
    return (int stopIndex) -> {
      var s = transitLayer.getStopByIndex(stopIndex);
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
  public RaptorConstrainedTripScheduleBoardingSearch<TripSchedule> transferConstraintsForwardSearch(
    int routeIndex
  ) {
    TransferForPatternByStopPos transfers = forwardConstrainedTransfers.get(routeIndex);
    if (transfers == null) {
      return ConstrainedBoardingSearch.NOOP_SEARCH;
    }
    return new ConstrainedBoardingSearch(true, transfers);
  }

  @Override
  public RaptorConstrainedTripScheduleBoardingSearch<TripSchedule> transferConstraintsReverseSearch(
    int routeIndex
  ) {
    TransferForPatternByStopPos transfers = reverseConstrainedTransfers.get(routeIndex);
    if (transfers == null) {
      return ConstrainedBoardingSearch.NOOP_SEARCH;
    }
    return new ConstrainedBoardingSearch(false, transfers);
  }
}
