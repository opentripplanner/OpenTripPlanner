package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.opentripplanner.model.transfer.TransferService;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.constrainedtransfer.ConstrainedTransfersForPatterns;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.constrainedtransfer.TransferIndexGenerator;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.RaptorRequestTransferCache;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.StopModel;

public class TransitLayer {

  /**
   * Transit data required for routing, indexed by each local date(Graph TimeZone) it runs through.
   * A Trip "runs through" a date if any of its arrivals or departures is happening on that date.
   */
  private final HashMap<LocalDate, List<TripPatternForDate>> tripPatternsRunningOnDate;

  /**
   * Index of outer list is from stop index, inner list index has no specific meaning. To stop index
   * is a field of the Transfer object.
   */
  private final List<List<Transfer>> transfersByStopIndex;

  /**
   * Trip to trip transfers like with properties like guaranteedTransfer, staySeated and priority.
   */
  private final TransferService transferService;

  private final StopModel stopModel;

  private final ZoneId transitDataZoneId;

  private final RaptorRequestTransferCache transferCache;

  private ConstrainedTransfersForPatterns constrainedTransfers;

  private final TransferIndexGenerator transferIndexGenerator;

  private final int[] stopBoardAlightCosts;

  /**
   * Makes a shallow copy of the TransitLayer, except for the tripPatternsForDate, where a shallow
   * copy of the HashMap is made. This is sufficient, as the TransitLayerUpdater will replace entire
   * keys and their values in the map.
   */
  public TransitLayer(TransitLayer transitLayer) {
    this(
      transitLayer.tripPatternsRunningOnDate,
      transitLayer.transfersByStopIndex,
      transitLayer.transferService,
      transitLayer.stopModel,
      transitLayer.transitDataZoneId,
      transitLayer.transferCache,
      transitLayer.constrainedTransfers,
      transitLayer.transferIndexGenerator,
      transitLayer.stopBoardAlightCosts
    );
  }

  public TransitLayer(
    Map<LocalDate, List<TripPatternForDate>> tripPatternsRunningOnDate,
    List<List<Transfer>> transfersByStopIndex,
    TransferService transferService,
    StopModel stopModel,
    ZoneId transitDataZoneId,
    RaptorRequestTransferCache transferCache,
    ConstrainedTransfersForPatterns constrainedTransfers,
    TransferIndexGenerator transferIndexGenerator,
    int[] stopBoardAlightCosts
  ) {
    this.tripPatternsRunningOnDate = new HashMap<>(tripPatternsRunningOnDate);
    this.transfersByStopIndex = transfersByStopIndex;
    this.transferService = transferService;
    this.stopModel = stopModel;
    this.transitDataZoneId = transitDataZoneId;
    this.transferCache = transferCache;
    this.constrainedTransfers = constrainedTransfers;
    this.transferIndexGenerator = transferIndexGenerator;
    this.stopBoardAlightCosts = stopBoardAlightCosts;
  }

  @Nullable
  public StopLocation getStopByIndex(int stop) {
    return stop == -1 ? null : this.stopModel.stopByIndex(stop);
  }

  public Collection<TripPatternForDate> getTripPatternsForDate(LocalDate date) {
    return tripPatternsRunningOnDate.getOrDefault(date, List.of());
  }

  /**
   * This is the time zone which is used for interpreting all local "service" times (in transfers,
   * trip schedules and so on). This is the time zone of the internal OTP time - which is used in
   * logging and debugging. This is independent of the time zone of imported data and of the time
   * zone used on any API - it can be the same, but it does not need to.
   */
  public ZoneId getTransitDataZoneId() {
    return transitDataZoneId;
  }

  public int getStopCount() {
    return stopModel.stopIndexSize();
  }

  public List<TripPatternForDate> getTripPatternsRunningOnDateCopy(LocalDate runningPeriodDate) {
    List<TripPatternForDate> tripPatternForDate = tripPatternsRunningOnDate.get(runningPeriodDate);
    return tripPatternForDate != null ? new ArrayList<>(tripPatternForDate) : new ArrayList<>();
  }

  public List<TripPatternForDate> getTripPatternsStartingOnDateCopy(LocalDate date) {
    List<TripPatternForDate> tripPatternsRunningOnDate = getTripPatternsRunningOnDateCopy(date);
    return tripPatternsRunningOnDate
      .stream()
      .filter(t -> t.getLocalDate().equals(date))
      .collect(Collectors.toList());
  }

  public TransferService getTransferService() {
    return transferService;
  }

  public RaptorTransferIndex getRaptorTransfersForRequest(RouteRequest request) {
    return transferCache.get(transfersByStopIndex, request);
  }

  public RaptorRequestTransferCache getTransferCache() {
    return transferCache;
  }

  @Nullable
  public ConstrainedTransfersForPatterns getConstrainedTransfers() {
    return constrainedTransfers;
  }

  public TransferIndexGenerator getTransferIndexGenerator() {
    return transferIndexGenerator;
  }

  public int[] getStopBoardAlightCosts() {
    return stopBoardAlightCosts;
  }

  /**
   * Replaces all the TripPatternForDates for a single date. This is an atomic operation according
   * to the HashMap implementation.
   */
  public void replaceTripPatternsForDate(
    LocalDate date,
    List<TripPatternForDate> tripPatternForDates
  ) {
    this.tripPatternsRunningOnDate.replace(date, tripPatternForDates);
  }

  public void setConstrainedTransfers(ConstrainedTransfersForPatterns constrainedTransfers) {
    this.constrainedTransfers = constrainedTransfers;
  }
}
