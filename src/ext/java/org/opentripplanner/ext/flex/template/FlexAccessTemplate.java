package org.opentripplanner.ext.flex.template;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.ext.flex.FlexPathDurations;
import org.opentripplanner.ext.flex.edgetype.FlexTripEdge;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.model.PathTransfer;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;

class FlexAccessTemplate extends AbstractFlexTemplate {

  FlexAccessTemplate(
    FlexTrip<?, ?> trip,
    NearbyStop boardStop,
    int boardStopPosition,
    StopLocation alightStop,
    int alightStopPosition,
    FlexServiceDate date,
    FlexPathCalculator calculator,
    Duration maxTransferDuration
  ) {
    super(
      trip,
      boardStop,
      alightStop,
      boardStopPosition,
      alightStopPosition,
      date,
      calculator,
      maxTransferDuration
    );
  }

  protected List<Edge> getTransferEdges(PathTransfer transfer) {
    return transfer.getEdges();
  }

  protected RegularStop getFinalStop(PathTransfer transfer) {
    return transfer.to instanceof RegularStop ? (RegularStop) transfer.to : null;
  }

  protected Collection<PathTransfer> getTransfersFromTransferStop(
    FlexAccessEgressCallbackAdapter callback
  ) {
    return callback.getTransfersFromStop(transferStop);
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
      boardStopPosition,
      alightStopPosition
    );

    if (flexPath == null) {
      return null;
    }

    return new FlexTripEdge(
      accessEgress.state.getVertex(),
      flexToVertex,
      accessEgress.stop,
      transferStop,
      trip,
      boardStopPosition,
      alightStopPosition,
      serviceDate,
      flexPath
    );
  }
}
