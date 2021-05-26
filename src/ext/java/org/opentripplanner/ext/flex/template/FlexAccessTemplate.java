package org.opentripplanner.ext.flex.template;

import org.opentripplanner.ext.flex.FlexServiceDate;
import org.opentripplanner.ext.flex.edgetype.FlexTripEdge;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.model.SimpleTransfer;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.mapping.GraphPathToItineraryMapper;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.routing.spt.GraphPath;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class FlexAccessTemplate extends FlexAccessEgressTemplate {
  public FlexAccessTemplate(
      NearbyStop accessEgress, FlexTrip trip, int fromStopTime, int toStopTime,
      StopLocation transferStop, FlexServiceDate date, FlexPathCalculator calculator
  ) {
    super(accessEgress, trip, fromStopTime, toStopTime, transferStop, date, calculator);
  }

  public Itinerary createDirectItinerary(
      NearbyStop egress, boolean arriveBy, int departureTime, ZonedDateTime startOfTime
  ) {
    List<Edge> egressEdges = egress.edges;

    Vertex flexToVertex = egress.state.getVertex();

    FlexTripEdge flexEdge = getFlexEdge(flexToVertex, egress.stop);

    State state = flexEdge.traverse(accessEgress.state);
    if(state == null)
  	  return null;

    for (Edge e : egressEdges) {
      state = e.traverse(state);
      if(state == null)
    	  return null;
    }

    Itinerary itinerary = GraphPathToItineraryMapper.generateItinerary(
        new GraphPath(state),
        Locale.ENGLISH
    );

    return itinerary;
  }

  protected List<Edge> getTransferEdges(SimpleTransfer simpleTransfer) {
    return simpleTransfer.getEdges();
  }

  protected Stop getFinalStop(SimpleTransfer simpleTransfer) {
    return simpleTransfer.to instanceof Stop ? (Stop) simpleTransfer.to : null;
  }

  protected Collection<SimpleTransfer> getTransfersFromTransferStop(Graph graph) {
    return graph.transfersByStop.get(transferStop);
  }

  protected Vertex getFlexVertex(Edge edge) {
    return edge.getFromVertex();
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
