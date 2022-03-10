package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.model.transfer.TransferService;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RaptorTransferIndex;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.DefaultCostCalculator;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.DateMapper;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.McCostParamsMapper;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.transit.raptor.api.transit.CostCalculator;
import org.opentripplanner.transit.raptor.api.transit.IntIterator;
import org.opentripplanner.transit.raptor.api.transit.RaptorConstrainedTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorPathConstrainedTransferSearch;
import org.opentripplanner.transit.raptor.api.transit.RaptorRoute;
import org.opentripplanner.transit.raptor.api.transit.RaptorStopNameResolver;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransitDataProvider;
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
   * Active trip patterns by stop index
   */
  private final List<List<TripPatternForDates>> activeTripPatternsPerStop;

  /**
   * Transfers by stop index
   */
  private final RaptorTransferIndex transfers;

  private final ZonedDateTime transitSearchTimeZero;

  private final CostCalculator generalizedCostCalculator;

  private final int validTransitDataStartTime;

  private final int validTransitDataEndTime;

  public RaptorRoutingRequestTransitData(
      TransferService transferService,
      TransitLayer transitLayer,
      ZonedDateTime transitSearchTimeZero,
      int additionalPastSearchDays,
      int additionalFutureSearchDays,
      TransitDataProviderFilter filter,
      RoutingRequest routingRequest
  ) {

    this.transferService = transferService;
    this.transitLayer = transitLayer;
    this.transitSearchTimeZero = transitSearchTimeZero;

    // Delegate to the creator to construct the needed data structures. The code is messy so
    // it is nice to NOT have it in the class. It isolate this code to only be available at
    // the time of construction
    var transitDataCreator = new RaptorRoutingRequestTransitDataCreator(
            transitLayer, transitSearchTimeZero
    );
    this.activeTripPatternsPerStop = transitDataCreator.createTripPatternsPerStop(
        additionalPastSearchDays,
        additionalFutureSearchDays,
        filter
    );
    this.transfers = transitLayer.getRaptorTransfersForRequest(routingRequest);
    this.generalizedCostCalculator = new DefaultCostCalculator(
            McCostParamsMapper.map(routingRequest),
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
  public Iterator<? extends RaptorRoute<TripSchedule>> routeIterator(IntIterator stops) {
    // A LinkedHashSet is used so that the iteration order is deterministic.
    Set<TripPatternForDates> activeTripPatternsForGivenStops = new LinkedHashSet<>();
    while (stops.hasNext()) {
      activeTripPatternsForGivenStops.addAll(activeTripPatternsPerStop.get(stops.next()));
    }
    return activeTripPatternsForGivenStops.iterator();
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
