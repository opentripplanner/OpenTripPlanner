package org.opentripplanner.gtfs.interlining;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.InterliningTeleport;
import org.opentripplanner.gtfs.mapping.StaySeatedNotAllowed;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.model.transfer.ConstrainedTransfer;
import org.opentripplanner.model.transfer.DefaultTransferService;
import org.opentripplanner.model.transfer.TransferConstraint;
import org.opentripplanner.model.transfer.TransferPriority;
import org.opentripplanner.model.transfer.TripTransferPoint;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.utils.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InterlineProcessor {

  private static final Logger LOG = LoggerFactory.getLogger(InterlineProcessor.class);
  private final DefaultTransferService transferService;
  private final int maxInterlineDistance;
  private final DataImportIssueStore issueStore;
  private final List<StaySeatedNotAllowed> staySeatedNotAllowed;
  private final LocalDate transitServiceStart;
  private final int daysInTransitService;
  private final CalendarServiceData calendarServiceData;
  private final Map<FeedScopedId, BitSet> daysOfServices = new HashMap<>();

  public InterlineProcessor(
    DefaultTransferService transferService,
    List<StaySeatedNotAllowed> staySeatedNotAllowed,
    int maxInterlineDistance,
    DataImportIssueStore issueStore,
    CalendarServiceData calendarServiceData
  ) {
    this.transferService = transferService;
    this.staySeatedNotAllowed = staySeatedNotAllowed;
    this.maxInterlineDistance = maxInterlineDistance > 0 ? maxInterlineDistance : 200;
    this.issueStore = issueStore;
    this.transitServiceStart = calendarServiceData.getFirstDate().orElse(null);
    this.daysInTransitService = calendarServiceData
      .getLastDate()
      .map(lastDate -> (int) ChronoUnit.DAYS.between(transitServiceStart, lastDate) + 1)
      .orElse(0);
    this.calendarServiceData = calendarServiceData;
  }

  public List<ConstrainedTransfer> run(Collection<TripPattern> tripPatterns) {
    if (daysInTransitService == 0) {
      return List.of();
    }
    var interlinedTrips = this.getInterlinedTrips(tripPatterns);
    var transfers = interlinedTrips
      .entries()
      .stream()
      .filter(this::staySeatedAllowed)
      .map(p -> {
        var constraint = TransferConstraint.of();
        constraint.staySeated();
        constraint.priority(TransferPriority.ALLOWED);

        var fromTrip = p.getValue().from();
        var toTrip = p.getValue().to();

        var from = new TripTransferPoint(fromTrip, p.getKey().from().numberOfStops() - 1);
        var to = new TripTransferPoint(toTrip, 0);

        LOG.debug(
          "Creating stay-seated transfer from trip {} (route {}) to trip {} (route {})",
          fromTrip.getId(),
          fromTrip.getRoute().getId(),
          toTrip.getId(),
          toTrip.getRoute().getId()
        );

        return new ConstrainedTransfer(null, from, to, constraint.build());
      })
      .toList();

    if (!transfers.isEmpty()) {
      LOG.info(
        "Found {} pairs of trips for which stay-seated (interlined) transfers were created",
        transfers.size()
      );

      transferService.addAll(transfers);
    }
    return transfers;
  }

  private boolean staySeatedAllowed(Map.Entry<TripPatternPair, TripPair> p) {
    var fromTrip = p.getValue().from();
    var toTrip = p.getValue().to();
    return staySeatedNotAllowed
      .stream()
      .noneMatch(
        t ->
          t.fromTrip().getId().equals(fromTrip.getId()) && t.toTrip().getId().equals(toTrip.getId())
      );
  }

  /**
   * Identify interlined trips (where a physical vehicle continues on to another logical trip).
   */
  private Multimap<TripPatternPair, TripPair> getInterlinedTrips(
    Collection<TripPattern> tripPatterns
  ) {
    /* Record which Pattern each interlined TripTimes belongs to. */
    Map<TripTimes, TripPattern> patternForTripTimes = new HashMap<>();

    /* TripTimes grouped by the block ID of their trips. Must be a ListMultimap to allow sorting. */
    ListMultimap<String, TripTimes> tripTimesForBlockId = ArrayListMultimap.create();

    LOG.info("Finding interlining trips based on block IDs.");
    for (TripPattern pattern : tripPatterns) {
      Timetable timetable = pattern.getScheduledTimetable();
      /* TODO: Block semantics seem undefined for frequency trips, so skip them? */
      for (TripTimes tripTimes : timetable.getTripTimes()) {
        Trip trip = tripTimes.getTrip();
        if (StringUtils.hasValue(trip.getGtfsBlockId())) {
          tripTimesForBlockId.put(trip.getGtfsBlockId(), tripTimes);
          // For space efficiency, only record times that are part of a block.
          patternForTripTimes.put(tripTimes, pattern);
        }
      }
    }

    // Associate pairs of TripPatterns with lists of trips that continue from one pattern to the other.
    Multimap<TripPatternPair, TripPair> interlines = ArrayListMultimap.create();

    // Sort trips within each block by first departure time, then iterate over trips in this block,
    // linking them. One from trip can have multiple interline transfers if trip which interlines
    // with the from trip doesn't operate on every service date of the from trip.
    for (String blockId : tripTimesForBlockId.keySet()) {
      List<TripTimes> blockTripTimes = tripTimesForBlockId.get(blockId);
      Collections.sort(blockTripTimes);
      for (int i = 0; i < blockTripTimes.size(); i++) {
        var fromTripTimes = blockTripTimes.get(i);
        var fromServiceId = fromTripTimes.getTrip().getServiceId();
        BitSet uncoveredDays = getAndCopyDaysForService(fromServiceId);
        for (int j = i + 1; j < blockTripTimes.size(); j++) {
          var toTripTimes = blockTripTimes.get(j);
          var toServiceId = toTripTimes.getTrip().getServiceId();
          if (
            toServiceId.equals(fromServiceId) &&
            createInterline(fromTripTimes, toTripTimes, blockId, patternForTripTimes, interlines)
          ) {
            break;
          }
          BitSet daysForToTripTimes = getDaysForService(toTripTimes.getTrip().getServiceId());
          if (
            uncoveredDays.intersects(daysForToTripTimes) &&
            createInterline(fromTripTimes, toTripTimes, blockId, patternForTripTimes, interlines)
          ) {
            uncoveredDays.andNot(daysForToTripTimes);
            if (uncoveredDays.isEmpty()) {
              break;
            }
          }
        }
      }
    }

    return interlines;
  }

  /**
   * Validates that trip times are continuous and that the transfer stop(s) are not too far away
   * from each other. Then creates interline between the trips.
   *
   * @return true if interline has been created or if there is an issue preventing an interline
   * creation for certain service dates.
   */
  private boolean createInterline(
    TripTimes fromTripTimes,
    TripTimes toTripTimes,
    String blockId,
    Map<TripTimes, TripPattern> patternForTripTimes,
    Multimap<TripPatternPair, TripPair> interlines
  ) {
    if (
      fromTripTimes.getDepartureTime(fromTripTimes.getNumStops() - 1) >
      toTripTimes.getArrivalTime(0)
    ) {
      LOG.error(
        "Trip times within block {} are not increasing on after trip {}.",
        blockId,
        fromTripTimes.getTrip().getId()
      );
      return true;
    }
    var fromPattern = patternForTripTimes.get(fromTripTimes);
    var toPattern = patternForTripTimes.get(toTripTimes);
    var fromStop = fromPattern.lastStop();
    var toStop = toPattern.firstStop();
    double teleportationDistance = SphericalDistanceLibrary.fastDistance(
      fromStop.getLat(),
      fromStop.getLon(),
      toStop.getLat(),
      toStop.getLon()
    );
    if (teleportationDistance > maxInterlineDistance) {
      issueStore.add(
        new InterliningTeleport(
          fromTripTimes.getTrip(),
          blockId,
          (int) teleportationDistance,
          fromStop,
          toStop
        )
      );
      // Only skip this particular interline edge; there may be other valid ones in the block for the
      // from trip.
      return false;
    } else {
      interlines.put(
        new TripPatternPair(fromPattern, toPattern),
        new TripPair(fromTripTimes.getTrip(), toTripTimes.getTrip())
      );
      return true;
    }
  }

  /**
   * This method should only be used when the returned {@link BitSet} is not altered as the returned
   * value is cached for future use. If the BitSet needs to be modified, use
   * {@link #getAndCopyDaysForService(FeedScopedId)}
   *
   * @return {@link BitSet} which index starts at the first overall date of the services and the
   * last index is the last date.
   */
  private BitSet getDaysForService(FeedScopedId serviceId) {
    BitSet daysForService = this.daysOfServices.get(serviceId);
    if (daysForService == null) {
      daysForService = new BitSet(daysInTransitService);
      var serviceDates = calendarServiceData.getServiceDatesForServiceId(serviceId);
      if (serviceDates != null) {
        for (LocalDate serviceDate : serviceDates) {
          int daysBetween = (int) ChronoUnit.DAYS.between(transitServiceStart, serviceDate);
          daysForService.set(daysBetween);
        }
      }
      daysOfServices.put(serviceId, daysForService);
    }
    return daysForService;
  }

  /**
   * This {@link BitSet} returned from this method can be modified. If there is no need to modify
   * it, {@link #getDaysForService(FeedScopedId)} can be used instead.
   *
   * @return {@link BitSet} which index starts at the first overall date of the services and the
   * last index is the last date.
   */
  private BitSet getAndCopyDaysForService(FeedScopedId serviceId) {
    return (BitSet) getDaysForService(serviceId).clone();
  }

  private record TripPatternPair(TripPattern from, TripPattern to) {}

  private record TripPair(Trip from, Trip to) {}
}
