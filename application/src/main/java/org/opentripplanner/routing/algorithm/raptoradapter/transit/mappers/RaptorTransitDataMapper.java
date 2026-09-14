package org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers;

import static org.opentripplanner.transfer.regular.model.TransfersMapper.mapTransfers;

import com.google.common.collect.ArrayListMultimap;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.graph_builder.module.transfer.api.TransferProfilesConfig;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.raptor.data.stop.StopIndex;
import org.opentripplanner.raptor.data.transfers.regular.streetadapter.StreetTransferPathProvider;
import org.opentripplanner.raptor.spi.RaptorCostConverter;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RaptorTransitData;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitTuningParameters;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripPatternForDate;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.transfercache.RaptorRequestTransferCache;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.transfer.constrained.raptoradaptor.ConstrainedTransfersForPatterns;
import org.opentripplanner.transfer.constrained.raptoradaptor.TransferIndexGenerator;
import org.opentripplanner.transfer.regular.TransferRepository;
import org.opentripplanner.transfer.regular.model.PathTransfer;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopTransferPriority;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.transit.service.TransitRepository;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.transit.transfer.regular.RaptorRegularTransferServiceFactory;
import org.opentripplanner.transit.transfer.regular.internal.RegularTransferRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maps the RaptorTransitData object from the TransitRepository object. The ServiceDay hierarchy is reversed,
 * with service days at the top level, which contains TripPatternForDate objects that contain only
 * TripSchedules running on that particular date. This makes it faster to filter out TripSchedules
 * when doing Range Raptor searches.
 * <p>
 * CONCURRENCY: This mapper runs part of the mapping in parallel using parallel streams. This
 * improves startup time on the Norwegian network by 20 seconds, by reducing this mapper from 36
 * seconds to 15 seconds, and the total startup time from 80 seconds to 60 seconds. (JAN 2020,
 * MacBook Pro, 3.1 GHz i7)
 */
public class RaptorTransitDataMapper {

  private static final Logger LOG = LoggerFactory.getLogger(RaptorTransitDataMapper.class);

  private final TransitService transitService;
  private final SiteRepository siteRepository;
  private final TransferRepository transferRepository;
  private final Graph graph;
  private final TransitRepository transitRepository;
  private final RegularTransferRepository<NearbyStop> regularTransferRepository;
  private final TransferProfilesConfig transferProfilesConfig;

  private RaptorTransitDataMapper(
    Graph graph,
    TransitRepository transitRepository,
    TransferRepository transferRepository,
    RegularTransferRepository<NearbyStop> regularTransferRepository,
    TransferProfilesConfig transferProfilesConfig
  ) {
    this.transitService = new DefaultTransitService(transitRepository);
    this.siteRepository = transitRepository.getSiteRepository();
    this.transferRepository = transferRepository;
    this.graph = graph;
    this.transitRepository = transitRepository;
    this.regularTransferRepository = regularTransferRepository;
    this.transferProfilesConfig = transferProfilesConfig;
  }

  /**
   * Without raptor-data wiring - the permanent graph-less/test entry point. Regular transfers
   * fall back to the {@code RaptorTransferIndex} mechanism built from {@code transferRepository}.
   * Used by tests and any deployment building {@code RaptorTransitData} without a {@code Graph}.
   */
  public static RaptorTransitData map(
    TransitTuningParameters tuningParameters,
    TransitRepository transitRepository,
    TransferRepository transferRepository
  ) {
    return map(tuningParameters, null, transitRepository, transferRepository, null, null);
  }

  public static RaptorTransitData map(
    TransitTuningParameters tuningParameters,
    @Nullable Graph graph,
    TransitRepository transitRepository,
    TransferRepository transferRepository,
    @Nullable RegularTransferRepository<NearbyStop> regularTransferRepository,
    @Nullable TransferProfilesConfig transferProfilesConfig
  ) {
    return new RaptorTransitDataMapper(
      graph,
      transitRepository,
      transferRepository,
      regularTransferRepository,
      transferProfilesConfig
    ).map(tuningParameters);
  }

  private RaptorTransitData map(TransitTuningParameters tuningParameters) {
    HashMap<LocalDate, List<TripPatternForDate>> tripPatternsByStopByDate;
    List<List<PathTransfer>> transfersByStopIndex;
    ConstrainedTransfersForPatterns constrainedTransfers = null;

    LOG.info("Mapping raptorTransitData from TransitRepository...");

    Collection<TripPattern> allTripPatterns = transitService.listTripPatterns();

    tripPatternsByStopByDate = mapTripPatterns(allTripPatterns);

    transfersByStopIndex = mapTransfers(siteRepository, transferRepository);

    TransferIndexGenerator transferIndexGenerator = null;
    if (OTPFeature.TransferConstraints.isOn()) {
      transferIndexGenerator = new TransferIndexGenerator(
        transitService.getConstrainedTransferService().listAll(),
        allTripPatterns
      );
      constrainedTransfers = transferIndexGenerator.generateTransfers();
    }

    var transferCache = new RaptorRequestTransferCache(tuningParameters.transferCacheMaxSize());
    var regularTransferServiceFactory = createRegularTransferServiceFactory();

    LOG.info("Mapping complete.");

    return new RaptorTransitData(
      tripPatternsByStopByDate,
      transfersByStopIndex,
      transitService.getConstrainedTransferService(),
      siteRepository,
      transferCache,
      constrainedTransfers,
      transferIndexGenerator,
      createStopBoardAlightTransferCosts(siteRepository, tuningParameters),
      transferProfilesConfig,
      regularTransferServiceFactory
    );
  }

