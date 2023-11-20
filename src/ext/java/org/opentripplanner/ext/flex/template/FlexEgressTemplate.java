package org.opentripplanner.ext.flex.template;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.ext.flex.FlexPathDurations;
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
    int fromStopIndex,
    int toStopIndex,
    StopLocation transferStop,
    FlexServiceDate date,
    FlexPathCalculator calculator,
    FlexConfig config
  ) {
    super(accessEgress, trip, fromStopIndex, toStopIndex, transferStop, date, calculator, config);
  }

  protected List<Edge> getTransferEdges(PathTransfer transfer) {
    return Lists.reverse(transfer.getEdges());
  }

  protected RegularStop getFinalStop(PathTransfer transfer) {
    return transfer.from instanceof RegularStop regularStop ? regularStop : null;
  }

  protected Collection<PathTransfer> getTransfersFromTransferStop(TransitService transitService) {
    return transitService.getFlexIndex().getTransfersToStop(transferStop);
  }

  protected Vertex getFlexVertex(Edge edge) {
    return edge.getToVertex();
  }

  protected FlexPathDurations calculateFlexPathDurations(FlexTripEdge flexEdge, State state) {
    int postFlexTime = (int) accessEgress.state.getElapsedTimeSeconds();
    int edgeTimeInSeconds = flexEdge.getTimeInSeconds();
    int preFlexTime = (int) state.getElapsedTimeSeconds() - postFlexTime - edgeTimeInSeconds;
    return new FlexPathDurations(
      preFlexTime,
      edgeTimeInSeconds,
      postFlexTime,
      secondsFromStartOfTime
    );
  }

  protected FlexTripEdge getFlexEdge(Vertex flexFromVertex, StopLocation transferStop) {
    var flexPath = calculator.calculateFlexPath(
      flexFromVertex,
      accessEgress.state.getVertex(),
      fromStopIndex,
      toStopIndex
    );

    if (flexPath == null) {
      return null;
    }

    return FlexTripEdge.createFlexTripEdge(
      flexFromVertex,
      accessEgress.state.getVertex(),
      transferStop,
      accessEgress.stop,
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
        flexVertex,
        accessEgress.state.getVertex(),
        fromStopIndex,
        toStopIndex
      ) !=
      null
    );
  }
}
