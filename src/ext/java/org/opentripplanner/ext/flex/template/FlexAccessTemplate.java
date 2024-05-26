package org.opentripplanner.ext.flex.template;

import static org.opentripplanner.model.StopTime.MISSING_VALUE;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
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

class FlexAccessTemplate extends AbstractFlexTemplate {

  FlexAccessTemplate(
    FlexTrip<?, ?> trip,
    NearbyStop boardStop,
    int boardStopPosition,
    StopLocation alightStop,
    int alightStopPosition,
    FlexServiceDate date,
    FlexPathCalculator calculator,
    Duration maxTransferDuration
  ) {
    super(
      trip,
      boardStop,
      alightStop,
      boardStopPosition,
      alightStopPosition,
      date,
      calculator,
      maxTransferDuration
    );
  }

  Optional<DirectFlexPath> createDirectGraphPath(
    NearbyStop egress,
    boolean arriveBy,
    int departureTime
  ) {
    List<Edge> egressEdges = egress.edges;

    Vertex flexToVertex = egress.state.getVertex();

    if (!isRouteable(flexToVertex)) {
      return Optional.empty();
    }

    var flexEdge = getFlexEdge(flexToVertex, egress.stop);

    if (flexEdge == null) {
      return Optional.empty();
    }

    final State[] afterFlexState = flexEdge.traverse(accessEgress.state);

    var finalStateOpt = EdgeTraverser.traverseEdges(afterFlexState[0], egressEdges);

    if (finalStateOpt.isEmpty()) {
      return Optional.empty();
    }

    var finalState = finalStateOpt.get();
    var flexDurations = calculateFlexPathDurations(flexEdge, finalState);

    int timeShift;

    if (arriveBy) {
      int lastStopArrivalTime = flexDurations.mapToFlexTripArrivalTime(departureTime);
      int latestArrivalTime = trip.latestArrivalTime(
        lastStopArrivalTime,
        fromStopIndex,
        toStopIndex,
        flexDurations.trip()
      );

      if (latestArrivalTime == MISSING_VALUE) {
        return Optional.empty();
      }

      // Shift from departing at departureTime to arriving at departureTime
      timeShift = flexDurations.mapToRouterArrivalTime(latestArrivalTime) - flexDurations.total();
    } else {
      int firstStopDepartureTime = flexDurations.mapToFlexTripDepartureTime(departureTime);
      int earliestDepartureTime = trip.earliestDepartureTime(
        firstStopDepartureTime,
        fromStopIndex,
        toStopIndex,
        flexDurations.trip()
      );

      if (earliestDepartureTime == MISSING_VALUE) {
        return Optional.empty();
      }
      timeShift = flexDurations.mapToRouterDepartureTime(earliestDepartureTime);
    }

    return Optional.of(new DirectFlexPath(timeShift, finalState));
  }

  protected List<Edge> getTransferEdges(PathTransfer transfer) {
    return transfer.getEdges();
  }

  protected RegularStop getFinalStop(PathTransfer transfer) {
    return transfer.to instanceof RegularStop ? (RegularStop) transfer.to : null;
  }

  protected Collection<PathTransfer> getTransfersFromTransferStop(
    FlexAccessEgressCallbackAdapter callback
  ) {
    return callback.getTransfersFromStop(transferStop);
  }

  protected Vertex getFlexVertex(Edge edge) {
    return edge.getFromVertex();
  }

  protected FlexPathDurations calculateFlexPathDurations(FlexTripEdge flexEdge, State state) {
    int preFlexTime = (int) accessEgress.state.getElapsedTimeSeconds();
    int edgeTimeInSeconds = flexEdge.getTimeInSeconds();
    int postFlexTime = (int) state.getElapsedTimeSeconds() - preFlexTime - edgeTimeInSeconds;
    return new FlexPathDurations(
      preFlexTime,
      edgeTimeInSeconds,
      postFlexTime,
      secondsFromStartOfTime
    );
  }

  protected FlexTripEdge getFlexEdge(Vertex flexToVertex, StopLocation transferStop) {
    var flexPath = calculator.calculateFlexPath(
      accessEgress.state.getVertex(),
      flexToVertex,
      fromStopIndex,
      toStopIndex
    );

    if (flexPath == null) {
      return null;
    }

    return new FlexTripEdge(
      accessEgress.state.getVertex(),
      flexToVertex,
      accessEgress.stop,
      transferStop,
      trip,
      fromStopIndex,
      toStopIndex,
      serviceDate,
      flexPath
    );
  }

  protected boolean isRouteable(Vertex flexVertex) {
    if (accessEgress.state.getVertex() == flexVertex) {
      return false;
    } else return (
      calculator.calculateFlexPath(
        accessEgress.state.getVertex(),
        flexVertex,
        fromStopIndex,
        toStopIndex
      ) !=
      null
    );
  }
}