  /**
   * Built once per process (like {@code transferCache} above), not per-request - mirrors how
   * {@code RaptorRequestTransferCache} is used. {@code null} unless raptor-data wiring was
   * supplied to the mapper (see the two-overload {@code map(...)} above).
   */
  @Nullable
  private RaptorRegularTransferServiceFactory<
    NearbyStop,
    RouteRequest
  > createRegularTransferServiceFactory() {
    if (graph == null || regularTransferRepository == null || transferProfilesConfig == null) {
      return null;
    }
    var stopIndex = new StopIndex(
      siteRepository.stopIndexSize(),
      siteRepository.listRegularStops(),
      RegularStop::getIndex,
      RegularStop::getId
    );
    var nearbyStopFinder = StreetTransferPathProvider.createNearbyStopFinder(
      graph,
      transitRepository
    );
    var pathProvider = new StreetTransferPathProvider(
      graph,
      nearbyStopFinder,
      transferProfilesConfig,
      transitRepository
    );
    return new RaptorRegularTransferServiceFactory<>(
      stopIndex,
      regularTransferRepository,
      pathProvider
    );
  }

  /**
   * Map pre-Raptor TripPatterns and Trips to the corresponding Raptor classes.
   * <p>
   * Part of this method runs IN PARALLEL.
   * <p>
   */
  private HashMap<LocalDate, List<TripPatternForDate>> mapTripPatterns(
    Collection<TripPattern> allTripPatterns
  ) {
    TripPatternForDateMapper tripPatternForDateMapper = new TripPatternForDateMapper(
      transitService.getServiceCodesRunningForDate()
    );

    Set<LocalDate> allServiceDates = transitService.listServiceDates();

    List<TripPatternForDate> tripPatternForDates = Collections.synchronizedList(new ArrayList<>());

    // THIS CODE RUNS IN PARALLEL
    allServiceDates.parallelStream().forEach(serviceDate -> {
      // Create a List to hold the values for this iteration. The results are then added
      // to the common synchronized list at the end.
      List<TripPatternForDate> values = new ArrayList<>();

      // This nested loop could be quite inefficient.
      // Maybe determine in advance which patterns are running on each service and day.
      for (TripPattern oldTripPattern : allTripPatterns) {
        TripPatternForDate tripPatternForDate = tripPatternForDateMapper.map(
          oldTripPattern.getScheduledTimetable(),
          serviceDate
        );
        if (tripPatternForDate != null) {
          values.add(tripPatternForDate);
        }
      }
      if (!values.isEmpty()) {
        tripPatternForDates.addAll(values);
      }
    });
    // END PARALLEL CODE

    return keyByRunningPeriodDates(tripPatternForDates);
  }

  /**
   * Returns a map of TripPatternsForDate objects by their active dates.
   */
  private HashMap<LocalDate, List<TripPatternForDate>> keyByRunningPeriodDates(
    List<TripPatternForDate> tripPatternForDates
  ) {
    // Create multimap by running period dates
    ArrayListMultimap<LocalDate, TripPatternForDate> multiMap = ArrayListMultimap.create();
    for (TripPatternForDate tripPatternForDate : tripPatternForDates) {
      for (LocalDate date : tripPatternForDate.getRunningPeriodDates()) {
        multiMap.put(date, tripPatternForDate);
      }
    }

    // Convert to Map<LocalDate, List<TripPatternForDate>>
    HashMap<LocalDate, List<TripPatternForDate>> result = new HashMap<>();
    for (Map.Entry<LocalDate, Collection<TripPatternForDate>> entry : multiMap.asMap().entrySet()) {
      result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
    }

    return result;
  }

  /**
   * Create static board/alight cost for Raptor to apply during transfer
   */
  @Nullable
  static int[] createStopBoardAlightTransferCosts(
    SiteRepository stops,
    TransitTuningParameters tuningParams
  ) {
    if (!tuningParams.enableStopTransferPriority()) {
      return null;
    }
    int defaultCost = RaptorCostConverter.toRaptorCost(
      tuningParams.stopBoardAlightDuringTransferCost(StopTransferPriority.defaultValue())
    );
    int[] stopBoardAlightTransferCosts = new int[stops.stopIndexSize()];

    for (int i = 0; i < stops.stopIndexSize(); ++i) {
      // There can be holes in the stop index, so we need to account for 'null' here.
      var stop = stops.stopByIndex(i);
      if (stop == null) {
        stopBoardAlightTransferCosts[i] = defaultCost;
      } else {
        var priority = stop.getPriority();
        int domainCost = tuningParams.stopBoardAlightDuringTransferCost(priority);
        stopBoardAlightTransferCosts[i] = RaptorCostConverter.toRaptorCost(domainCost);
      }
    }
    return stopBoardAlightTransferCosts;
  }
}
