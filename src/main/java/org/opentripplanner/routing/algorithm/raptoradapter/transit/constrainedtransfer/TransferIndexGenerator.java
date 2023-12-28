package org.opentripplanner.routing.algorithm.raptoradapter.transit.constrainedtransfer;

import static org.opentripplanner.routing.algorithm.raptoradapter.transit.constrainedtransfer.TransferPointForPatternFactory.createTransferPointForPattern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import org.opentripplanner.model.transfer.ConstrainedTransfer;
import org.opentripplanner.model.transfer.RouteStationTransferPoint;
import org.opentripplanner.model.transfer.RouteStopTransferPoint;
import org.opentripplanner.model.transfer.StationTransferPoint;
import org.opentripplanner.model.transfer.StopTransferPoint;
import org.opentripplanner.model.transfer.TransferPoint;
import org.opentripplanner.model.transfer.TripTransferPoint;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.RoutingTripPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransferIndexGenerator {

  private static final Logger LOG = LoggerFactory.getLogger(TransferIndexGenerator.class);

  private static final boolean BOARD = true;
  private static final boolean ALIGHT = false;

  private final Collection<ConstrainedTransfer> constrainedTransfers;
  private final Map<Station, Set<RoutingTripPattern>> patternsByStation = new HashMap<>();
  private final Map<StopLocation, Set<RoutingTripPattern>> patternsByStop = new HashMap<>();
  private final Map<Route, Set<RoutingTripPattern>> patternsByRoute = new HashMap<>();
  private final Map<Trip, Set<RoutingTripPattern>> patternsByTrip = new HashMap<>();

  public TransferIndexGenerator(
    Collection<ConstrainedTransfer> constrainedTransfers,
    Collection<TripPattern> tripPatterns
  ) {
    this.constrainedTransfers = constrainedTransfers;
    setupPatterns(tripPatterns);
  }

  public ConstrainedTransfersForPatterns generateTransfers() {
    int nPatterns = RoutingTripPattern.indexCounter();
    TransferForPatternByStopPos[] forwardTransfers = new TransferForPatternByStopPos[nPatterns];
    TransferForPatternByStopPos[] reverseTransfers = new TransferForPatternByStopPos[nPatterns];

    for (ConstrainedTransfer tx : constrainedTransfers) {
      var c = tx.getTransferConstraint();
      // Only add transfers which have an effect on the Raptor routing here.
      // Some transfers only have the priority set, and that is used in optimized-
      // transfers, but not in Raptor.
      if (!c.includeInRaptorRouting()) {
        continue;
      }

      try {
        findTPoints(tx.getFrom(), ALIGHT)
          .stream()
          .filter(TPoint::canAlight)
          .forEachOrdered(fromPoint -> {
            for (var toPoint : findTPoints(tx.getTo(), BOARD)) {
              if (toPoint.canBoard() && !fromPoint.equals(toPoint)) {
                fromPoint.addTransferConstraints(tx, toPoint, forwardTransfers, reverseTransfers);
              }
            }
          });
      } catch (Exception e) {
        LOG.error("Unable to generate transfers: {}. Affected transfer: {}", e, tx);
      }
    }

    sortTransfers(forwardTransfers);
    sortTransfers(reverseTransfers);

    return new ConstrainedTransfersForPatterns(
      Arrays.asList(forwardTransfers),
      Arrays.asList(reverseTransfers)
    );
  }

  /**
   * Add information about a newly created pattern and timetables in the index, in order to be able
   * to create constrained transfers for these patterns.
   */
  public void addRealtimeTrip(TripPattern tripPattern, List<Trip> trips) {
    setupPattern(tripPattern, trips);
  }

  /**
   * Index scheduled patterns when loading the graph initially.
   */
  private void setupPatterns(Collection<TripPattern> tripPatterns) {
    for (TripPattern tripPattern : tripPatterns) {
      setupPattern(tripPattern, tripPattern.scheduledTripsAsStream().toList());
    }
  }

  private void setupPattern(TripPattern tripPattern, List<Trip> trips) {
    RoutingTripPattern pattern = tripPattern.getRoutingTripPattern();
    patternsByRoute.computeIfAbsent(tripPattern.getRoute(), t -> new HashSet<>()).add(pattern);

    for (Trip trip : trips) {
      patternsByTrip.computeIfAbsent(trip, t -> new HashSet<>()).add(pattern);
    }

    for (StopLocation stop : tripPattern.getStops()) {
      patternsByStop.computeIfAbsent(stop, t -> new HashSet<>()).add(pattern);
      Station station = stop.getParentStation();
      if (station != null) {
        patternsByStation.computeIfAbsent(station, t -> new HashSet<>()).add(pattern);
      }
    }
  }

  /** Sort trips in a TransferForPatternByStopPos, if it is not null */
  private void sortTransfers(TransferForPatternByStopPos[] transfers) {
    for (var transfersForStop : transfers) {
      if (transfersForStop != null) {
        transfersForStop.sortOnSpecificityRanking();
      }
    }
  }

  private Collection<TPoint> findTPoints(TransferPoint txPoint, boolean boarding) {
    if (txPoint.isStationTransferPoint()) {
      return findTPoints(txPoint.asStationTransferPoint());
    } else if (txPoint.isStopTransferPoint()) {
      return findTPoints(txPoint.asStopTransferPoint());
    } else if (txPoint.isRouteStationTransferPoint()) {
      return findTPoint(txPoint.asRouteStationTransferPoint(), boarding);
    } else if (txPoint.isRouteStopTransferPoint()) {
      return findTPoint(txPoint.asRouteStopTransferPoint(), boarding);
    } else {
      return findTPoints(txPoint.asTripTransferPoint());
    }
  }

  private List<TPoint> findTPoints(StationTransferPoint point) {
    var station = point.getStation();
    var patterns = patternsByStation.get(station);

    if (patterns == null) {
      return List.of();
    }

    var sourcePoint = createTransferPointForPattern(station);
    var result = new ArrayList<TPoint>();

    for (RoutingTripPattern pattern : patterns) {
      var tripPattern = pattern.getPattern();
      for (int pos = 0; pos < tripPattern.numberOfStops(); ++pos) {
        if (point.getStation() == tripPattern.getStop(pos).getParentStation()) {
          result.add(new TPoint(pattern, sourcePoint, null, pos));
        }
      }
    }
    return result;
  }

  private List<TPoint> findTPoints(StopTransferPoint point) {
    var stop = point.asStopTransferPoint().getStop();
    var patterns = patternsByStop.get(stop);

    if (patterns == null) {
      return List.of();
    }

    var sourcePoint = createTransferPointForPattern(stop.getIndex());
    var result = new ArrayList<TPoint>();

    for (RoutingTripPattern pattern : patterns) {
      var p = pattern.getPattern();
      for (int pos = 0; pos < p.numberOfStops(); ++pos) {
        if (point.getStop() == p.getStop(pos)) {
          result.add(new TPoint(pattern, sourcePoint, null, pos));
        }
      }
    }
    return result;
  }

  private List<TPoint> findTPoint(RouteStationTransferPoint point, boolean boarding) {
    return findTPointForRoute(
      point.getRoute(),
      boarding
        ? p -> p.findBoardingStopPositionInPattern(point.getStation())
        : p -> p.findAlightStopPositionInPattern(point.getStation())
    );
  }

  private List<TPoint> findTPoint(RouteStopTransferPoint point, boolean boarding) {
    return findTPointForRoute(
      point.getRoute(),
      boarding
        ? p -> p.findBoardingStopPositionInPattern(point.getStop())
        : p -> p.findAlightStopPositionInPattern(point.getStop())
    );
  }

  private List<TPoint> findTPointForRoute(
    Route route,
    ToIntFunction<TripPattern> resolveStopPosInPattern
  ) {
    var patterns = patternsByRoute.get(route);

    // A route should have a pattern(trip), but it does not hurt to check here
    if (patterns == null) {
      return List.of();
    }

    var points = new ArrayList<TPoint>();

    for (var pattern : patterns) {
      int stopPosInPattern = resolveStopPosInPattern.applyAsInt(pattern.getPattern());
      // stopPosInPattern == -1 means stop is not on pattern
      if (stopPosInPattern >= 0) {
        int stopIndex = pattern.stopIndex(stopPosInPattern);
        var sourcePoint = createTransferPointForPattern(route, stopIndex);
        points.add(new TPoint(pattern, sourcePoint, null, stopPosInPattern));
      }
    }
    return points;
  }

  private List<TPoint> findTPoints(TripTransferPoint point) {
    var trip = point.getTrip();

    // All trips have at least one pattern, no need to check for null here
    var patterns = patternsByTrip.get(trip);
    var patternsByRealtimeOrScheduled = patterns
      .stream()
      .collect(Collectors.groupingBy(pattern -> pattern.getPattern().isCreatedByRealtimeUpdater()));

    // Process first the pattern for which stopPosInPattern was calculated for
    List<RoutingTripPattern> scheduledPatterns = patternsByRealtimeOrScheduled.get(Boolean.FALSE);
    if (scheduledPatterns == null || scheduledPatterns.size() != 1) {
      LOG.warn(
        "Trip {} does not have exactly one scheduled trip pattern, found: {}. " +
        "Skipping transfer generation.",
        trip,
        scheduledPatterns
      );
      return List.of();
    }

    int stopPosInPattern = point.getStopPositionInPattern();
    var scheduledPattern = scheduledPatterns.get(0);
    int stopIndex = scheduledPattern.stopIndex(stopPosInPattern);
    var sourcePoint = createTransferPointForPattern(trip, stopIndex);
    TPoint scheduledPoint = new TPoint(scheduledPattern, sourcePoint, trip, stopPosInPattern);

    // Return early if only scheduled pattern exists
    var realtimePatterns = patternsByRealtimeOrScheduled.get(Boolean.TRUE);
    if (realtimePatterns == null || realtimePatterns.isEmpty()) {
      return List.of(scheduledPoint);
    }

    // Process the other patterns based on the scheduled pattern
    StopLocation scheduledStop = scheduledPattern.getPattern().getStop(stopPosInPattern);

    List<TPoint> res = new ArrayList<>();
    res.add(scheduledPoint);
    for (RoutingTripPattern pattern : realtimePatterns) {
      // Check if the same stop or its sibling is at the same position, if yes, generate a transfer point
      if (stopPosInPattern < pattern.numberOfStopsInPattern()) {
        StopLocation stop = pattern.getPattern().getStop(stopPosInPattern);
        if (stop.equals(scheduledStop) || stop.isPartOfSameStationAs(scheduledStop)) {
          res.add(
            new TPoint(
              pattern,
              createTransferPointForPattern(trip, pattern.stopIndex(stopPosInPattern)),
              trip,
              stopPosInPattern
            )
          );
          continue;
        }
      }
      LOG.info(
        "Updated pattern for trip {}, does not match original for stop {} at pos {}. " +
        "Skipping transfer generation.",
        trip,
        scheduledStop,
        stopPosInPattern
      );
      // TODO - Find the expected stop in the new pattern, as its position in the pattern has been
      //  shifted due to added or removed (not just cancelled) stops in the realtime pattern.
    }

    return List.copyOf(res);
  }

  private static class TPoint {

    RoutingTripPattern pattern;
    TransferPointMatcher sourcePoint;
    Trip trip;
    int stopPosition;

    private TPoint(
      RoutingTripPattern pattern,
      TransferPointMatcher sourcePoint,
      Trip trip,
      int stopPosition
    ) {
      this.pattern = pattern;
      this.sourcePoint = sourcePoint;
      this.trip = trip;
      this.stopPosition = stopPosition;
    }

    @Override
    public int hashCode() {
      return Objects.hash(pattern, trip, stopPosition);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof TPoint tPoint)) {
        return false;
      }
      return (
        stopPosition == tPoint.stopPosition &&
        Objects.equals(pattern, tPoint.pattern) &&
        Objects.equals(trip, tPoint.trip)
      );
    }

    boolean canBoard() {
      // We prevent boarding at the last stop, this might be enforced by the
      // canBoard method, but we do not trust it here.
      int lastStopPosition = pattern.numberOfStopsInPattern() - 1;
      return stopPosition != lastStopPosition && pattern.boardingPossibleAt(stopPosition);
    }

    boolean canAlight() {
      // We prevent alighting at the first stop, this might be enforced by the
      // canAlight method, but we do not trust it here.
      return stopPosition != 0 && pattern.alightingPossibleAt(stopPosition);
    }

    void addTransferConstraints(
      ConstrainedTransfer tx,
      TPoint to,
      TransferForPatternByStopPos[] forwardTransfers,
      TransferForPatternByStopPos[] reverseTransfers
    ) {
      int rank = tx.getSpecificityRanking();
      var c = tx.getTransferConstraint();

      // Forward search
      if (forwardTransfers[to.pattern.patternIndex()] == null) {
        forwardTransfers[to.pattern.patternIndex()] = new TransferForPatternByStopPos();
      }
      forwardTransfers[to.pattern.patternIndex()].add(
          to.stopPosition,
          new TransferForPattern(sourcePoint, to.trip, rank, c)
        );
      // Reverse search
      if (reverseTransfers[pattern.patternIndex()] == null) {
        reverseTransfers[pattern.patternIndex()] = new TransferForPatternByStopPos();
      }
      reverseTransfers[pattern.patternIndex()].add(
          stopPosition,
          new TransferForPattern(to.sourcePoint, trip, rank, c)
        );
    }
  }
}
