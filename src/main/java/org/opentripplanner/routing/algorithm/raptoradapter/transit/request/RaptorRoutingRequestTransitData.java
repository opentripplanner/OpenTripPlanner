package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import java.time.ZonedDateTime;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.model.transfer.TransferService;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RaptorTransferIndex;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.DefaultCostCalculator;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.DateMapper;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.McCostParamsMapper;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.api.request.RoutingRequest.AccessibilityMode;
import org.opentripplanner.transit.raptor.api.transit.CostCalculator;
import org.opentripplanner.transit.raptor.api.transit.IntIterator;
import org.opentripplanner.transit.raptor.api.transit.RaptorConstrainedTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorPathConstrainedTransferSearch;
import org.opentripplanner.transit.raptor.api.transit.RaptorRoute;
import org.opentripplanner.transit.raptor.api.transit.RaptorStopNameResolver;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransitDataProvider;
import org.opentripplanner.transit.raptor.util.BitSetIterator;
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
  private final RaptorTransferIndex transfers;

  private final ZonedDateTime transitSearchTimeZero;

  private final CostCalculator generalizedCostCalculator;

  private final int validTransitDataStartTime;

  private final int validTransitDataEndTime;

  private final AccessibilityMode accessibilityMode;

  public RaptorRoutingRequestTransitData(
          TransferService transferService,
          TransitLayer transitLayer,
          ZonedDateTime transitSearchTimeZero,
          int additionalPastSearchDays,
          int additionalFutureSearchDays,
          TransitDataProviderFilter filter,
          RoutingContext routingContext
  ) {

    this.transferService = transferService;
    this.transitLayer = transitLayer;
    this.transitSearchTimeZero = transitSearchTimeZero;
    this.accessibilityMode = routingContext.opt.accessibilityMode;

    // Delegate to the creator to construct the needed data structures. The code is messy so
    // it is nice to NOT have it in the class. It isolate this code to only be available at
    // the time of construction
    var transitDataCreator = new RaptorRoutingRequestTransitDataCreator(
            transitLayer, transitSearchTimeZero
    );
    this.patternIndex = transitDataCreator.createTripPatterns(
        additionalPastSearchDays,
        additionalFutureSearchDays,
        filter
    );
    this.activeTripPatternsPerStop = transitDataCreator.createTripPatternsPerStop(patternIndex);
    this.transfers = transitLayer.getRaptorTransfersForRequest(routingContext);
    this.generalizedCostCalculator = new DefaultCostCalculator(
            McCostParamsMapper.map(routingContext.opt),
            transitLayer.getStopIndex().stopBoardAlightCosts
    );
    this.validTransitDataStartTime = DateMapper.secondsSinceStartOfTime(
            this.transitSearchTimeZero,
        this.transitSearchTimeZero.minusDays(additionalPastSearchDays).toInstant()
    );
    // The +1 is due to the validity being to the end of the day
    this.validTransitDataEndTime = DateMapper.secondsSinceStartOfTime(
            this.transitSearchTimeZero,
        this.transitSearchTimeZero.plusDays(additionalFutureSearchDays + 1).toInstant()
    );
  }

  @Override
  public Iterator<RaptorTransfer> getTransfersFromStop(int stopIndex) {
    return transfers.getForwardTransfers().get(stopIndex).iterator();
  }

  @Override
  public Iterator<? extends RaptorTransfer> getTransfersToStop(int stopIndex) {
    return transfers.getReversedTransfers().get(stopIndex).iterator();
  }

  @Override
  public IntIterator routeIndexIterator(IntIterator stops) {
    BitSet activeTripPatternsForGivenStops = new BitSet(patternIndex.size());

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
  public CostCalculator multiCriteriaCostCalculator() {
    return generalizedCostCalculator;
  }

  @Override
  public RaptorPathConstrainedTransferSearch<TripSchedule> transferConstraintsSearch() {
    if(OTPFeature.TransferConstraints.isOff() || transferService == null) { return null; }

    return new RaptorPathConstrainedTransferSearch<>() {
      @Nullable @Override
      public RaptorConstrainedTransfer findConstrainedTransfer(
              TripSchedule fromTrip, int fromStopPosition, TripSchedule toTrip, int toStopPosition
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

  @Override
  public RaptorStopNameResolver stopNameResolver() {
    return (int stopIndex) -> {
      var s = transitLayer.getStopByIndex(stopIndex);
      return s==null ? "null" : s.getName() + "(" + stopIndex + ")";
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

}
