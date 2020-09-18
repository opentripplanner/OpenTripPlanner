package org.opentripplanner.ext.flex.template;

import org.opentripplanner.ext.flex.FlexTripEdge;
import org.opentripplanner.ext.flex.distancecalculator.DistanceCalculator;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.model.SimpleTransfer;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.Transfer;
import org.opentripplanner.model.TransferType;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.mapping.GraphPathToItineraryMapper;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.graphfinder.StopAtDistance;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.vertextype.TransitStopVertex;

import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class FlexAccessTemplate extends FlexAccessEgressTemplate {
  public FlexAccessTemplate(
      StopAtDistance accessEgress, FlexTrip trip, int fromStopTime, int toStopTime,
      StopLocation transferStop, int differenceFromStartOfTime, ServiceDate serviceDate, DistanceCalculator calculator
  ) {
    super(accessEgress, trip, fromStopTime, toStopTime, transferStop, differenceFromStartOfTime, serviceDate, calculator);
  }

  public Itinerary getDirectItinerary(
      StopAtDistance egress, boolean arriveBy, int departureTime, ZonedDateTime startOfTime
  ) {
    List<Edge> egressEdges = egress.edges;

    Vertex flexToVertex = egressEdges.get(0).getFromVertex();

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

    Integer timeShift = null;

    if (arriveBy) {
      int lastStopArrivalTime = departureTime - postFlexTime - differenceFromStartOfTime;
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
      timeShift = differenceFromStartOfTime + latestArrivalTime - flexTime - preFlexTime;
    } else {
      int firstStopDepartureTime = departureTime + preFlexTime - differenceFromStartOfTime;
      int earliestDepartureTime = trip.earliestDepartureTime(
          firstStopDepartureTime,
          fromStopIndex,
          toStopIndex,
          flexTime
      );
      if (earliestDepartureTime == -1) {
        return null;
      }

      timeShift = differenceFromStartOfTime + earliestDepartureTime - preFlexTime;
    }

    Itinerary itinerary = GraphPathToItineraryMapper.generateItinerary(
        new GraphPath(state, false),
        Locale.ENGLISH
    );

    ZonedDateTime zdt = startOfTime.plusSeconds(timeShift);
    Calendar c = Calendar.getInstance(TimeZone.getTimeZone(zdt.getZone()));
    c.setTimeInMillis(zdt.toInstant().toEpochMilli());
    itinerary.timeShiftToStartAt(c);
    return itinerary;
  }

  protected List<Edge> getTransferEdges(SimpleTransfer simpleTransfer) {
    return simpleTransfer.getEdges();
  }

  protected StopLocation getFinalStop(SimpleTransfer simpleTransfer) {
    return simpleTransfer.to;
  }

  protected Collection<SimpleTransfer> getTransfersFromTransferStop(Graph graph) {
    return graph.transfersByStop.get(transferStop);
  }

  protected Vertex getFlexVertex(Edge edge) {
    return edge.getFromVertex();
  }

  protected boolean isRouteable(Vertex flexVertex) {
    if (accessEgress.state.getVertex() == flexVertex) {
      return false;
    } else if (calculator.getDuration(accessEgress.state.getVertex(), flexVertex, fromStopIndex, toStopIndex) == null) {
      return false;
    }
    return true;
  };

  protected int[] getFlexTimes(FlexTripEdge flexEdge, State state) {
    int preFlexTime = (int) accessEgress.state.getElapsedTimeSeconds();
    int edgeTimeInSeconds = flexEdge.getTimeInSeconds();
    int postFlexTime = (int) state.getElapsedTimeSeconds() - preFlexTime - edgeTimeInSeconds;
    return new int[]{ preFlexTime, edgeTimeInSeconds, postFlexTime };
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
}
