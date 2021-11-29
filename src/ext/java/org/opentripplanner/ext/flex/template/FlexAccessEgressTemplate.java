package org.opentripplanner.ext.flex.template;

import com.google.common.base.MoreObjects;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.opentripplanner.ext.flex.FlexAccessEgress;
import org.opentripplanner.ext.flex.FlexServiceDate;
import org.opentripplanner.ext.flex.edgetype.FlexTripEdge;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.model.PathTransfer;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.opentripplanner.ext.flex.FlexParameters;

public abstract class FlexAccessEgressTemplate {
  protected final NearbyStop accessEgress;
  protected final FlexTrip trip;
  public final int fromStopIndex;
  public final int toStopIndex;
  protected final StopLocation transferStop;
  protected final int secondsFromStartOfTime;
  public final ServiceDate serviceDate;
  protected final FlexPathCalculator calculator;
  private final FlexParameters flexParams;

  /**
   *
   * @param accessEgress  Path from origin to the point of boarding for this flex trip
   * @param trip          The FlexTrip used for this Template
   * @param fromStopIndex Stop sequence index where this FlexTrip is boarded
   * @param toStopIndex   The stop where this FlexTrip alights
   * @param transferStop  The stop location where this FlexTrip alights
   * @param date          The service date of this FlexTrip
   * @param calculator    Calculates the path and duration of the FlexTrip
   */
  FlexAccessEgressTemplate(
      NearbyStop accessEgress,
      FlexTrip trip,
      int fromStopIndex,
      int toStopIndex,
      StopLocation transferStop,
      FlexServiceDate date,
      FlexPathCalculator calculator,
      FlexParameters flexParams
  ) {
    this.accessEgress = accessEgress;
    this.trip = trip;
    this.fromStopIndex = fromStopIndex;
    this.toStopIndex = toStopIndex;
    this.transferStop = transferStop;
    this.secondsFromStartOfTime = date.secondsFromStartOfTime;
    this.serviceDate = date.serviceDate;
    this.calculator = calculator;
    this.flexParams = flexParams;
  }

  public StopLocation getTransferStop() {
    return transferStop;
  }

  public StopLocation getAccessEgressStop() {
    return accessEgress.stop;
  }

  public FlexTrip getFlexTrip() {
    return trip;
  }

  /**
   * Get a list of edges used for transferring to and from the scheduled transit network. The edges
   * should be in the order of traversal of the state in the NearbyStop
   * */
  abstract protected List<Edge> getTransferEdges(PathTransfer transfer);

  /**
   * Get the {@Link Stop} where the connection to the scheduled transit network is made.
   */
  abstract protected Stop getFinalStop(PathTransfer transfer);

  /**
   * Get the transfers to/from stops in the scheduled transit network from the beginning/end of the
   * flex ride for the access/egress.
   */
  abstract protected Collection<PathTransfer> getTransfersFromTransferStop(Graph graph);

  /**
   * Get the {@Link Vertex} where the flex ride ends/begins for the access/egress.
   */
  abstract protected Vertex getFlexVertex(Edge edge);

  /**
   * Get the times in seconds, before during and after the flex ride.
   */
  abstract protected int[] getFlexTimes(FlexTripEdge flexEdge, State state);

  /**
   * Get the FlexTripEdge for the flex ride.
   */
  abstract protected FlexTripEdge getFlexEdge(Vertex flexFromVertex, StopLocation transferStop);

  /**
   * This method is very much the hot code path in the flex access/egress search so any optimization
   * here will lead to noticeable speedups.
   */
  public Stream<FlexAccessEgress> createFlexAccessEgressStream(Graph graph) {
    if (transferStop instanceof Stop) {
      TransitStopVertex flexVertex = graph.index.getStopVertexForStop().get(transferStop);
      return Stream.of(getFlexAccessEgress(new ArrayList<>(), flexVertex, (Stop) transferStop))
              .filter(Objects::nonNull);
    }
    // transferStop is Location Area/Line
    else {
      return getTransfersFromTransferStop(graph)
              .stream()
              .filter(pathTransfer -> pathTransfer.getDistanceMeters() <= flexParams.maxTransferMeters)
              .filter(transfer -> getFinalStop(transfer) != null)
              .map(transfer -> {
                List<Edge> edges = getTransferEdges(transfer);
                return getFlexAccessEgress(
                        edges,
                        getFlexVertex(edges.get(0)),
                        getFinalStop(transfer)
                );
              })
              .filter(Objects::nonNull);
    }
  }

  protected FlexAccessEgress getFlexAccessEgress(List<Edge> transferEdges, Vertex flexVertex, Stop stop) {
    FlexTripEdge flexEdge = getFlexEdge(flexVertex, transferStop);

    // this code is a little repetitive but needed as a performance improvement. previously
    // the flex path was checked before this method was called. this meant that every path
    // was traversed twice leading to a noticeable slowdown.
    State state = flexEdge.traverse(accessEgress.state);
    if (state == null) {
      return null;
    }
    for (Edge e : transferEdges) {
      state = e.traverse(state);
      if (state == null) {
        return null;
      }
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

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
            .add("accessEgress", accessEgress)
            .add("trip", trip)
            .add("fromStopIndex", fromStopIndex)
            .add("toStopIndex", toStopIndex)
            .add("transferStop", transferStop)
            .add("secondsFromStartOfTime", secondsFromStartOfTime)
            .add("serviceDate", serviceDate)
            .add("calculator", calculator)
            .add("flexParams", flexParams)
            .toString();
  }
}
