package org.opentripplanner.ext.flex.template;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.ext.flex.FlexServiceDate;
import org.opentripplanner.ext.flex.edgetype.FlexTripEdge;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.model.PathTransfer;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.ext.flex.FlexParameters;

public class FlexEgressTemplate extends FlexAccessEgressTemplate {
  public FlexEgressTemplate(
      NearbyStop accessEgress, FlexTrip trip, int fromStopTime, int toStopTime,
      StopLocation transferStop, FlexServiceDate date, FlexPathCalculator calculator,
      FlexParameters config
  ) {
    super(accessEgress, trip, fromStopTime, toStopTime, transferStop, date, calculator, config);
  }

  protected List<Edge> getTransferEdges(PathTransfer transfer) {
    return Lists.reverse(transfer.getEdges());
  }

  protected Stop getFinalStop(PathTransfer transfer) {
    return transfer.from instanceof Stop ? (Stop) transfer.from : null;
  }

  protected Collection<PathTransfer> getTransfersFromTransferStop(Graph graph) {
    return graph.index.getFlexIndex().transfersToStop.get(transferStop);
  }

  protected Vertex getFlexVertex(Edge edge) {
    return edge.getToVertex();
  }

  protected boolean isRouteable(Vertex flexVertex) {
    if (accessEgress.state.getVertex() == flexVertex) {
      return false;
    } else
      return calculator.calculateFlexPath(flexVertex,
          accessEgress.state.getVertex(),
          fromStopIndex,
          toStopIndex
      ) != null;
  };

  protected int[] getFlexTimes(FlexTripEdge flexEdge, State state) {
    int postFlexTime = (int) accessEgress.state.getElapsedTimeSeconds();
    int edgeTimeInSeconds = flexEdge.getTimeInSeconds();
    int preFlexTime = (int) state.getElapsedTimeSeconds() - postFlexTime - edgeTimeInSeconds;
    return new int[]{ preFlexTime, edgeTimeInSeconds, postFlexTime };
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
