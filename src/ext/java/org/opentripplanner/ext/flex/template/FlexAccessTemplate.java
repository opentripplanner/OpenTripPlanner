package org.opentripplanner.ext.flex.template;

import static org.opentripplanner.model.StopTime.MISSING_VALUE;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.ext.flex.FlexPathDurations;
import org.opentripplanner.ext.flex.FlexServiceDate;
import org.opentripplanner.ext.flex.edgetype.FlexTripEdge;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.model.PathTransfer;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.mapping.GraphPathToItineraryMapper;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.standalone.config.sandbox.FlexConfig;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.EdgeTraverser;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.TransitService;

public class FlexAccessTemplate extends FlexAccessEgressTemplate {

  public FlexAccessTemplate(
    NearbyStop accessEgress,
    FlexTrip trip,
    int fromStopTime,
    int toStopTime,
    StopLocation transferStop,
    FlexServiceDate date,
    FlexPathCalculator calculator,
    FlexConfig config
  ) {
    super(accessEgress, trip, fromStopTime, toStopTime, transferStop, date, calculator, config);
  }

  public Itinerary createDirectGraphPath(
    NearbyStop egress,
    boolean arriveBy,
    int departureTime,
    ZonedDateTime startOfTime,
    GraphPathToItineraryMapper graphPathToItineraryMapper
  ) {
    List<Edge> egressEdges = egress.edges;

    Vertex flexToVertex = egress.state.getVertex();

    if (!isRouteable(flexToVertex)) {
      return null;
    }

    var flexEdge = getFlexEdge(flexToVertex, egress.stop);

    if (flexEdge == null) {
      return null;
    }

    final State[] afterFlexState = flexEdge.traverse(accessEgress.state);

    var finalStateOpt = EdgeTraverser.traverseEdges(afterFlexState[0], egressEdges);

    return finalStateOpt
      .map(finalState -> {
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
            return null;
          }

          // Shift from departing at departureTime to arriving at departureTime
          timeShift =
            flexDurations.mapToRouterArrivalTime(latestArrivalTime) - flexDurations.total();
        } else {
          int firstStopDepartureTime = flexDurations.mapToFlexTripDepartureTime(departureTime);
          int earliestDepartureTime = trip.earliestDepartureTime(
            firstStopDepartureTime,
            fromStopIndex,
            toStopIndex,
            flexDurations.trip()
          );

          if (earliestDepartureTime == MISSING_VALUE) {
            return null;
          }
          timeShift = flexDurations.mapToRouterDepartureTime(earliestDepartureTime);
        }

        ZonedDateTime startTime = startOfTime.plusSeconds(timeShift);

        return graphPathToItineraryMapper
          .generateItinerary(new GraphPath<>(finalState))
          .withTimeShiftToStartAt(startTime);
      })
      .orElse(null);
  }

  protected List<Edge> getTransferEdges(PathTransfer transfer) {
    return transfer.getEdges();
  }

  protected RegularStop getFinalStop(PathTransfer transfer) {
    return transfer.to instanceof RegularStop ? (RegularStop) transfer.to : null;
  }

  protected Collection<PathTransfer> getTransfersFromTransferStop(TransitService transitService) {
    return transitService.getTransfersByStop(transferStop);
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

    return FlexTripEdge.createFlexTripEdge(
      accessEgress.state.getVertex(),
      flexToVertex,
      accessEgress.stop,
      transferStop,
      trip,
      this,
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
