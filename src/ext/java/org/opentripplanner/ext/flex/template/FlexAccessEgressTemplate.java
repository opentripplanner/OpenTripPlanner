package org.opentripplanner.ext.flex.template;

import org.opentripplanner.ext.flex.FlexAccessEgress;
import org.opentripplanner.ext.flex.FlexServiceDate;
import org.opentripplanner.ext.flex.edgetype.FlexTripEdge;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.model.SimpleTransfer;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.routing.vertextype.TransitStopVertex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public abstract class FlexAccessEgressTemplate {
  protected final NearbyStop accessEgress;
  protected final FlexTrip trip;
  public final int fromStopIndex;
  public final int toStopIndex;
  protected final StopLocation transferStop;
  protected final int secondsFromStartOfTime;
  public final ServiceDate serviceDate;
  protected final FlexPathCalculator calculator;

  FlexAccessEgressTemplate(
      NearbyStop accessEgress,
      FlexTrip trip,
      int fromStopIndex,
      int toStopIndex,
      StopLocation transferStop,
      FlexServiceDate date,
      FlexPathCalculator calculator
  ) {
    this.accessEgress = accessEgress;
    this.trip = trip;
    this.fromStopIndex = fromStopIndex;
    this.toStopIndex = toStopIndex;
    this.transferStop = transferStop;
    this.secondsFromStartOfTime = date.secondsFromStartOfTime;
    this.serviceDate = date.serviceDate;
    this.calculator = calculator;
  }

  public StopLocation getTransferStop() {
    return transferStop;
  }

  public FlexTrip getFlexTrip() {
    return trip;
  }

  abstract protected List<Edge> getTransferEdges(SimpleTransfer simpleTransfer);

  abstract protected Stop getFinalStop(SimpleTransfer simpleTransfer);

  abstract protected Collection<SimpleTransfer> getTransfersFromTransferStop(Graph graph);

  abstract protected Vertex getFlexVertex(Edge edge);

  abstract protected int[] getFlexTimes(FlexTripEdge flexEdge, State state);

  abstract protected FlexTripEdge getFlexEdge(Vertex flexFromVertex, StopLocation transferStop);

  abstract protected boolean isRouteable(Vertex flexVertex);

  public Stream<FlexAccessEgress> createFlexAccessEgressStream(Graph graph) {
    if (transferStop instanceof Stop) {
      TransitStopVertex flexVertex = graph.index.getStopVertexForStop().get(transferStop);
      if (isRouteable(flexVertex)) {
        return Stream.of(getFlexAccessEgress(new ArrayList<>(), flexVertex, (Stop) transferStop));
      }
      return Stream.empty();
    }
    // transferStop is Location Area/Line
    else {
      return getTransfersFromTransferStop(graph)
          .stream()
          .filter(simpleTransfer -> getFinalStop(simpleTransfer) != null)
          .filter(simpleTransfer -> isRouteable(getFlexVertex(getTransferEdges(simpleTransfer).get(0))))
          .map(simpleTransfer -> {
            List<Edge> edges = getTransferEdges(simpleTransfer);
            return getFlexAccessEgress(edges,
                getFlexVertex(edges.get(0)),
                getFinalStop(simpleTransfer)
            );
          });
    }
  }

  protected FlexAccessEgress getFlexAccessEgress(List<Edge> transferEdges, Vertex flexVertex, Stop stop) {
    FlexTripEdge flexEdge = getFlexEdge(flexVertex, transferStop);

    State state = flexEdge.traverse(accessEgress.state);
    for (Edge e : transferEdges) {
      state = e.traverse(state);
    }

    int[] times = getFlexTimes(flexEdge, state);

    return new FlexAccessEgress(
        stop,
        times[0],
        times[1],
        times[2],
        fromStopIndex,
        toStopIndex, secondsFromStartOfTime,
        trip,
        state,
        transferEdges.isEmpty()
    );
  }

}
