package org.opentripplanner.ext.flex.template;

import com.google.common.collect.Lists;
import org.opentripplanner.ext.flex.FlexTripEdge;
import org.opentripplanner.ext.flex.distancecalculator.DistanceCalculator;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.model.SimpleTransfer;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.graphfinder.StopAtDistance;

import java.util.Collection;
import java.util.List;

public class FlexEgressTemplate extends FlexAccessEgressTemplate {
  public FlexEgressTemplate(
      StopAtDistance accessEgress, FlexTrip trip, int fromStopTime, int toStopTime,
      StopLocation transferStop, int differenceFromStartOfTime, ServiceDate serviceDate, DistanceCalculator calculator
  ) {
    super(accessEgress, trip, fromStopTime, toStopTime, transferStop, differenceFromStartOfTime, serviceDate, calculator);
  }

  protected List<Edge> getTransferEdges(SimpleTransfer simpleTransfer) {
    return Lists.reverse(simpleTransfer.getEdges());
  }

  protected StopLocation getFinalStop(SimpleTransfer simpleTransfer) {
    return simpleTransfer.from;
  }

  protected Collection<SimpleTransfer> getTransfersFromTransferStop(Graph graph) {
    return graph.index.getFlexIndex().transfersToStop.get(transferStop);
  }

  protected Vertex getFlexVertex(Edge edge) {
    return edge.getToVertex();
  }

  protected boolean isRouteable(Vertex flexVertex) {
    if (accessEgress.state.getVertex() == flexVertex) {
      return false;
    } else if (calculator.getDuration(flexVertex, accessEgress.state.getVertex(), fromStopIndex, toStopIndex) == null) {
      return false;
    }
    return true;
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
