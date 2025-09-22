package org.opentripplanner.ext.flex.template;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.opentripplanner.ext.flex.FlexAccessEgress;
import org.opentripplanner.ext.flex.FlexPathDurations;
import org.opentripplanner.ext.flex.edgetype.FlexTripEdge;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.model.PathTransfer;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.EdgeTraverser;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * A container for a few pieces of information that can be used to calculate flex accesses, egresses,
 * direct flex itineraries or polylines.
 * <p>
 * Please also see Flex.svg for an illustration of how the flex concepts relate to each other.
 */
abstract class AbstractFlexTemplate {

  /**
   * We do not want extremely short flex trips, they will normally be dominated in the
   * routing later. We set an absolute min duration to 10 seconds (167m with 60 km/h).
   */
  private static final int MIN_FLEX_TRIP_DURATION_SECONDS = 10;

  // TODO - This is confusing, and not following OO principles. The from/to stop
  //      - changes type for access/egress, move them down into child class.
  //      - this apply to transferStop as well.
  protected final NearbyStop accessEgress;
  protected final FlexTrip<?, ?> trip;
  protected final int boardStopPosition;
  protected final int alightStopPosition;
  protected final StopLocation transferStop;
  protected final int secondsFromStartOfTime;
  protected final LocalDate serviceDate;
  protected final int requestedBookingTime;
  protected final FlexPathCalculator calculator;
  private final Duration maxTransferDuration;

  /**
   * @param trip                The FlexTrip used for this template
   * @param accessEgress        Path from origin/destination to the point of boarding/alighting for
   *                            this flex trip
   * @param transferStop        The stop location where this FlexTrip transfers to another transit
   *                            service.
   * @param boardStopPosition   The stop-board-position in the trip pattern
   * @param alightStopPosition  The stop-alight-position in the trip pattern
   * @param date                The service date of this FlexTrip
   * @param calculator          Calculates the path and duration of the FlexTrip
   * @param maxTransferDuration The limit for how long a transfer is allowed to be
   */
  AbstractFlexTemplate(
    FlexTrip<?, ?> trip,
    NearbyStop accessEgress,
    StopLocation transferStop,
    int boardStopPosition,
    int alightStopPosition,
    FlexServiceDate date,
    FlexPathCalculator calculator,
    Duration maxTransferDuration
  ) {
    this.accessEgress = accessEgress;
    this.trip = trip;
    this.boardStopPosition = boardStopPosition;
    this.alightStopPosition = alightStopPosition;
    this.transferStop = transferStop;
    this.secondsFromStartOfTime = date.secondsFromStartOfTime();
    this.serviceDate = date.serviceDate();
    this.requestedBookingTime = date.requestedBookingTime();
    this.calculator = calculator;
    this.maxTransferDuration = maxTransferDuration;
  }

  StopLocation getTransferStop() {
    return transferStop;
  }

  StopLocation getAccessEgressStop() {
    return accessEgress.stop;
  }

  /**
   * This method is very much the hot code path in the flex access/egress search, so any
   * optimization here will lead to noticeable speedups.
   */
  Stream<FlexAccessEgress> createFlexAccessEgressStream(FlexAccessEgressCallbackAdapter callback) {
    if (transferStop instanceof RegularStop stop) {
      var flexVertex = callback.getStopVertex(stop.getId());
      return Stream.of(createFlexAccessEgress(new ArrayList<>(), flexVertex, stop)).filter(
        Objects::nonNull
      );
    }
    // transferStop is Location Area/Line
    else {
      double maxDistanceMeters =
        maxTransferDuration.getSeconds() *
        accessEgress.state.getRequest().preferences().walk().speed();

      return getTransfersFromTransferStop(callback)
        .stream()
        .filter(pathTransfer -> pathTransfer.getDistanceMeters() <= maxDistanceMeters)
        .filter(transfer -> getFinalStop(transfer) != null)
        .map(transfer -> {
          var edges = getTransferEdges(transfer);
          var flexVertex = getFlexVertex(edges.get(0));
          var finalStop = getFinalStop(transfer);
          return createFlexAccessEgress(edges, flexVertex, finalStop);
        })
        .filter(Objects::nonNull);
    }
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(AbstractFlexTemplate.class)
      .addObj("accessEgress", accessEgress)
      .addObj("trip", trip)
      .addNum("boardStopPosition", boardStopPosition)
      .addNum("alightStopPosition", alightStopPosition)
      .addObj("transferStop", transferStop)
      .addServiceTime("secondsFromStartOfTime", secondsFromStartOfTime)
      .addDate("serviceDate", serviceDate)
      .addObj("calculator", calculator)
      .addDuration("maxTransferDuration", maxTransferDuration)
      .toString();
  }

  /**
   * Get a list of edges used for transferring to and from the scheduled transit network. The edges
   * should be in the order of traversal of the state in the NearbyStop
   */
  protected abstract List<Edge> getTransferEdges(PathTransfer transfer);

  /**
   * Get the {@Link Stop} where the connection to the scheduled transit network is made.
   */
  protected abstract RegularStop getFinalStop(PathTransfer transfer);

  /**
   * Get the transfers to/from stops in the scheduled transit network from the beginning/end of the
   * flex ride for the access/egress.
   */
  protected abstract Collection<PathTransfer> getTransfersFromTransferStop(
    FlexAccessEgressCallbackAdapter callback
  );

  /**
   * Get the {@Link Vertex} where the flex ride ends/begins for the access/egress.
   */
  protected abstract Vertex getFlexVertex(Edge edge);

  /**
   * Break down the time spent on flex ride/path in access, trip and egress.
   */
  protected abstract FlexPathDurations calculateFlexPathDurations(
    FlexTripEdge flexEdge,
    State state
  );

  /**
   * Get the FlexTripEdge for the flex ride.
   */
  @Nullable
  protected abstract FlexTripEdge getFlexEdge(Vertex flexFromVertex, StopLocation transferStop);

  @Nullable
  private FlexAccessEgress createFlexAccessEgress(
    List<Edge> transferEdges,
    Vertex flexVertex,
    RegularStop stop
  ) {
    var flexEdge = getFlexEdge(flexVertex, transferStop);

    // Drop non-routable and very short(<10s) trips
    if (flexEdge == null || flexEdge.getTimeInSeconds() < MIN_FLEX_TRIP_DURATION_SECONDS) {
      return null;
    }

    // this code is a little repetitive but needed as a performance improvement. previously
    // the flex path was checked before this method was called. this meant that every path
    // was traversed twice, leading to a noticeable slowdown.
    final var afterFlexState = flexEdge.traverse(accessEgress.state);
    if (State.isEmpty(afterFlexState)) {
      return null;
    }

    final var finalStateOpt = EdgeTraverser.traverseEdges(afterFlexState[0], transferEdges);

    return finalStateOpt
      .map(finalState -> {
        var durations = calculateFlexPathDurations(flexEdge, finalState);

        return new FlexAccessEgress(
          stop,
          durations,
          boardStopPosition,
          alightStopPosition,
          trip,
          finalState,
          transferEdges.isEmpty(),
          requestedBookingTime
        );
      })
      .orElse(null);
  }
}
