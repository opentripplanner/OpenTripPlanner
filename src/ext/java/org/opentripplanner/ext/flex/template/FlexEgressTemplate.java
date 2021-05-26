package org.opentripplanner.ext.flex.template;

import com.google.common.collect.Lists;
import org.opentripplanner.ext.flex.FlexServiceDate;
import org.opentripplanner.ext.flex.edgetype.FlexTripEdge;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.model.SimpleTransfer;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.graphfinder.NearbyStop;

import java.util.Collection;
import java.util.List;

public class FlexEgressTemplate extends FlexAccessEgressTemplate {
  public FlexEgressTemplate(
      NearbyStop accessEgress, FlexTrip trip, int fromStopTime, int toStopTime,
      StopLocation transferStop, FlexServiceDate date, FlexPathCalculator calculator
  ) {
    super(accessEgress, trip, fromStopTime, toStopTime, transferStop, date, calculator);
  }

  protected List<Edge> getTransferEdges(SimpleTransfer simpleTransfer) {
    return Lists.reverse(simpleTransfer.getEdges());
  }

  protected Stop getFinalStop(SimpleTransfer simpleTransfer) {
    return simpleTransfer.from instanceof Stop ? (Stop) simpleTransfer.from : null;
  }

  protected Collection<SimpleTransfer> getTransfersFromTransferStop(Graph graph) {
    return graph.index.getFlexIndex().transfersToStop.get(transferStop);
  }

  protected Vertex getFlexVertex(Edge edge) {
    return edge.getToVertex();
  }

  protected FlexTripEdge getFlexEdge(Vertex flexFromVertex, StopLocation transferStop) {
    return new FlexTripEdge(
        flexFromVertex,
        accessEgress.state.getVertex(),
        transferStop,
        accessEgress.stop,
        trip,
        this,
        calculator
    );
  }
}
