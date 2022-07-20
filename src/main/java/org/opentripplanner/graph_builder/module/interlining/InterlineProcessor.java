package org.opentripplanner.graph_builder.module.interlining;

import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.InterliningTeleport;
import org.opentripplanner.gtfs.mapping.StaySeatedNotAllowed;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.transfer.ConstrainedTransfer;
import org.opentripplanner.model.transfer.TransferConstraint;
import org.opentripplanner.model.transfer.TransferPriority;
import org.opentripplanner.model.transfer.TransferService;
import org.opentripplanner.model.transfer.TripTransferPoint;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.Trip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InterlineProcessor {

  private static final Logger LOG = LoggerFactory.getLogger(InterlineProcessor.class);
  private final TransferService transferService;
  private final int maxInterlineDistance;
  private final DataImportIssueStore issueStore;
  private final List<StaySeatedNotAllowed> staySeatedNotAllowed;

  public InterlineProcessor(
    TransferService transferService,
    List<StaySeatedNotAllowed> staySeatedNotAllowed,
    int maxInterlineDistance,
    DataImportIssueStore issueStore
  ) {
    this.transferService = transferService;
    this.staySeatedNotAllowed = staySeatedNotAllowed;
    this.maxInterlineDistance = maxInterlineDistance > 0 ? maxInterlineDistance : 200;
    this.issueStore = issueStore;
  }

  public List<ConstrainedTransfer> run(Collection<TripPattern> tripPatterns) {
    var interlinedTrips = this.getInterlinedTrips(tripPatterns);
    var transfers = interlinedTrips
      .entries()
      .stream()
      .filter(this::staySeatedAllowed)
      .map(p -> {
        var constraint = TransferConstraint.create();
        constraint.staySeated();
        constraint.priority(TransferPriority.ALLOWED);

        var fromTrip = p.getValue().first;
        var toTrip = p.getValue().second;

        var from = new TripTransferPoint(fromTrip, p.getKey().first.numberOfStops() - 1);
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
        interlinedTrips.keySet().size()
      );

      transferService.addAll(transfers);
    }
    return transfers;
  }

  private boolean staySeatedAllowed(Map.Entry<P2<TripPattern>, P2<Trip>> p) {
    var fromTrip = p.getValue().first;
    var toTrip = p.getValue().second;
    return staySeatedNotAllowed
      .stream()
      .noneMatch(t ->
        t.fromTrip().getId().equals(fromTrip.getId()) && t.toTrip().getId().equals(toTrip.getId())
      );
  }

  /**
   * Identify interlined trips (where a physical vehicle continues on to another logical trip).
   */
  private Multimap<P2<TripPattern>, P2<Trip>> getInterlinedTrips(
    Collection<TripPattern> tripPatterns
  ) {
    /* Record which Pattern each interlined TripTimes belongs to. */
    Map<TripTimes, TripPattern> patternForTripTimes = new HashMap<>();

    /* TripTimes grouped by the block ID and service ID of their trips. Must be a ListMultimap to allow sorting. */
    ListMultimap<BlockIdAndServiceId, TripTimes> tripTimesForBlock = ArrayListMultimap.create();

    LOG.info("Finding interlining trips based on block IDs.");
    for (TripPattern pattern : tripPatterns) {
      Timetable timetable = pattern.getScheduledTimetable();
      /* TODO: Block semantics seem undefined for frequency trips, so skip them? */
      for (TripTimes tripTimes : timetable.getTripTimes()) {
        Trip trip = tripTimes.getTrip();
        if (!Strings.isNullOrEmpty(trip.getGtfsBlockId())) {
          tripTimesForBlock.put(BlockIdAndServiceId.ofTrip(trip), tripTimes);
          // For space efficiency, only record times that are part of a block.
          patternForTripTimes.put(tripTimes, pattern);
        }
      }
    }

    // Associate pairs of TripPatterns with lists of trips that continue from one pattern to the other.
    Multimap<P2<TripPattern>, P2<Trip>> interlines = ArrayListMultimap.create();

    // Sort trips within each block by first departure time, then iterate over trips in this block and service,
    // linking them. Has no effect on single-trip blocks.
    SERVICE_BLOCK:for (BlockIdAndServiceId block : tripTimesForBlock.keySet()) {
      List<TripTimes> blockTripTimes = tripTimesForBlock.get(block);
      Collections.sort(blockTripTimes);
      TripTimes prev = null;
      for (TripTimes curr : blockTripTimes) {
        if (prev != null) {
          if (prev.getDepartureTime(prev.getNumStops() - 1) > curr.getArrivalTime(0)) {
            LOG.error(
              "Trip times within block {} are not increasing on service {} after trip {}.",
              block.blockId(),
              block.serviceId(),
              prev.getTrip().getId()
            );
            continue SERVICE_BLOCK;
          }
          TripPattern prevPattern = patternForTripTimes.get(prev);
          TripPattern currPattern = patternForTripTimes.get(curr);
          var fromStop = prevPattern.lastStop();
          var toStop = currPattern.firstStop();
          double teleportationDistance = SphericalDistanceLibrary.fastDistance(
            fromStop.getLat(),
            fromStop.getLon(),
            toStop.getLat(),
            toStop.getLon()
          );
          if (teleportationDistance > maxInterlineDistance) {
            issueStore.add(
              new InterliningTeleport(prev.getTrip(), block.blockId(), (int) teleportationDistance)
            );
            // Only skip this particular interline edge; there may be other valid ones in the block.
          } else {
            interlines.put(
              new P2<>(prevPattern, currPattern),
              new P2<>(prev.getTrip(), curr.getTrip())
            );
          }
        }
        prev = curr;
      }
    }

    return interlines;
  }

  /**
   * This compound key object is used when grouping interlining trips together by (serviceId,
   * blockId).
   */
  private record BlockIdAndServiceId(String blockId, FeedScopedId serviceId) {
    static BlockIdAndServiceId ofTrip(Trip trip) {
      return new BlockIdAndServiceId(trip.getGtfsBlockId(), trip.getServiceId());
    }
  }
}
