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
import org.opentripplanner.graph_builder.module.geometry.GeometryProcessor;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.Trip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InterlineProcessor {

  private final int maxInterlineDistance;
  private final DataImportIssueStore issueStore;
  private static final Logger LOG = LoggerFactory.getLogger(GeometryProcessor.class);

  public InterlineProcessor(int maxInterlineDistance, DataImportIssueStore issueStore) {
    this.maxInterlineDistance = maxInterlineDistance > 0 ? maxInterlineDistance : 200;
    this.issueStore = issueStore;
  }

  /**
   * Identify interlined trips (where a physical vehicle continues on to another logical trip) and
   * update the TripPatterns accordingly.
   */
  public Multimap<P2<TripPattern>, P2<Trip>> getInterlinedTrips(
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

    LOG.info("Done finding interlining trips.");

    return interlines;
  }
}

/**
 * This compound key object is used when grouping interlining trips together by (serviceId,
 * blockId).
 */
record BlockIdAndServiceId(String blockId, FeedScopedId serviceId) {
  static BlockIdAndServiceId ofTrip(Trip trip) {
    return new BlockIdAndServiceId(trip.getGtfsBlockId(), trip.getServiceId());
  }
}
