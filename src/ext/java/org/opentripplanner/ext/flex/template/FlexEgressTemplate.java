package org.opentripplanner.ext.flex.template;

import com.google.common.collect.Lists;
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

class FlexEgressTemplate extends AbstractFlexTemplate {

  FlexEgressTemplate(
    FlexTrip<?, ?> trip,
    StopLocation boardStop,
    int boardStopPosition,
    NearbyStop alightStop,
    int alightStopPosition,
    FlexServiceDate date,
    FlexPathCalculator calculator,
    Duration maxTransferDuration
  ) {
    super(
      trip,
      alightStop,
      boardStop,
      boardStopPosition,
      alightStopPosition,
      date,
      calculator,
      maxTransferDuration
    );
  }

  protected List<Edge> getTransferEdges(PathTransfer transfer) {
    return Lists.reverse(transfer.getEdges());
  }

  protected RegularStop getFinalStop(PathTransfer transfer) {
    return transfer.from instanceof RegularStop regularStop ? regularStop : null;
  }

  protected Collection<PathTransfer> getTransfersFromTransferStop(
    FlexAccessEgressCallbackAdapter callback
  ) {
    return callback.getTransfersToStop(transferStop);
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
      boardStopPosition,
      alightStopPosition
    );

    if (flexPath == null) {
      return null;
    }

    return new FlexTripEdge(
      flexFromVertex,
      accessEgress.state.getVertex(),
      transferStop,
      accessEgress.stop,
      trip,
      boardStopPosition,
      alightStopPosition,
      serviceDate,
      flexPath
    );
  }
}
