package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import java.time.LocalDate;
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
import org.opentripplanner.transit.service.SiteRepository;

/**
 * This is a replica of public transportation data already present in TimetableRepository, but rearranged
 * and indexed differently for efficient use by the Raptor router. Patterns and trips are split out
 * by days, retaining only the services actually running on any particular day.
 *
 */
public class RaptorTransitData {

  /**
   * Transit data required for routing, indexed by each local date(Graph TimeZone) it runs through.
   * A Trip "runs through" a date if any of its arrivals or departures is happening on that date.
   * The same trip pattern can therefore have multiple running dates and trip pattern is not
   * required to "run" on its service date.
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

  private final SiteRepository siteRepository;

  private final RaptorRequestTransferCache transferCache;

  private ConstrainedTransfersForPatterns constrainedTransfers;

  private final TransferIndexGenerator transferIndexGenerator;

  @Nullable
  private final int[] stopBoardAlightTransferCosts;

  /**
   * Makes a shallow copy of the RaptorTransitData, except for the tripPatternsForDate, where a shallow
   * copy of the HashMap is made. This is sufficient, as the RealTimeRaptorTransitDataUpdater will replace entire
   * keys and their values in the map.
   */
  public RaptorTransitData(RaptorTransitData raptorTransitData) {
    this(
      raptorTransitData.tripPatternsRunningOnDate,
      raptorTransitData.transfersByStopIndex,
      raptorTransitData.transferService,
      raptorTransitData.siteRepository,
      raptorTransitData.transferCache,
      raptorTransitData.constrainedTransfers,
      raptorTransitData.transferIndexGenerator,
      raptorTransitData.stopBoardAlightTransferCosts
    );
  }

  public RaptorTransitData(
    Map<LocalDate, List<TripPatternForDate>> tripPatternsRunningOnDate,
    List<List<Transfer>> transfersByStopIndex,
    TransferService transferService,
    SiteRepository siteRepository,
    RaptorRequestTransferCache transferCache,
    ConstrainedTransfersForPatterns constrainedTransfers,
    TransferIndexGenerator transferIndexGenerator,
    @Nullable int[] stopBoardAlightTransferCosts
  ) {
    this.tripPatternsRunningOnDate = new HashMap<>(tripPatternsRunningOnDate);
    this.transfersByStopIndex = transfersByStopIndex;
    this.transferService = transferService;
    this.siteRepository = siteRepository;
    this.transferCache = transferCache;
    this.constrainedTransfers = constrainedTransfers;
    this.transferIndexGenerator = transferIndexGenerator;
    this.stopBoardAlightTransferCosts = stopBoardAlightTransferCosts;
  }

  @Nullable
  public StopLocation getStopByIndex(int stop) {
    return stop == -1 ? null : this.siteRepository.stopByIndex(stop);
  }

  /**
   * Returns trip patterns for the given running date. Running date is not necessarily the same
   * as the service date. A Trip "runs through" a date if any of its arrivals or departures is
   * happening on that date. Trip pattern can have multiple running dates.
   */
  public Collection<TripPatternForDate> getTripPatternsForRunningDate(LocalDate date) {
    return tripPatternsRunningOnDate.getOrDefault(date, List.of());
  }

  public int getStopCount() {
    return siteRepository.stopIndexSize();
  }

  /**
   * Returns a copy of the list of trip patterns for the given running date. Running date is not
   * necessarily the same as the service date. A Trip "runs through" a date if any of its arrivals
   * or departures is happening on that date. Trip pattern can have multiple running dates.
   */
  public List<TripPatternForDate> getTripPatternsRunningOnDateCopy(LocalDate runningPeriodDate) {
    List<TripPatternForDate> tripPatternForDate = tripPatternsRunningOnDate.get(runningPeriodDate);
    return tripPatternForDate != null ? new ArrayList<>(tripPatternForDate) : new ArrayList<>();
  }

  /**
   * Returns a copy of the list of trip patterns for the given service date. Service date is not
   * necessarily the same as any of the trip pattern's running dates.
   */
  public List<TripPatternForDate> getTripPatternsOnServiceDateCopy(LocalDate date) {
    List<TripPatternForDate> tripPatternsRunningOnDates = getTripPatternsRunningOnDateCopy(date);
    // Trip pattern can run only after midnight. Therefore, we need to get the trip pattern's for
    // the next running date as well and filter out duplicates.
    tripPatternsRunningOnDates.addAll(getTripPatternsRunningOnDateCopy(date.plusDays(1)));
    return tripPatternsRunningOnDates
      .stream()
      .filter(t -> t.getServiceDate().equals(date))
      .distinct()
      .collect(Collectors.toList());
  }

  public TransferService getTransferService() {
    return transferService;
  }

  public RaptorTransferIndex getRaptorTransfersForRequest(RouteRequest request) {
    return transferCache.get(transfersByStopIndex, request);
  }

  public void initTransferCacheForRequest(RouteRequest request) {
    transferCache.put(transfersByStopIndex, request);
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

  /**
   * Costs for both boarding and alighting at a given stop during transfer. Note that this is in
   * raptor centi-second units.
   */
  @Nullable
  public int[] getStopBoardAlightTransferCosts() {
    return stopBoardAlightTransferCosts;
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
