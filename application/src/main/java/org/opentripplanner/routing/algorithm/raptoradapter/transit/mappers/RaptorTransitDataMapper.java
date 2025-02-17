package org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers;

import static org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.TransfersMapper.mapTransfers;

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
import org.opentripplanner.raptor.api.model.RaptorCostConverter;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RaptorTransitData;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.Transfer;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitTuningParameters;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripPatternForDate;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.constrainedtransfer.ConstrainedTransfersForPatterns;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.constrainedtransfer.TransferIndexGenerator;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.RaptorRequestTransferCache;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.StopTransferPriority;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.transit.service.TransitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maps the RaptorTransitData object from the TimetableRepository object. The ServiceDay hierarchy is reversed,
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

  private RaptorTransitDataMapper(TimetableRepository timetableRepository) {
    this.transitService = new DefaultTransitService(timetableRepository);
    this.siteRepository = timetableRepository.getSiteRepository();
  }

  public static RaptorTransitData map(
    TransitTuningParameters tuningParameters,
    TimetableRepository timetableRepository
  ) {
    return new RaptorTransitDataMapper(timetableRepository).map(tuningParameters);
  }

  private RaptorTransitData map(TransitTuningParameters tuningParameters) {
    HashMap<LocalDate, List<TripPatternForDate>> tripPatternsByStopByDate;
    List<List<Transfer>> transfersByStopIndex;
    ConstrainedTransfersForPatterns constrainedTransfers = null;

    LOG.info("Mapping raptorTransitData from TimetableRepository...");

    Collection<TripPattern> allTripPatterns = transitService.listTripPatterns();

    tripPatternsByStopByDate = mapTripPatterns(allTripPatterns);

    transfersByStopIndex = mapTransfers(siteRepository, transitService);

    TransferIndexGenerator transferIndexGenerator = null;
    if (OTPFeature.TransferConstraints.isOn()) {
      transferIndexGenerator =
        new TransferIndexGenerator(transitService.getTransferService().listAll(), allTripPatterns);
      constrainedTransfers = transferIndexGenerator.generateTransfers();
    }

    var transferCache = new RaptorRequestTransferCache(tuningParameters.transferCacheMaxSize());

    LOG.info("Mapping complete.");

    return new RaptorTransitData(
      tripPatternsByStopByDate,
      transfersByStopIndex,
      transitService.getTransferService(),
      siteRepository,
      transferCache,
      constrainedTransfers,
      transferIndexGenerator,
      createStopBoardAlightTransferCosts(siteRepository, tuningParameters)
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
    allServiceDates
      .parallelStream()
      .forEach(serviceDate -> {
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
