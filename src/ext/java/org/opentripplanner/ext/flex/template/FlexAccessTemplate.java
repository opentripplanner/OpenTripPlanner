package org.opentripplanner.ext.flex.template;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.astar.model.GraphPath;
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

    FlexTripEdge flexEdge = getFlexEdge(flexToVertex, egress.stop);

    State state = flexEdge.traverse(accessEgress.state);

    for (Edge e : egressEdges) {
      state = e.traverse(state);
    }

    int[] flexTimes = getFlexTimes(flexEdge, state);

    int preFlexTime = flexTimes[0];
    int flexTime = flexTimes[1];
    int postFlexTime = flexTimes[2];

    int timeShift;

    if (arriveBy) {
      int lastStopArrivalTime = departureTime - postFlexTime - secondsFromStartOfTime;
      int latestArrivalTime = trip.latestArrivalTime(
        lastStopArrivalTime,
        fromStopIndex,
        toStopIndex,
        flexTime
      );
      if (latestArrivalTime == -1) {
        return null;
      }

      // Shift from departing at departureTime to arriving at departureTime
      timeShift = secondsFromStartOfTime + latestArrivalTime - flexTime - preFlexTime;
    } else {
      int firstStopDepartureTime = departureTime + preFlexTime - secondsFromStartOfTime;
      int earliestDepartureTime = trip.earliestDepartureTime(
        firstStopDepartureTime,
        fromStopIndex,
        toStopIndex,
        flexTime
      );
      if (earliestDepartureTime == -1) {
        return null;
      }

      timeShift = secondsFromStartOfTime + earliestDepartureTime - preFlexTime;
    }

    ZonedDateTime startTime = startOfTime.plusSeconds(timeShift);

    return graphPathToItineraryMapper
      .generateItinerary(new GraphPath<>(state))
      .withTimeShiftToStartAt(startTime);
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

  protected int[] getFlexTimes(FlexTripEdge flexEdge, State state) {
    int preFlexTime = (int) accessEgress.state.getElapsedTimeSeconds();
    int edgeTimeInSeconds = flexEdge.getTimeInSeconds();
    int postFlexTime = (int) state.getElapsedTimeSeconds() - preFlexTime - edgeTimeInSeconds;
    return new int[] { preFlexTime, edgeTimeInSeconds, postFlexTime };
  }

  protected FlexTripEdge getFlexEdge(Vertex flexToVertex, StopLocation transferStop) {
    return new FlexTripEdge(
      accessEgress.state.getVertex(),
      flexToVertex,
      accessEgress.stop,
      transferStop,
      trip,
      this,
      calculator
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
