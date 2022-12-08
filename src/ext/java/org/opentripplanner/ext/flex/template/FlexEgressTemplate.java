package org.opentripplanner.ext.flex.template;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.ext.flex.FlexServiceDate;
import org.opentripplanner.ext.flex.edgetype.FlexTripEdge;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.model.PathTransfer;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.standalone.config.sandbox.FlexConfig;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.TransitService;

public class FlexEgressTemplate extends FlexAccessEgressTemplate {

  public FlexEgressTemplate(
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

  protected List<Edge> getTransferEdges(PathTransfer transfer) {
    return Lists.reverse(transfer.getEdges());
  }

  protected RegularStop getFinalStop(PathTransfer transfer) {
    return transfer.from instanceof RegularStop ? (RegularStop) transfer.from : null;
  }

  protected Collection<PathTransfer> getTransfersFromTransferStop(TransitService transitService) {
    return transitService.getFlexIndex().getTransfersToStop(transferStop);
  }

  protected Vertex getFlexVertex(Edge edge) {
    return edge.getToVertex();
  }

  protected int[] getFlexTimes(FlexTripEdge flexEdge, State state) {
    int postFlexTime = (int) accessEgress.state.getElapsedTimeSeconds();
    int edgeTimeInSeconds = flexEdge.getTimeInSeconds();
    int preFlexTime = (int) state.getElapsedTimeSeconds() - postFlexTime - edgeTimeInSeconds;
    return new int[] { preFlexTime, edgeTimeInSeconds, postFlexTime };
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

  protected boolean isRouteable(Vertex flexVertex) {
    if (accessEgress.state.getVertex() == flexVertex) {
      return false;
    } else return (
      calculator.calculateFlexPath(
        flexVertex,
        accessEgress.state.getVertex(),
        fromStopIndex,
        toStopIndex
      ) !=
      null
    );
  }
}
